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
     * 清空整表：{@code TRUNCATE TABLE} + {@link #quote(String)}；MySQL / PostgreSQL / Oracle / SQL Server 均支持该形态。
     * <p>
     * 慎用：与事务、外键、复制等组合时各库语义不同；调用侧通常映射为 {@code @UpdateProvider}。
     */
    default String truncateTable(String tableName) {
        return "TRUNCATE TABLE " + quote(tableName);
    }

    /**
     * 在「可能返回多行」的 {@code SELECT} 末尾追加单行限制（MySQL 系为 {@code LIMIT 1}；Oracle 12c+ 为 {@code FETCH FIRST}；SQL Server 为 {@code OFFSET/FETCH}）。
     * <p>
     * <b>不要</b>用于 {@code SELECT COUNT(*)}、{@code SELECT MAX(...)} 等聚合：结果已最多一行，再追加会在部分方言上产生多余子句（如 SQL Server 的 {@code ORDER BY (SELECT NULL)}）。
     */
    default String limitOne() {
        return " LIMIT 1";
    }

    /**
     * 生成 {@code LIKE} 右侧的「两端通配」模式表达式，与 MyBatis 占位符拼接。
     * <p>
     * 勿手写 {@code concat('%', #{x}, '%')}：Oracle {@code CONCAT} 仅双参；应统一用本方法，由各方言实现。
     *
     * @param mybatisParamPlaceholder 形如 {@code #{username}} 的占位符串（含括号）
     */
    default String likeContainsAny(String mybatisParamPlaceholder) {
        return "concat('%', " + mybatisParamPlaceholder + ", '%')";
    }

    /**
     * 判断「列当前值」是否出现在「逗号分隔字面量列表」中（语义对齐 MySQL {@code FIND_IN_SET}，用于数据权限等拼接 SQL）。
     *
     * @param qualifiedColumn 已带表别名前缀的列，如 {@code t.dept_key}
     * @param csvInner        逗号分隔的若干 id，<b>不含</b>外层单引号
     */
    String columnValueInCommaSeparatedList(String qualifiedColumn, String csvInner);

    /**
     * 手写 SQL 中与「开关」列比较时使用的字面量（无 {@code #{}} 占位符场景）。
     * <p>
     * PostgreSQL 原生 {@code boolean} 列需 {@code TRUE}，不能与整型 {@code 1} 比较；
     * MySQL / MariaDB 等常用 {@code tinyint(1)/smallint} 存 0/1，使用 {@code 1}。
     */
    default String enabledTrueSqlLiteral() {
        return "1";
    }
}
