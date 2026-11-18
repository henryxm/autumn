package cn.org.autumn.table.data.index;

import cn.org.autumn.table.annotation.IndexOrder;

import java.util.Locale;
import java.util.Objects;

/**
 * 索引中的一列（有序）：列名或表达式、前缀长度、ASC/DESC。
 */
public final class IndexColumnSpec {

    private final String column;
    private final String expression;
    private final int prefixLength;
    private final IndexOrder order;
    private final String collation;

    private IndexColumnSpec(String column, String expression, int prefixLength, IndexOrder order, String collation) {
        this.column = column == null ? "" : column;
        this.expression = expression == null ? "" : expression;
        this.prefixLength = Math.max(0, prefixLength);
        this.order = order == null ? IndexOrder.ASC : order;
        this.collation = collation == null ? "" : collation;
    }

    public static IndexColumnSpec ofColumn(String column, int prefixLength, IndexOrder order) {
        return ofColumn(column, prefixLength, order, "");
    }

    public static IndexColumnSpec ofColumn(String column, int prefixLength, IndexOrder order, String collation) {
        return new IndexColumnSpec(column, "", prefixLength, order, collation);
    }

    public static IndexColumnSpec ofColumn(String column, int prefixLength) {
        return ofColumn(column, prefixLength, IndexOrder.ASC);
    }

    public static IndexColumnSpec ofExpression(String expression, IndexOrder order) {
        return new IndexColumnSpec("", expression, 0, order, "");
    }

    public IndexColumnSpec withPrefixLength(int length) {
        return new IndexColumnSpec(column, expression, length, order, collation);
    }

    public IndexColumnSpec withOrder(IndexOrder o) {
        return new IndexColumnSpec(column, expression, prefixLength, o, collation);
    }

    public boolean isExpression() {
        return expression != null && !expression.isEmpty();
    }

    public String getColumn() {
        return column;
    }

    public String getExpression() {
        return expression;
    }

    public int getPrefixLength() {
        return prefixLength;
    }

    public IndexOrder getOrder() {
        return order;
    }

    public String getCollation() {
        return collation;
    }

    /** 比对用键：表达式优先，否则归一化列名。 */
    public String identityKey() {
        if (isExpression()) {
            return "expr:" + expression.replace(" ", "").toLowerCase(Locale.ROOT);
        }
        return column == null ? "" : column.replace("_", "").toLowerCase(Locale.ROOT).trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IndexColumnSpec)) {
            return false;
        }
        IndexColumnSpec that = (IndexColumnSpec) o;
        return prefixLength == that.prefixLength
                && Objects.equals(identityKey(), that.identityKey())
                && order == that.order
                && Objects.equals(collation, that.collation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identityKey(), prefixLength, order, collation);
    }
}
