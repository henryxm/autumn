package cn.org.autumn.table.data.index;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.IndexTypeEnum;
import cn.org.autumn.table.data.IndexInfo;
import cn.org.autumn.table.data.IndexPrefixRules;
import cn.org.autumn.table.utils.HumpConvert;
import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 索引硬限制预检：重复列、列数、未知列、表索引数、键长粗估、isUnique+默认空串。
 * <p>
 * 行为约定：
 * <ul>
 *   <li>列数超限 / 未知列：默认 {@code skipEmit}（DDL 必败）</li>
 *   <li>键长粗估 / 表索引数：默认仅 WARN；{@code skip-on-limit-violation=true} 时才 skip</li>
 *   <li>不因限制校验抛未捕获异常拖垮启动</li>
 * </ul>
 */
@Slf4j
public final class IndexLimitGuard {

    private IndexLimitGuard() {
    }

    /**
     * @param indexes     实体收集的索引（会被原地去重列 / 标记 skipEmit）
     * @param tableName   表名（日志）
     * @param entityClass 实体类（未知列 / isUnique 空串 / 键长粗估）；可 null
     */
    public static void guard(List<IndexInfo> indexes, String tableName, Class<?> entityClass) {
        guard(indexes, tableName, entityClass, 0);
    }

    /**
     * @param extraIndexCount 未并入 {@code indexes} 的内联 UNIQUE / UniqueKey 数量（计入表级阈值）
     */
    public static void guard(List<IndexInfo> indexes, String tableName, Class<?> entityClass, int extraIndexCount) {
        boolean hasIndexes = indexes != null && !indexes.isEmpty();
        if (!hasIndexes && extraIndexCount <= 0) {
            return;
        }
        String table = tableName == null ? "" : tableName;
        Set<String> knownCols = entityClass == null ? null : collectKnownColumns(entityClass);
        int maxCols = IndexTableSettings.maxColumnsPerIndex();
        int maxKeyBytes = IndexTableSettings.maxKeyBytes();
        boolean skipOnLimit = IndexTableSettings.skipOnLimitViolation();
        boolean warnUniqueBlank = IndexTableSettings.warnUniqueBlankDefault();

        if (warnUniqueBlank && entityClass != null) {
            warnUniqueBlankDefaults(entityClass, table);
        }

        if (hasIndexes) {
            for (IndexInfo index : indexes) {
                if (index == null) {
                    continue;
                }
                dedupeColumns(index, table);
                List<IndexColumnSpec> specs = index.getColumnSpecs();
                int colCount = countKeyParts(specs);
                // maxCols<=0 表示关闭列数检查
                if (maxCols > 0 && colCount > maxCols) {
                    log.warn("索引列数超限: table={} index={} columns={} max={}（MySQL 单索引最多 {} 列）→ 跳过发射",
                            table, index.getName(), colCount, maxCols, maxCols);
                    index.setSkipEmit(true);
                }
                if (knownCols != null) {
                    List<String> unknown = findUnknownColumns(specs, knownCols);
                    if (!unknown.isEmpty()) {
                        log.warn("索引含未知列（实体无对应字段）: table={} index={} columns={} → 跳过发射",
                                table, index.getName(), unknown);
                        index.setSkipEmit(true);
                    }
                }
                int keyBytes = estimateKeyBytes(specs, entityClass);
                if (maxKeyBytes > 0 && keyBytes > maxKeyBytes) {
                    log.warn("索引键长粗估超限: table={} index={} approxBytes={} max={}；建议对宽字符串使用 @IndexField(length=191)",
                            table, index.getName(), keyBytes, maxKeyBytes);
                    if (skipOnLimit) {
                        index.setSkipEmit(true);
                    }
                }
                warnFulltextSpatialTypes(index, entityClass, table);
            }
        }

        int maxPerTable = IndexTableSettings.maxPerTable();
        int total = countActiveIndexes(indexes) + Math.max(0, extraIndexCount);
        if (maxPerTable > 0 && total > maxPerTable) {
            log.warn("单表索引数量超过建议阈值: table={} count={} max={}（含字段/组合/@Column(isUnique)）；过大可能导致 Too many keys",
                    table, total, maxPerTable);
            if (skipOnLimit && hasIndexes) {
                int budget = Math.max(0, maxPerTable - Math.max(0, extraIndexCount));
                int keep = 0;
                for (IndexInfo index : indexes) {
                    if (index == null || index.isSkipEmit()) {
                        continue;
                    }
                    keep++;
                    if (keep > budget) {
                        log.warn("索引因表级数量超限跳过发射: table={} index={}", table, index.getName());
                        index.setSkipEmit(true);
                    }
                }
            }
        }
    }

