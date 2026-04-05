/**
 * 运行时 SQL 方言：与注解建表包 {@code cn.org.autumn.table.platform} 分离，专供业务 Mapper / Wrapper 使用。
 * <p>
 * 扩展其它库：新增 {@link RuntimeSqlDialect} 实现类，并在 {@link RoutingRuntimeSqlDialect} 中按 {@link cn.org.autumn.database.AutumnDatabaseType} 分支接线。
 * <p>
 * {@link RuntimeSqlDialect#columnValueInCommaSeparatedList(String, String)} 用于替代 MySQL 专有 {@code FIND_IN_SET}（如 {@link cn.org.autumn.modules.sys.aspect.DataFilterAspect}）。
 */
package cn.org.autumn.database.runtime;
