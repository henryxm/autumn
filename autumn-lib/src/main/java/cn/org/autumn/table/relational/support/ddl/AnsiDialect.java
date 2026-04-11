package cn.org.autumn.table.relational.support.ddl;

/**
 * 双引号标识符族方言变体，供 {@link AnsiDoubleQuotedDdlGenerator} 使用。
 */
public enum AnsiDialect {
    SQLITE,
    H2,
    HSQLDB,
    DB2,
    DERBY,
    FIREBIRD,
    INFORMIX
}
