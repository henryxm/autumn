package cn.org.autumn.database;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 识别「内嵌 H2 + MySQL 兼容」：JDBC URL 为 {@code jdbc:h2:...} 且含 {@code MODE=MySQL}（不区分大小写）。
 * <p>
 * <b>与 {@code MODE!=MySQL} 的分工</b>：未声明或声明为其它 {@code MODE}（如 PostgreSQL、Oracle）时，
 * {@link cn.org.autumn.database.DatabaseHolder#inferFromJdbcUrl} 将类型视为 {@link DatabaseType#H2}，
 * 注解建表走 {@link cn.org.autumn.table.relational.dialect.h2.H2NativeRelationalSchemaSql}（双引号标识符 + ANSI 类型映射），
 * <b>不要</b>再套用本类的 MySQL 反引号 / {@code information_schema} 路径。仅当业务刻意使用
 * {@code MODE=MySQL} 模拟 MySQL 时，才与 {@link cn.org.autumn.database.DatabaseHolder#resolveType} 中
 * 「H2+MySQL 模式 → {@link DatabaseType#MYSQL}」及 {@link cn.org.autumn.table.relational.provider.EmbeddedH2MysqlMode} 对齐。
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
