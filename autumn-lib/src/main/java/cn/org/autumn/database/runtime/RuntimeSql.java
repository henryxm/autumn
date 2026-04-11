package cn.org.autumn.database.runtime;

/**
 * 手写 SQL / MyBatis {@code *Provider} 的基类：统一从 {@link RuntimeSqlDialectRegistry} 取当前方言，
 * 子类通过 {@link #quote(String)}、{@link #limitOne()}、{@link #likeContainsAny(String)} 等拼装可移植 SQL，
 * 避免在每个 Provider 中重复 {@code RuntimeSqlDialectRegistry.get()} 与 {@code d()} 样板代码。
 * <p>
 * 多库兼容纪律、Wrapper 边界与推荐分层见仓库根目录 {@code AI_DATABASE.md}；PostgreSQL 专项见 {@code AI_POSTGRESQL.md}。
 *
 * @see RuntimeSqlDialect
 */
public class RuntimeSql {

    /**
     * 当前线程/上下文下的运行时方言（由注册表解析，通常为 Spring 管理的 {@link RoutingRuntimeSqlDialect} 等）。
     */
    public RuntimeSqlDialect dialect() {
        return RuntimeSqlDialectRegistry.get();
    }

    /**
     * 引用标识符（列名、表名等）。
     */
    public String quote(String identifier) {
        return dialect().quote(identifier);
    }

    /**
     * 供 Wrapper 等传入的列片段（已含方言引号）。
     */
    public String columnInWrapper(String name) {
        return dialect().columnInWrapper(name);
    }

    /**
     * 标准当前时间戳表达式。
     */
    public String currentTimestamp() {
        return dialect().currentTimestamp();
    }

    /**
     * 清空整表（慎用）。
     */
    public String truncateTable(String tableName) {
        return dialect().truncateTable(tableName);
    }

    /**
     * 单行 {@code SELECT} 末尾限制。
     */
    public String limitOne() {
        return dialect().limitOne();
    }

    /**
     * {@code LIKE} 两端通配模式，参数为 MyBatis 占位符串（如 {@code #{name}}）。
     */
    public String likeContainsAny(String mybatisParamPlaceholder) {
        return dialect().likeContainsAny(mybatisParamPlaceholder);
    }

    /**
     * 语义对齐 MySQL {@code FIND_IN_SET} 的可移植片段。
     */
    public String columnValueInCommaSeparatedList(String qualifiedColumn, String csvInner) {
        return dialect().columnValueInCommaSeparatedList(qualifiedColumn, csvInner);
    }

    /**
     * 手写 SQL 中与「开关」列比较时的字面量（PG boolean 与整型库差异）。
     */
    public String enabledTrueSqlLiteral() {
        return dialect().enabledTrueSqlLiteral();
    }
}