    /**
     * ADD 失败时根据 MySQL 错误信息附加人话原因（不替代原 error）。
     * 优先匹配文案；错误码仅认 {@code ERROR nnnn} / {@code nnnn (} 形态，避免时间戳等数字误伤。
     */
    public static String classifyAddFailure(String sqlError) {
        if (sqlError == null || sqlError.isEmpty()) {
            return "";
        }
        String m = sqlError.toLowerCase(Locale.ROOT);
        if (m.contains("too many key parts") || containsMysqlErrno(m, "1070")) {
            return "超过单索引最大列数（通常 16）";
        }
        if (m.contains("specified key was too long") || containsMysqlErrno(m, "1071")) {
            return "索引键长超限（utf8mb4 下常见上限 3072 字节）；建议加前缀 length";
        }
        if (m.contains("duplicate column") || containsMysqlErrno(m, "1060")) {
            return "索引定义含重复列";
        }
        if (m.contains("duplicate key name") || containsMysqlErrno(m, "1061")) {
            return "索引名冲突";
        }
        if (m.contains("duplicate entry") || containsMysqlErrno(m, "1062")) {
            return "唯一约束与已有数据冲突（常见于多行空串 ''）";
        }
        if (m.contains("too many keys") || containsMysqlErrno(m, "1069")) {
            return "单表索引数量过多";
        }
        if (m.contains("unknown column") || containsMysqlErrno(m, "1054")) {
            return "索引引用了不存在的列";
        }
        return "";
    }

    /** 认 MySQL 常见 {@code ERROR 1070 (42000):} / {@code 1070 (} 形态。 */
    static boolean containsMysqlErrno(String lowerMsg, String code) {
        if (lowerMsg == null || code == null || code.isEmpty()) {
            return false;
        }
        return lowerMsg.contains("error " + code) || lowerMsg.contains(code + " (");
    }

    /** 去重同 index 内重复列（按 identityKey），保留首次出现。 */
    public static void dedupeColumns(IndexInfo index, String table) {
        List<IndexColumnSpec> specs = index.getColumnSpecs();
        if (specs == null || specs.isEmpty()) {
            return;
        }
        Set<String> seen = new LinkedHashSet<>();
        List<IndexColumnSpec> kept = new ArrayList<>();
        List<String> dupes = new ArrayList<>();
        for (IndexColumnSpec spec : specs) {
            if (spec == null) {
                continue;
            }
            String key = spec.identityKey();
            if (!seen.add(key)) {
                dupes.add(spec.isExpression() ? spec.getExpression() : spec.getColumn());
                continue;
            }
            kept.add(spec);
        }
        if (!dupes.isEmpty()) {
            log.warn("索引含重复列已去重保留首次: table={} index={} removed={}", table, index.getName(), dupes);
            index.setColumnSpecs(kept);
        }
    }

    static int countKeyParts(List<IndexColumnSpec> specs) {
        if (specs == null) {
            return 0;
        }
        int n = 0;
        for (IndexColumnSpec s : specs) {
            if (s != null) {
                n++;
            }
        }
        return n;
    }

    static int countActiveIndexes(List<IndexInfo> indexes) {
        if (indexes == null) {
            return 0;
        }
        int n = 0;
        for (IndexInfo i : indexes) {
            if (i != null && !i.isSkipEmit()) {
                n++;
            }
        }
        return n;
    }

    static Set<String> collectKnownColumns(Class<?> entityClass) {
        Set<String> keys = new HashSet<>();
        if (entityClass == null) {
            return keys;
        }
        // 含父类：避免继承列被误判未知而 skipEmit
        for (Field f : IndexPrefixRules.fieldsIncludingSuper(entityClass)) {
            Column c = f.getAnnotation(Column.class);
            keys.add(normalizeColKey(HumpConvert.HumpToUnderline(f.getName())));
            keys.add(normalizeColKey(f.getName()));
            if (c != null && StringUtils.isNotBlank(c.value())) {
                keys.add(normalizeColKey(c.value()));
            }
        }
        return keys;
    }

    static List<String> findUnknownColumns(List<IndexColumnSpec> specs, Set<String> known) {
        List<String> unknown = new ArrayList<>();
        if (specs == null || known == null) {
            return unknown;
        }
        for (IndexColumnSpec spec : specs) {
            if (spec == null || spec.isExpression()) {
                continue;
            }
            String col = spec.getColumn();
            if (StringUtils.isBlank(col)) {
                continue;
            }
            if (!known.contains(normalizeColKey(col))) {
                unknown.add(col);
            }
        }
        return unknown;
    }

    static String normalizeColKey(String s) {
        return s == null ? "" : s.replace("_", "").toLowerCase(Locale.ROOT).trim();
    }

