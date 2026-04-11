package cn.org.autumn.database;

/**
 * 注解驱动建表（{@link cn.org.autumn.table.service.MysqlTableService} + {@link cn.org.autumn.table.platform.RelationalTableOperations}）
 * 的库级能力，与 {@link DatabaseType#supportsAnnotationTableSync()} 对齐。
 * <p>
 * <b>已接入（端到端 DDL + 元数据）</b>
 * <ul>
 *   <li><b>MySQL 协议族</b>：{@link DatabaseType#MYSQL}、{@link DatabaseType#MARIADB}、{@link DatabaseType#TIDB}、
 *   {@link DatabaseType#OCEANBASE_MYSQL} —— TableDao + {@code MysqlSchemaSql}</li>
 *   <li><b>PostgreSQL 协议族</b>：{@link DatabaseType#POSTGRESQL}、{@link DatabaseType#KINGBASE} ——
 *   PostgresTableDao + {@code PostgresRelationalSchemaSql}（Kingbase 按 PG 兼容；若有方言差异再拆专用实现）</li>
 *   <li><b>内嵌 H2 + {@code MODE=MySQL}</b>：{@link DatabaseHolder} 解析为 {@link DatabaseType#MYSQL}，走 {@code H2MysqlCompatSchemaSql}</li>
 * </ul>
 * <b>其余类型</b>：Oracle / SQL Server / SQLite / H2 原生 / HSQLDB / DB2 / Derby / Firebird / Informix / 达梦 /
 * OceanBase Oracle 等由 {@link cn.org.autumn.table.platform.jdbc.AbstractJdbcVendorRelationalTableOperations} 结合
 * {@link cn.org.autumn.table.relational.RelationalSchemaSqlCatalog} 生成 DDL；各库版本与类型映射差异较大，复杂演进建议配合 Flyway/Liquibase。
 */
public final class AnnotationTableSyncSupport {

    private AnnotationTableSyncSupport() {
    }

    /**
     * 是否允许 {@link cn.org.autumn.table.TableInit} 执行注解建表。
     */
    public static boolean supports(DatabaseType type) {
        return type != null && type != DatabaseType.OTHER;
    }
}
