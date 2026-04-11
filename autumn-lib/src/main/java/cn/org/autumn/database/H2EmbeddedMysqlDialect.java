package cn.org.autumn.database;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 识别「内嵌 H2 + MySQL 兼容」：主数据源 JDBC URL 为 {@code jdbc:h2:...} 且含 {@code MODE=MySQL}（不区分大小写）。
 */
public final class H2EmbeddedMysqlDialect {

    private static final Pattern MODE_PARAM = Pattern.compile("(?i)[;?&]MODE=([^;&]+)");

    private H2EmbeddedMysqlDialect() {
    }

    public static boolean isJdbcH2(String jdbcUrl) {
        return jdbcUrl != null && jdbcUrl.trim().toLowerCase(Locale.ROOT).startsWith("jdbc:h2:");
    }

    /**
     * URL 中是否显式声明 H2 的 MySQL 兼容模式（{@code MODE=MySQL}）。
     */
    public static boolean urlDeclaresMysqlCompatibilityMode(String jdbcUrl) {
        if (!isJdbcH2(jdbcUrl)) {
            return false;
        }
        Matcher m = MODE_PARAM.matcher(jdbcUrl);
        if (!m.find()) {
            return false;
        }
        String v = m.group(1).trim();
        return "mysql".equalsIgnoreCase(v);
    }

    /**
     * 是否为「H2 + MySQL 兼容模式」连接（与 {@link DatabaseHolder#resolveType}、{@link cn.org.autumn.table.relational.provider.EmbeddedH2MysqlMode} 一致）。
     */
    public static boolean isActive(String jdbcUrl) {
        return urlDeclaresMysqlCompatibilityMode(jdbcUrl);
    }
}
