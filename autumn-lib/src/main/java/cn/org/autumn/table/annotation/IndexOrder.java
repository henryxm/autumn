package cn.org.autumn.table.annotation;

/**
 * 索引列排序方向（写入 DDL 的 ASC/DESC）。
 * <p>
 * 默认 {@link #ASC}。字段级：{@code @Index(order = DESC)}；组合：{@code @IndexField(order = DESC)}。
 * 用法与性能见 {@code docs/AI_STANDARDS.md} §10.2「列方向 ASC / DESC」。
 */
public enum IndexOrder {
    ASC,
    DESC;

    public static IndexOrder fromCollation(String collation) {
        if (collation == null) {
            return ASC;
        }
        String c = collation.trim();
        if ("D".equalsIgnoreCase(c) || "DESC".equalsIgnoreCase(c)) {
            return DESC;
        }
        return ASC;
    }

    public String sql() {
        return name();
    }
}
