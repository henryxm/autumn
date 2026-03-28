package cn.org.autumn.table.data;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.utils.HumpConvert;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * MySQL 索引「前缀长度」{@code (`col`(n))} 仅适用于部分字符/二进制串类型；对数值、日期时间等使用前缀会语法错误。
 * 本类在生成 DDL 前统一收敛 {@link IndexInfo} / {@link UniqueKeyInfo} 中的 length 字段，使任意可索引列均可建索引而不因错误前缀失败。
 */
public final class IndexPrefixRules {

    private IndexPrefixRules() {
    }

    /**
     * 判断 MySQL 列类型是否允许在索引定义中使用前缀长度语法 {@code col(n)}。
     */
    public static boolean supportsMysqlPrefixIndex(String sqlType) {
        if (StringUtils.isBlank(sqlType)) {
            return false;
        }
        String t = sqlType.trim().toLowerCase(Locale.ROOT);
        int p = t.indexOf('(');
        if (p > 0) {
            t = t.substring(0, p).trim();
        }
        switch (t) {
            case "char":
            case "varchar":
            case "binary":
            case "varbinary":
            case "text":
            case "tinytext":
            case "mediumtext":
            case "longtext":
            case "blob":
            case "tinyblob":
            case "mediumblob":
            case "longblob":
                return true;
            default:
                return false;
        }
    }

    /**
     * 计算应写入索引定义的前缀长度：非前缀类型或非法请求时返回 0（生成 {@code `col`} 而非 {@code `col`(n)}）。
     *
     * @param field            实体字段，可为 null（仅注解中的列名）
     * @param column           {@link Column}，用于读取 {@link Column#type()}；可为 null
     * @param requestedLength  注解中的长度（如 {@link Column#length()}、{@link cn.org.autumn.table.annotation.IndexField#length()}）
     */
    public static int effectivePrefixLength(Field field, Column column, int requestedLength) {
        if (requestedLength <= 0) {
            return 0;
        }
        if (column != null && StringUtils.isNotBlank(column.type())) {
            // @Column 未写 type 时默认为 varchar，但 Java 为基本类型：不能按 varchar 做前缀
            if (isUnsetVarcharOnNonStringField(field, column)) {
                return prefixLengthFromJavaTypeOnly(field, requestedLength);
            }
            if (!supportsMysqlPrefixIndex(column.type())) {
                return 0;
            }
            return requestedLength;
        }
        if (field == null) {
            return 0;
        }
        return prefixLengthFromJavaTypeOnly(field, requestedLength);
    }

    /**
     * 注解仍为默认 {@link DataType#VARCHAR}，但 Java 类型非字符串系：视为未指定 SQL 类型，前缀按 Java 类型判断。
     */
    private static boolean isUnsetVarcharOnNonStringField(Field field, Column column) {
        if (field == null || column == null) {
            return false;
        }
        if (!DataType.VARCHAR.equalsIgnoreCase(column.type())) {
            return false;
        }
        Class<?> ft = field.getType();
        if (ft == String.class || ft == char[].class || ft == byte[].class) {
            return false;
        }
        if (ft.isEnum()) {
            return false;
        }
        return true;
    }

    private static int prefixLengthFromJavaTypeOnly(Field field, int requestedLength) {
        Class<?> ft = field.getType();
        if (ft == String.class) {
            return requestedLength;
        }
        if (ft == byte[].class) {
            return requestedLength;
        }
        return 0;
    }

    /**
     * 根据实体类字段与 {@link Column} 注解，修正索引条目中各列的前缀长度（类级 {@code @Indexes} / {@code @UniqueKey} 中显式 length 可能误用于数值列）。
     */
    public static void applyPrefixLengthPolicy(Map<String, Integer> fields, Class<?> entityClass) {
        if (fields == null || fields.isEmpty() || entityClass == null) {
            return;
        }
        Field[] declared = entityClass.getDeclaredFields();
        List<String> keys = new ArrayList<>(fields.keySet());
        for (String colKey : keys) {
            Integer old = fields.get(colKey);
            int len = old == null ? 0 : old.intValue();
            Field f = findFieldForColumnKey(declared, colKey);
            Column col = f != null ? f.getAnnotation(Column.class) : null;
            int fixed = effectivePrefixLength(f, col, len);
            if (fixed != len) {
                fields.put(colKey, fixed);
            }
        }
    }

    static Field findFieldForColumnKey(Field[] declared, String columnKey) {
        if (columnKey == null || declared == null) {
            return null;
        }
        for (Field f : declared) {
            Column c = f.getAnnotation(Column.class);
            if (c != null && StringUtils.isNotBlank(c.value())) {
                if (columnKey.equalsIgnoreCase(c.value())) {
                    return f;
                }
            }
        }
        for (Field f : declared) {
            String underline = HumpConvert.HumpToUnderline(f.getName());
            if (columnKey.equalsIgnoreCase(underline)) {
                return f;
            }
        }
        String hump = HumpConvert.UnderlineToHump(columnKey);
        for (Field f : declared) {
            if (f.getName().equalsIgnoreCase(hump)) {
                return f;
            }
        }
        return null;
    }
}
