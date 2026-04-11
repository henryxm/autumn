package cn.org.autumn.table.annotation.sql;

/**
 * 目标 SQL 方言（用于配置、DDL 路由与后续多库实现；与业务注解中的引擎/字符集语义配合使用）。
 */
public enum Dialect {

    /** MySQL 5.7+ / 8.x */
    MYSQL,

    /** MariaDB */
    MARIADB,

    /** PostgreSQL（字符集/存储模型与 MySQL 不同，需方言映射） */
    POSTGRESQL,

    /** Oracle */
    ORACLE,

    /** Microsoft SQL Server */
    SQLSERVER,

    /** SQLite */
    SQLITE,

    /** H2 */
    H2,

    /** HyperSQL */
    HSQLDB,

    /** IBM DB2 */
    DB2,

    /** Apache Derby */
    DERBY,

    /** Firebird */
    FIREBIRD,

    /** Informix */
    INFORMIX,

    /** 达梦 DM */
    DAMENG,

    /** 人大金仓 KingbaseES */
    KINGBASE,

    /** PingCAP TiDB */
    TIDB,

    /** OceanBase MySQL 兼容模式 */
    OCEANBASE_MYSQL,

    /** OceanBase Oracle 兼容模式 */
    OCEANBASE_ORACLE,

    /** 其他或未分类 */
    OTHER
}
