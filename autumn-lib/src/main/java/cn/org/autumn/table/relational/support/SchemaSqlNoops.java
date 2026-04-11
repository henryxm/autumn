package cn.org.autumn.table.relational.support;

/**
 * 无副作用占位 SQL：用于当前走 JDBC {@code RelationalTableOperations}、尚未接 MyBatis Provider 的方言，
 * 或 DDL 暂由上层禁用的场景。
 */
public final class SchemaSqlNoops {

    public static final String ANSI_FALSE = "SELECT 1 WHERE FALSE";

    public static final String ORACLE_FAMILY_FALSE = "SELECT 1 FROM DUAL WHERE 1=0";
}