    /**
     * utf8mb4 粗估：字符列按 bytesPerChar * min(prefixOrLen, colLen)；其它类型按固定小常量。
     * <p>须与建表一致：注解默认 {@code type=VARCHAR} 落在 boolean/int 等字段上时，
     * 按非字符串计（勿用 length=255×4），否则会出现 {@code deletedstatusapplevisible} 一类假阳性。
     */
    static int estimateKeyBytes(List<IndexColumnSpec> specs, Class<?> entityClass) {
        if (specs == null || specs.isEmpty()) {
            return 0;
        }
        int total = 0;
        Field[] fields = entityClass == null ? new Field[0] : IndexPrefixRules.fieldsIncludingSuper(entityClass);
        for (IndexColumnSpec spec : specs) {
            if (spec == null || spec.isExpression()) {
                total += 64;
                continue;
            }
            Field f = IndexPrefixRules.findFieldForColumnKey(fields, spec.getColumn());
            Column col = f != null ? f.getAnnotation(Column.class) : null;
            int prefix = Math.max(0, spec.getPrefixLength());
            boolean stringKey = col != null
                    && IndexPrefixRules.supportsMysqlPrefixIndex(col.type())
                    && !IndexPrefixRules.isUnsetVarcharOnNonStringField(f, col);
            if (stringKey) {
                int colLen = Math.max(1, col.length());
                int use = prefix > 0 ? Math.min(prefix, colLen) : colLen;
                total += use * charsetBytesPerChar(col);
            } else if (f != null && f.getType() == String.class) {
                int use = prefix > 0 ? prefix : 255;
                total += use * 4;
            } else {
                total += fixedKeyBytesForField(f);
            }
        }
        return total;
    }

    /** 非字符列键长粗估（对齐常见 InnoDB 存储量级，偏保守即可）。 */
    static int fixedKeyBytesForField(Field f) {
        if (f == null) {
            return 8;
        }
        Class<?> ft = f.getType();
        if (ft == boolean.class || ft == Boolean.class || ft == byte.class || ft == Byte.class) {
            return 1;
        }
        if (ft == short.class || ft == Short.class) {
            return 2;
        }
        if (ft == int.class || ft == Integer.class || ft == float.class || ft == Float.class) {
            return 4;
        }
        if (ft == long.class || ft == Long.class || ft == double.class || ft == Double.class
                || ft == java.util.Date.class || ft == java.sql.Date.class
                || ft == java.sql.Timestamp.class || ft == java.math.BigDecimal.class) {
            return 8;
        }
        return 8;
    }

    private static int charsetBytesPerChar(Column col) {
        if (col == null || col.charset() == null) {
            return 4;
        }
        String cs = col.charset().name();
        if (cs.contains("UTF8MB4") || "INHERIT".equals(cs)) {
            return 4;
        }
        if (cs.contains("UTF8")) {
            return 3;
        }
        return 4;
    }

    /**
     * MySQL UNIQUE 允许多 NULL，但多行 {@code ''} 会冲突。
     * 仅当 {@code defaultValue} 为字面空串时 WARN；可空 + default NULL 不告警。
     */
    static void warnUniqueBlankDefaults(Class<?> entityClass, String table) {
        for (Field f : IndexPrefixRules.fieldsIncludingSuper(entityClass)) {
            Column c = f.getAnnotation(Column.class);
            if (c == null || !c.isUnique()) {
                continue;
            }
            if (!isBlankStringDefault(c.defaultValue())) {
                continue;
            }
            boolean stringLike = f.getType() == String.class
                    || (StringUtils.isNotBlank(c.type()) && IndexPrefixRules.supportsMysqlPrefixIndex(c.type()));
            if (!stringLike) {
                continue;
            }
            log.warn("列 isUnique 且默认空串，多行 '' 会导致 UNIQUE 落库失败: table={} field={} column={} defaultValue={}；建议保证非空唯一值或改用普通 @Index",
                    table, f.getName(),
                    StringUtils.isNotBlank(c.value()) ? c.value() : HumpConvert.HumpToUnderline(f.getName()),
                    c.defaultValue());
        }
    }

    /** {@code ""} / {@code ''} / 空串；不含注解默认的 {@code NULL}。 */
    static boolean isBlankStringDefault(String def) {
        if (def == null) {
            return false;
        }
        String t = def.trim();
        return t.isEmpty() || "\"\"".equals(t) || "''".equals(t);
    }

    static void warnFulltextSpatialTypes(IndexInfo index, Class<?> entityClass, String table) {
        if (index == null || entityClass == null) {
            return;
        }
        String type = index.getIndexType();
        if (!IndexTypeEnum.FULLTEXT.name().equalsIgnoreCase(type)
                && !IndexTypeEnum.SPATIAL.name().equalsIgnoreCase(type)) {
            return;
        }
        Field[] fields = IndexPrefixRules.fieldsIncludingSuper(entityClass);
        for (IndexColumnSpec spec : index.getColumnSpecs()) {
            if (spec == null || spec.isExpression()) {
                continue;
            }
            Field f = IndexPrefixRules.findFieldForColumnKey(fields, spec.getColumn());
            Column col = f != null ? f.getAnnotation(Column.class) : null;
            String colType = col != null ? col.type() : "";
            if (IndexTypeEnum.FULLTEXT.name().equalsIgnoreCase(type)) {
                if (StringUtils.isNotBlank(colType) && !IndexPrefixRules.supportsMysqlPrefixIndex(colType)
                        && f != null && f.getType() != String.class) {
                    log.warn("FULLTEXT 索引列类型可能不合规: table={} index={} column={} type={}",
                            table, index.getName(), spec.getColumn(), colType);
                }
            }
        }
    }
}
