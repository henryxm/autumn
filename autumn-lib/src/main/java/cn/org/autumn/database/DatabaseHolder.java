package cn.org.autumn.database;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 持有当前生效的 {@link DatabaseType}，供路由数据源、分页方言、建表适配等使用。
 */
@Component
public class DatabaseHolder {

    @Value("${autumn.database:}")
    private String databaseRaw;

    @Autowired(required = false)
    private Environment environment;

    public DatabaseType getType() {
        if (environment != null) {
            String url = environment.getProperty("spring.datasource.druid.first.url");
            if (StringUtils.isBlank(url)) {
                url = environment.getProperty("spring.datasource.url");
            }
            return resolveType(url, databaseRaw);
        }
        return resolveType(null, databaseRaw);
    }

    /**
     * 与 {@link #getType()} 相同解析规则，供无 Spring 注入场景（如 {@link cn.org.autumn.database.runtime.RuntimeSqlDialectRegistry}）使用。
     * <p>
     * {@code jdbc:h2:...} 且 URL 含 {@code MODE=MySQL} 时返回 {@link DatabaseType#MYSQL}（内嵌 MySQL 兼容 H2）。
     */
    public static DatabaseType resolveType(String jdbcUrl, String autumnDatabaseRaw) {
        String u = jdbcUrl == null ? "" : jdbcUrl.trim();
        if (H2EmbeddedMysqlDialect.isActive(u)) {
            return DatabaseType.MYSQL;
        }
        DatabaseType fromUrl = inferFromJdbcUrl(u);
        if (StringUtils.isNotBlank(autumnDatabaseRaw)) {
            DatabaseType cfg = DatabaseType.fromConfig(autumnDatabaseRaw);
            String ul = u.toLowerCase(Locale.ROOT);
            if (cfg == DatabaseType.OCEANBASE_ORACLE && ul.startsWith("jdbc:oceanbase:")) {
                return DatabaseType.OCEANBASE_ORACLE;
            }
            if (fromUrl == DatabaseType.MYSQL) {
                if (cfg == DatabaseType.TIDB || cfg == DatabaseType.OCEANBASE_MYSQL) {
                    return cfg;
                }
            }
        }
        if (fromUrl != null) {
            return fromUrl;
        }
        return StringUtils.isBlank(autumnDatabaseRaw) ? DatabaseType.MYSQL : DatabaseType.fromConfig(autumnDatabaseRaw);
    }

    /**
     * 从 JDBC URL 推断库类型；无法识别时返回 null。
     * <p>
     * TiDB 若使用官方 {@code jdbc:mysql://}，URL  alone 与 MySQL 不可区分，请配置 {@code autumn.database=tidb}。
     */
    public static DatabaseType inferFromJdbcUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return null;
        }
        String u = url.trim().toLowerCase(Locale.ROOT);
        if (u.startsWith("jdbc:kingbase8:") || u.startsWith("jdbc:kingbase86:")) {
            return DatabaseType.KINGBASE;
        }
        if (u.startsWith("jdbc:oceanbase:")) {
            return isOceanBaseOracleJdbcUrl(u) ? DatabaseType.OCEANBASE_ORACLE : DatabaseType.OCEANBASE_MYSQL;
        }
        if (u.startsWith("jdbc:tidb:")) {
            return DatabaseType.TIDB;
        }
        if (u.startsWith("jdbc:postgresql:") || u.startsWith("jdbc:pgsql:")) {
            return DatabaseType.POSTGRESQL;
        }
        if (u.startsWith("jdbc:oracle:")) {
            return DatabaseType.ORACLE;
        }
        if (u.startsWith("jdbc:sqlserver:") || u.startsWith("jdbc:microsoft:sqlserver:")) {
            return DatabaseType.SQLSERVER;
        }
        if (u.startsWith("jdbc:informix-sqli:")) {
            return DatabaseType.INFORMIX;
        }
        if (u.startsWith("jdbc:informix:")) {
            return DatabaseType.INFORMIX;
        }
        if (u.startsWith("jdbc:firebirdsql:")) {
            return DatabaseType.FIREBIRD;
        }
        if (u.startsWith("jdbc:firebird:")) {
            return DatabaseType.FIREBIRD;
        }
        if (u.startsWith("jdbc:dm:") || u.startsWith("jdbc:dm8:")) {
            return DatabaseType.DAMENG;
        }
        if (u.startsWith("jdbc:db2:")) {
            return DatabaseType.DB2;
        }
        if (u.startsWith("jdbc:derby:")) {
            return DatabaseType.DERBY;
        }
        if (u.startsWith("jdbc:hsqldb:")) {
            return DatabaseType.HSQLDB;
        }
        if (u.startsWith("jdbc:h2:")) {
            return DatabaseType.H2;
        }
        if (u.startsWith("jdbc:sqlite:")) {
            return DatabaseType.SQLITE;
        }
        if (u.startsWith("jdbc:mariadb:")) {
            return DatabaseType.MARIADB;
        }
        if (u.startsWith("jdbc:mysql:")) {
            return DatabaseType.MYSQL;
        }
        return null;
    }

    /**
     * OceanBase Connector/J：Oracle 兼容模式可在 URL 参数中声明 {@code compatibleMode=oracle} 或 {@code compatible-mode=oracle}（不区分大小写）。
     */
    static boolean isOceanBaseOracleJdbcUrl(String urlLowercase) {
        int q = urlLowercase.indexOf('?');
        if (q < 0) {
            return false;
        }
        String qry = urlLowercase.substring(q + 1).replace(" ", "");
        return qry.contains("compatiblemode=oracle") || qry.contains("compatible-mode=oracle");
    }
}
