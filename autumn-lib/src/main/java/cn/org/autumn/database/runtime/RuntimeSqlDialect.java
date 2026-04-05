package cn.org.autumn.database.runtime;

/**
 * 业务层手写 SQL / MyBatis Provider 使用的运行时方言（标识符引用、时间函数等）。
 * <p>
 * 按库分实现类，置于同包下 {@link MysqlRuntimeSqlDialect}、{@link PostgresqlRuntimeSqlDialect}；
 * 其它数据库可新增实现并在 {@link RoutingRuntimeSqlDialect} 中接线。
 */
public interface RuntimeSqlDialect {

    /**
     * 引用标识符（列名、表名等），避免与保留字冲突。
     */
    String quote(String identifier);

    /**
     * 供 MyBatis-Plus {@code EntityWrapper} / {@code orderBy} 等传入的“列片段”（已含方言引号）。
     */
    default String columnInWrapper(String name) {
        return quote(name);
    }

    /**
     * 标准当前时间戳表达式（MySQL / PostgreSQL 均支持）。
     */
    default String currentTimestamp() {
        return "CURRENT_TIMESTAMP";
    }

    /**
     * 追加 {@code LIMIT 1}（PostgreSQL 同样支持）。
     */
    default String limitOne() {
        return " LIMIT 1";
    }

    /**
     * 判断「列当前值」是否出现在「逗号分隔字面量列表」中（语义对齐 MySQL {@code FIND_IN_SET}，用于数据权限等拼接 SQL）。
     *
     * @param qualifiedColumn 已带表别名前缀的列，如 {@code t.dept_key}
     * @param csvInner        逗号分隔的若干 id，<b>不含</b>外层单引号
     */
    String columnValueInCommaSeparatedList(String qualifiedColumn, String csvInner);
}
