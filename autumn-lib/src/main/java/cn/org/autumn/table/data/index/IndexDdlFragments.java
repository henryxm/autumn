package cn.org.autumn.table.data.index;

import cn.org.autumn.table.annotation.IndexOrder;
import cn.org.autumn.table.data.IndexInfo;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 各方言共用的索引列 DDL 片段拼装（保留列序与 ASC/DESC）。
 */
public final class IndexDdlFragments {

    private IndexDdlFragments() {
    }

    /**
     * MySQL 风格：`col`(n) ASC，表达式列直接输出 expression。
     */
    public static void appendMysqlColumns(StringBuilder sb, IndexInfo indexInfo) {
        List<IndexColumnSpec> specs = indexInfo.getColumnSpecs();
        if (specs != null && !specs.isEmpty()) {
            for (int i = 0; i < specs.size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                appendMysqlColumn(sb, specs.get(i));
            }
            return;
        }
        // 兼容仅有 fields Map 的旧路径
        Iterator<Map.Entry<String, Integer>> iterator = indexInfo.getFields().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Integer> kv = iterator.next();
            appendMysqlColumn(sb, IndexColumnSpec.ofColumn(kv.getKey(), kv.getValue() == null ? 0 : kv.getValue()));
            if (iterator.hasNext()) {
                sb.append(",");
            }
        }
    }

    public static void appendMysqlColumn(StringBuilder sb, IndexColumnSpec spec) {
        if (spec.isExpression()) {
            sb.append("(").append(spec.getExpression()).append(")");
        } else {
            sb.append("`").append(spec.getColumn()).append("`");
            if (spec.getPrefixLength() > 0) {
                sb.append("(").append(spec.getPrefixLength()).append(")");
            }
        }
        if (spec.getOrder() == IndexOrder.DESC) {
            sb.append(" DESC");
        } else {
            sb.append(" ASC");
        }
    }

    /**
     * 双引号标识符方言（PG/ANSI）：忽略 MySQL 前缀长度。
     */
    public static void appendQuotedColumns(StringBuilder sb, IndexInfo indexInfo, boolean emitOrder) {
        List<IndexColumnSpec> specs = indexInfo.getColumnSpecs();
        if (specs == null || specs.isEmpty()) {
            Iterator<Map.Entry<String, Integer>> it = indexInfo.getFields().entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Integer> e = it.next();
                sb.append('"').append(e.getKey()).append('"');
                if (emitOrder) {
                    sb.append(" ASC");
                }
                if (it.hasNext()) {
                    sb.append(", ");
                }
            }
            return;
        }
        for (int i = 0; i < specs.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            IndexColumnSpec spec = specs.get(i);
            if (spec.isExpression()) {
                sb.append("(").append(spec.getExpression()).append(")");
            } else {
                sb.append('"').append(spec.getColumn()).append('"');
            }
            if (emitOrder) {
                sb.append(" ").append(spec.getOrder().sql());
            }
        }
    }
}
