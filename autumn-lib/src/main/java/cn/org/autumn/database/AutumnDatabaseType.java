package cn.org.autumn.database;

import org.apache.commons.lang.StringUtils;

/**
 * 应用主库类型（与 {@code autumn.database} 对齐）。MySQL/MariaDB 共用一套 JDBC 方言实现；PostgreSQL 独立实现。
 * Oracle、SQL Server 等可先配置类型以便跳过注解建表，后续再接 {@link cn.org.autumn.table.platform.RelationalTableOperations} 实现。
 */
public enum AutumnDatabaseType {

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

    public boolean isMysqlFamily() {
        return this == MYSQL || this == MARIADB;
    }

    /**
     * 解析配置值：mysql、mariadb、postgresql、postgres、oracle、sqlserver、mssql；其余为 {@link #OTHER}。
     */
    public static AutumnDatabaseType fromConfig(String raw) {
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
