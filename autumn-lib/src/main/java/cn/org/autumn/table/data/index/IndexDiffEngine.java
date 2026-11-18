package cn.org.autumn.table.data.index;

import cn.org.autumn.table.annotation.IndexOrder;
import cn.org.autumn.table.data.IndexInfo;

import lombok.extern.slf4j.Slf4j;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * 索引差异判定、配对与重建分类。
 * <p>
 * 身份以<strong>定义指纹</strong>（{@link IndexInfo#sameIndexColumnsAndKind}）为准；
 * 名字仅作配对回退与落库标识（规范化：忽略大小写与下划线）。
 */
@Slf4j
public final class IndexDiffEngine {

    private IndexDiffEngine() {
    }

    /** @return true 表示定义一致，无需迁移 */
    public static boolean sameDefinition(IndexInfo expected, IndexInfo actual) {
        return expected != null && expected.sameIndexColumnsAndKind(actual);
    }

    /** 含 schema-scoped 索引名别名规则 */
    public static boolean matches(String tableName, IndexInfo expected, IndexInfo actual) {
        return expected != null && expected.matchesForRelationalSchemaScopedIndex(tableName, actual);
    }

    /**
     * 已配对（名或指纹）后的升级分类。
     *
     * @return {@code null} 表示定义与规范化名均一致，无需迁移；
     *         {@link IndexRebuildTask.Reason#NAME} 仅物理名不同；
     *         {@link IndexRebuildTask.Reason#ORDER} / {@link IndexRebuildTask.Reason#DEFINITION} 需 DROP+ADD
     */
    public static IndexRebuildTask.Reason classifyAfterPair(String tableName, IndexInfo entity, IndexInfo db) {
        if (entity == null || db == null) {
            return IndexRebuildTask.Reason.DEFINITION;
        }
        if (sameDefinition(entity, db)) {
            if (IndexInfo.relationalSchemaScopedIndexNamesMatch(tableName, entity.getName(), db.getName())) {
                return null;
            }
            return IndexRebuildTask.Reason.NAME;
        }
        return classifyRebuild(entity, db);
    }

    /**
     * 同名且当前比对规则下不一致时，判定重建原因。
     * 列集合相同（忽略顺序）→ {@link IndexRebuildTask.Reason#ORDER}，否则 {@link IndexRebuildTask.Reason#DEFINITION}。
     */
    public static IndexRebuildTask.Reason classifyRebuild(IndexInfo entity, IndexInfo db) {
        if (entity == null || db == null) {
            return IndexRebuildTask.Reason.DEFINITION;
        }
        if (sameTypeAndMethod(entity, db) && sameColumnSetIgnoringOrder(entity.getColumnSpecs(), db.getColumnSpecs())) {
            return IndexRebuildTask.Reason.ORDER;
        }
        return IndexRebuildTask.Reason.DEFINITION;
    }

    public static boolean sameTypeAndMethod(IndexInfo a, IndexInfo b) {
        if (a == null || b == null) {
            return false;
        }
        return IndexInfo.indexTypesCompatible(a, b) && IndexInfo.indexMethodsCompatible(a, b);
    }

    public static boolean sameColumnSetIgnoringOrder(List<IndexColumnSpec> a, List<IndexColumnSpec> b) {
        if (a == null) {
            a = Collections.emptyList();
        }
        if (b == null) {
            b = Collections.emptyList();
        }
        if (a.size() != b.size()) {
            return false;
        }
        Set<String> keys = new HashSet<>();
        for (IndexColumnSpec s : b) {
            keys.add(s.identityKey());
        }
        for (IndexColumnSpec s : a) {
            if (!keys.contains(s.identityKey())) {
                return false;
            }
        }
        return true;
    }

    /** 日志用短列序描述，如 {@code tags ASC,enabled(32) ASC} */
    public static String formatColumns(IndexInfo index) {
        if (index == null) {
            return "-";
        }
        List<IndexColumnSpec> specs = index.getColumnSpecs();
        if (specs == null || specs.isEmpty()) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < specs.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            IndexColumnSpec s = specs.get(i);
            if (s.isExpression()) {
                sb.append(s.getExpression());
            } else {
                sb.append(s.getColumn());
                if (s.getPrefixLength() > 0) {
                    sb.append('(').append(s.getPrefixLength()).append(')');
                }
            }
            IndexOrder o = s.getOrder() == null ? IndexOrder.ASC : s.getOrder();
            sb.append(' ').append(o.sql());
        }
        return sb.toString();
    }

    /** @return true 表示定义不一致、需要 DROP+ADD */
    public static boolean isMeaningfulRebuild(IndexInfo entity, IndexInfo db) {
        return entity != null && db != null && !entity.sameIndexColumnsAndKind(db);
    }

    /** 在库内索引集合中按表名规则查找同名索引（规范化名：忽略大小写与 {@code _}）。 */
    public static IndexInfo findByName(String tableName, Iterable<IndexInfo> dbIndexes, IndexInfo entity) {
        if (dbIndexes == null || entity == null) {
            return null;
        }
        for (IndexInfo db : dbIndexes) {
            if (db != null && IndexInfo.relationalSchemaScopedIndexNamesMatch(tableName, entity.getName(), db.getName())) {
                return db;
            }
        }
        return null;
    }

    public static boolean nameMatchesAny(String tableName, IndexInfo db, Iterable<IndexInfo> entities) {
        if (db == null || entities == null) {
            return false;
        }
        for (IndexInfo e : entities) {
            if (e != null && IndexInfo.relationalSchemaScopedIndexNamesMatch(tableName, e.getName(), db.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 在剩余库索引中按定义指纹查找唯一匹配。
     */
    public static DefinitionMatch findUniqueByDefinition(IndexInfo entity, Iterable<IndexInfo> remaining) {
        if (entity == null || remaining == null) {
            return DefinitionMatch.none();
        }
        IndexInfo found = null;
        int hits = 0;
        for (IndexInfo db : remaining) {
            if (db != null && sameDefinition(entity, db)) {
                hits++;
                found = db;
                if (hits > 1) {
                    return DefinitionMatch.ambiguous();
                }
            }
        }
        if (hits == 1) {
            return DefinitionMatch.unique(found);
        }
        return DefinitionMatch.none();
    }

    /**
     * 实体↔库配对：先规范化名，再指纹唯一匹配。
     *
     * @param remainingDb 尚未配对的库索引（调用方在命中后应 remove）
     */
    public static PairOutcome pair(String tableName, IndexInfo entity, List<IndexInfo> remainingDb) {
        if (entity == null) {
            return PairOutcome.unpaired();
        }
        IndexInfo byName = findByName(tableName, remainingDb, entity);
        if (byName != null) {
            return PairOutcome.byName(byName);
        }
        DefinitionMatch def = findUniqueByDefinition(entity, remainingDb);
        if (def.kind == DefinitionMatch.Kind.AMBIGUOUS) {
            log.warn("索引指纹一对多，跳过自动改名配对: table={} entityIndex={} columns=[{}]",
                    tableName, entity.getName(), formatColumns(entity));
            return PairOutcome.ambiguous();
        }
        if (def.kind == DefinitionMatch.Kind.UNIQUE) {
            return PairOutcome.byDefinition(def.match);
        }
        return PairOutcome.unpaired();
    }

    /** 从 remaining 中按引用移除已配对实例。 */
    public static void removePaired(List<IndexInfo> remaining, IndexInfo paired) {
        if (remaining == null || paired == null) {
            return;
        }
        Iterator<IndexInfo> it = remaining.iterator();
        while (it.hasNext()) {
            if (it.next() == paired) {
                it.remove();
                return;
            }
        }
    }

    public static final class DefinitionMatch {
        public enum Kind {
            NONE,
            UNIQUE,
            AMBIGUOUS
        }

        public final Kind kind;
        public final IndexInfo match;

        private DefinitionMatch(Kind kind, IndexInfo match) {
            this.kind = kind;
            this.match = match;
        }

        public static DefinitionMatch none() {
            return new DefinitionMatch(Kind.NONE, null);
        }

        public static DefinitionMatch unique(IndexInfo match) {
            return new DefinitionMatch(Kind.UNIQUE, match);
        }

        public static DefinitionMatch ambiguous() {
            return new DefinitionMatch(Kind.AMBIGUOUS, null);
        }
    }

    public static final class PairOutcome {
        public enum Kind {
            UNPAIRED,
            BY_NAME,
            BY_DEFINITION,
            AMBIGUOUS
        }

        public final Kind kind;
        public final IndexInfo dbIndex;

        private PairOutcome(Kind kind, IndexInfo dbIndex) {
            this.kind = kind;
            this.dbIndex = dbIndex;
        }

        public static PairOutcome unpaired() {
            return new PairOutcome(Kind.UNPAIRED, null);
        }

        public static PairOutcome byName(IndexInfo db) {
            return new PairOutcome(Kind.BY_NAME, db);
        }

        public static PairOutcome byDefinition(IndexInfo db) {
            return new PairOutcome(Kind.BY_DEFINITION, db);
        }

        public static PairOutcome ambiguous() {
            return new PairOutcome(Kind.AMBIGUOUS, null);
        }

        public boolean paired() {
            return kind == Kind.BY_NAME || kind == Kind.BY_DEFINITION;
        }
    }
}
