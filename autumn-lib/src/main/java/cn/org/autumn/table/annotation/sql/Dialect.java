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

    /** H2 等嵌入式库 */
    H2,

    /** 其他或未分类 */
    OTHER
}
