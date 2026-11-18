package cn.org.autumn.table.data.index;

import cn.org.autumn.table.annotation.IndexOrder;
import cn.org.autumn.table.data.IndexInfo;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 索引差异判定与重建分类。
 */
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

    /** 在库内索引集合中按表名规则查找同名索引 */
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
}
