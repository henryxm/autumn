package cn.org.autumn.database;

import org.apache.commons.lang.StringUtils;

/**
 * 应用主库类型（优先由 JDBC URL 推断，其次 {@code autumn.database}）。
 * <ul>
 *   <li>MySQL / MariaDB：共用 {@link cn.org.autumn.table.platform.mysql.MysqlRelationalTableOperations} 与 {@link cn.org.autumn.database.runtime.MysqlRuntimeSqlDialect}；分页方言 MariaDB 单独为 {@code mariadb}。</li>
 *   <li>PostgreSQL：独立建表与运行时方言。</li>
 *   <li>Oracle / SQL Server：运行时方言与 JDBC 元数据/DROP；注解建表 DDL 未接入，见 {@link #supportsAnnotationTableSync()}。</li>
 * </ul>
 */
public enum DatabaseType {

    MYSQL,
    MARIADB,
    POSTGRESQL,
    ORACLE,
    SQLSERVER,
    /** 未识别的配置值 */
    OTHER;

    /**
     * 是否走注解驱动建表（{@code MysqlTableService} + 路由后的 {@link cn.org.autumn.table.platform.RelationalTableOperations}）。
     */
    public boolean supportsAnnotationTableSync() {
        return this == MYSQL || this == MARIADB || this == POSTGRESQL;
    }

    public boolean isPostgresql() {
        return this == POSTGRESQL;
    }

    public boolean isMariaDb() {
        return this == MARIADB;
    }

    public boolean isOracle() {
        return this == ORACLE;
    }

    public boolean isSqlServer() {
        return this == SQLSERVER;
    }

    public boolean isMysqlFamily() {
        return this == MYSQL || this == MARIADB;
    }

    /**
     * PageHelper {@code helper-dialect} 取值，与 JDBC URL 推断结果一致时无需再配 {@code pagehelper.helper-dialect}。
     */
    public String pageHelperDialectName() {
        switch (this) {
            case MYSQL:
                return "mysql";
            case MARIADB:
                return "mariadb";
            case POSTGRESQL:
                return "postgresql";
            case ORACLE:
                return "oracle";
            case SQLSERVER:
                return "sqlserver";
            default:
                return "mysql";
        }
    }

    /**
     * 解析配置值：mysql、mariadb、postgresql、postgres、oracle、sqlserver、mssql；其余为 {@link #OTHER}。
     */
    public static DatabaseType fromConfig(String raw) {
        if (StringUtils.isBlank(raw)) {
            return MYSQL;
        }
        String v = raw.trim().toLowerCase();
        switch (v) {
            case "mariadb":
                return MARIADB;
            case "postgresql":
            case "postgres":
                return POSTGRESQL;
            case "oracle":
                return ORACLE;
            case "sqlserver":
            case "mssql":
                return SQLSERVER;
            case "mysql":
                return MYSQL;
            default:
                return OTHER;
        }
    }
}
