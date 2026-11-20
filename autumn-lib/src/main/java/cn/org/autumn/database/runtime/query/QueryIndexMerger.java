package cn.org.autumn.database.runtime.query;

import cn.org.autumn.table.data.IndexInfo;
import cn.org.autumn.table.data.TableInfo;
import cn.org.autumn.table.data.index.IndexAnnotationValidator;
import cn.org.autumn.table.data.index.IndexColumnSpec;
import cn.org.autumn.table.data.index.IndexLimitGuard;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 将 {@link QueryIndexRegistry} 与 Entity 注解索引合并：同指纹以构建器为准并忽略注解侧。
 * <p>
 * 与 {@code @Indexes} 并列的第二种索引来源；不改变 {@code initFrom} / {@code IndexDiffEngine} 业务流程。
 * Registry 无同表登记时立即返回（纯注解表零开销）。
 */
@Slf4j
public final class QueryIndexMerger {

    private QueryIndexMerger() {
    }

    /** @deprecated 请使用 {@link #applyTo(TableInfo, Class)}，以便前缀策略与实体列校验对齐 {@code @Indexes}。 */
    @Deprecated
    public static void applyTo(TableInfo table) {
        applyTo(table, null);
    }

    /**
     * Registry 无同表项时立即返回（不碰 Entity 索引）。
     *
     * @param entityClass 实体类；用于 {@link IndexInfo#applyPrefixLengthPolicy} 与 {@link IndexLimitGuard}（与注解路径一致）
     */
    public static void applyTo(TableInfo table, Class<?> entityClass) {
        if (table == null) {
            return;
        }
        String tableName = table.getName();
        if (StringUtils.isBlank(tableName)) {
            return;
        }
        List<IndexInfo> rawQuery = QueryIndexRegistry.forTable(tableName);
        if (rawQuery.isEmpty()) {
            return;
        }

        // 与 TableInfo.initFrom 一致：先按实体列类型收敛前缀，再校验命名/安全
        List<IndexInfo> prepared = new ArrayList<>(rawQuery.size());
        for (IndexInfo q : rawQuery) {
            if (q == null) {
                continue;
            }
            if (entityClass != null) {
                q.applyPrefixLengthPolicy(entityClass);
            }
            prepared.add(q);
        }
        List<IndexInfo> fromQuery = collapsePrefixCovered(prepared);
        if (fromQuery.isEmpty()) {
            return;
        }

        // 仅规范化构建器侧，避免对已校验的 Entity 索引二次 WARN
        IndexAnnotationValidator.normalizeAndValidateAll(fromQuery, tableName, entityClass, 0);

        List<IndexInfo> indexInfos = table.getIndexInfos();
        if (indexInfos == null) {
            indexInfos = new ArrayList<>();
            table.setIndexInfos(indexInfos);
        }
        dropMatching(indexInfos, fromQuery, tableName, "class/field @Index");

        List<IndexInfo> indexColumn = table.getIndexColumn();
        if (indexColumn != null && !indexColumn.isEmpty()) {
            dropMatching(indexColumn, fromQuery, tableName, "@Column(isUnique)/indexColumn");
        }

        for (IndexInfo q : fromQuery) {
            if (q == null || q.getColumnSpecs() == null || q.getColumnSpecs().isEmpty()) {
                continue;
            }
            if (q.isSkipEmit()) {
                log.debug("Query→Index skipEmit: table={} index={}", tableName, q.getName());
                continue;
            }
            boolean dup = false;
            for (IndexInfo existing : indexInfos) {
                if (existing != null && existing.sameIndexColumnsAndKind(q)) {
                    dup = true;
                    break;
                }
            }
            if (!dup) {
                indexInfos.add(q);
            }
        }

        int extra = (indexColumn == null ? 0 : indexColumn.size())
                + (table.getUniqueKeyInfos() == null ? 0 : table.getUniqueKeyInfos().size());
        // 表级配额：合并后再 guard 一次（不会清除已有 skipEmit）
        IndexLimitGuard.guard(indexInfos, tableName, entityClass, extra);
    }

    private static void dropMatching(List<IndexInfo> entitySide, List<IndexInfo> fromQuery,
                                     String tableName, String source) {
        if (entitySide == null || entitySide.isEmpty()) {
            return;
        }
        Iterator<IndexInfo> it = entitySide.iterator();
        while (it.hasNext()) {
            IndexInfo entityIdx = it.next();
            if (entityIdx == null) {
                continue;
            }
            for (IndexInfo q : fromQuery) {
                if (q != null && entityIdx.sameIndexColumnsAndKind(q)) {
                    log.warn("表 [{}] 索引与 Query→Index 同指纹，忽略注解侧({}) name={}，以构建器为准 name={}",
                            tableName, source, entityIdx.getName(), q.getName());
                    it.remove();
                    break;
                }
            }
        }
    }

    /**
     * 构建器多查询之间：较短前缀且类型/方法等兼容时丢弃短索引，保留更长覆盖。
     * UNIQUE 不被普通索引的更长前缀吞掉。
     */
    static List<IndexInfo> collapsePrefixCovered(List<IndexInfo> input) {
        if (input == null || input.size() <= 1) {
            return input == null ? new ArrayList<>() : new ArrayList<>(input);
        }
        List<IndexInfo> list = new ArrayList<>(input);
        List<IndexInfo> kept = new ArrayList<>();
        for (IndexInfo candidate : list) {
            if (candidate == null) {
                continue;
            }
            boolean covered = false;
            for (IndexInfo other : list) {
                if (other == null || other == candidate) {
                    continue;
                }
                if (isPrefixCoveredBy(candidate, other)) {
                    covered = true;
                    break;
                }
            }
            if (!covered) {
                kept.add(candidate);
            }
        }
        return kept;
    }

    /**
     * {@code shorter} 是否被 {@code longer} 前缀覆盖（可安全省略 shorter）。
     */
    static boolean isPrefixCoveredBy(IndexInfo shorter, IndexInfo longer) {
        if (shorter == null || longer == null) {
            return false;
        }
        if (!IndexInfo.indexTypesCompatible(shorter, longer)
                || !IndexInfo.indexMethodsCompatible(shorter, longer)) {
            return false;
        }
        if (isUnique(shorter) && !isUnique(longer)) {
            return false;
        }
        if (!softBlankEqual(shorter.getWherePredicate(), longer.getWherePredicate())) {
            return false;
        }
        if (!softBlankEqual(shorter.getParser(), longer.getParser())) {
            return false;
        }
        List<IndexColumnSpec> a = shorter.getColumnSpecs();
        List<IndexColumnSpec> b = longer.getColumnSpecs();
        if (a == null || b == null || a.isEmpty() || a.size() >= b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            IndexColumnSpec sa = a.get(i);
            IndexColumnSpec sb = b.get(i);
            if (sa == null || sb == null) {
                return false;
            }
            if (!sa.identityKey().equals(sb.identityKey())) {
                return false;
            }
            if (sa.getOrder() != sb.getOrder()) {
                return false;
            }
        }
        return true;
    }

    private static boolean isUnique(IndexInfo info) {
        return info != null && "UNIQUE".equalsIgnoreCase(info.getIndexType());
    }

    private static boolean softBlankEqual(String a, String b) {
        String x = a == null ? "" : a.trim();
        String y = b == null ? "" : b.trim();
        return x.equalsIgnoreCase(y);
    }
}
