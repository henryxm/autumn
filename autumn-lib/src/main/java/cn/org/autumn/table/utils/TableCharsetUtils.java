package cn.org.autumn.table.utils;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 表字符集 / 排序规则校验与规范化（用于与 information_schema 比较及拼接 DDL）。
 */
public final class TableCharsetUtils {

    private static final Pattern MYSQL_CHARSET_OR_COLLATION_NAME = Pattern.compile("^[A-Za-z0-9_]+$");

    private TableCharsetUtils() {
    }

    /**
     * MySQL 字符集、排序规则名仅含字母数字与下划线，防止注解被误写导致 SQL 注入。
     */
    /**
     * 校验字符集/排序规则标识符（与主方言 DDL 拼接前使用）。
     */
    public static boolean isSafeSqlCharsetOrCollationName(String name) {
        return name != null && !name.isEmpty() && MYSQL_CHARSET_OR_COLLATION_NAME.matcher(name).matches();
    }

    /**
     * 与 {@link #sameCharset(String, String)} 配套：规范化后再比较。
     */
    public static String normalizeCharsetName(String charset) {
        if (charset == null) {
            return "";
        }
        String n = charset.trim().toLowerCase(Locale.ROOT);
        if (n.isEmpty()) {
            return "";
        }
        // MySQL 8.0 中 utf8 与 utf8mb3 为同一套 3 字节 UTF-8 实现，information_schema 可能返回 utf8mb3
        if ("utf8mb3".equals(n)) {
            return "utf8";
        }
        return n;
    }

    public static boolean sameCharset(String a, String b) {
        return normalizeCharsetName(a).equals(normalizeCharsetName(b));
    }

    public static String normalizeCollationName(String collation) {
        if (collation == null || collation.isEmpty()) {
            return "";
        }
        return collation.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean sameCollation(String a, String b) {
        return normalizeCollationName(a).equals(normalizeCollationName(b));
    }
}
