package cn.org.autumn.database;

import cn.org.autumn.datasources.DataSourceNames;
import cn.org.autumn.datasources.DynamicDataSource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 持有当前应用的 {@link DatabaseType}（默认与<strong>主数据源</strong>一致），供分页插件全局配置、建表、部分 TypeHandler 等使用。
 * <p>
 * 多数据源下：{@link #getType()} 仍解析<strong>主库</strong> JDBC URL（与 {@link #readPrimaryJdbcUrl} 一致），因为 MyBatis-Plus
 * 全局 {@code column-format}、{@link com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor} 等仅为
 * 单工厂配置，无法按连接切换。运行期手写 SQL / {@link cn.org.autumn.database.runtime.RuntimeSqlDialect} 则须按<strong>当前线程路由</strong>
 * 取 URL：{@link #readCurrentRoutingJdbcUrl(Environment)}、{@link #resolveTypeForCurrentRouting()}，避免把主库方言套在从库连接上。
 * <p>
 * <b>内嵌 H2 + {@code MODE=MySQL}</b>：{@link #resolveType(String, String)} 仍可对应该 URL 返回 {@link DatabaseType#MYSQL}（注解建表）；
 * 运行期引用由 {@link cn.org.autumn.database.runtime.RoutingRuntimeSqlDialect} 按 URL 选用 {@link cn.org.autumn.database.runtime.H2RuntimeSqlDialect}。
 */
@Component
public class DatabaseHolder {

    @Value("${autumn.database:}")
    private String databaseRaw;

    @Autowired(required = false)
    private Environment environment;

    /**
     * 与 {@link #getType()} 使用相同属性键读取主数据源 JDBC URL；供 {@link cn.org.autumn.database.runtime.RuntimeSqlDialectRegistry}、
     * {@link cn.org.autumn.database.runtime.RoutingRuntimeSqlDialect} 等与 {@code DatabaseType} 解耦的场合复用。
     */
    public static String readPrimaryJdbcUrl(Environment environment) {
        if (environment == null) {
            return "";
        }
        String url = environment.getProperty("spring.datasource.druid.first.url");
        if (StringUtils.isBlank(url)) {
            url = environment.getProperty("spring.datasource.url");
        }
        return url == null ? "" : url.trim();
    }

    /**
     * 当前线程在 {@link DynamicDataSource} 下选中的数据源的 JDBC URL：{@link DataSourceNames#SECOND} 且配置了
     * {@code spring.datasource.druid.second.url} 时用从库 URL，否则与 {@link #readPrimaryJdbcUrl(Environment)} 相同。
     * <p>
     * 未进入路由切面、ThreadLocal 为空时视为使用主数据源。
     */
    public static String readCurrentRoutingJdbcUrl(Environment environment) {
        if (environment == null) {
            return "";
        }
        String key = DynamicDataSource.getDataSource();
        if (DataSourceNames.SECOND.equals(key)) {
            String second = environment.getProperty("spring.datasource.druid.second.url");
            if (StringUtils.isNotBlank(second)) {
                return second.trim();
            }
        }
        return readPrimaryJdbcUrl(environment);
    }

    public DatabaseType getType() {
        if (environment != null) {
            return resolveType(readPrimaryJdbcUrl(environment), databaseRaw);
        }
        return resolveType(null, databaseRaw);
    }

    /**
     * 按 {@link #readCurrentRoutingJdbcUrl(Environment)} 与 {@code autumn.database} 解析类型，供运行期 SQL / TypeHandler 与主库聚合类型区分时使用。
     */
    public DatabaseType resolveTypeForCurrentRouting() {
        if (environment != null) {
            return resolveType(readCurrentRoutingJdbcUrl(environment), databaseRaw);
        }
        return getType();
    }

    /**
     * 供同模块内其它 Bean 在仅有 {@link DatabaseHolder} 引用时取得 {@link Environment}（如方言路由退化路径）。
     */
    public Environment getEnvironment() {
        return environment;
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
