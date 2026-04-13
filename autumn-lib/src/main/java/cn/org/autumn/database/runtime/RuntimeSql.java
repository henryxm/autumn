package cn.org.autumn.database.runtime;

/**
 * 手写 SQL / MyBatis {@code *Provider} 的基类：统一从 {@link RuntimeSqlDialectRegistry} 取当前方言，
 * 子类通过 {@link #quote(String)}、{@link #limitOne()}、{@link #likeContainsAny(String)}、{@link #booleanColumnAsTinyInt01(String)}、
 * {@link #limitOffsetSuffix(long, long)}、{@link #lowerColumnContainsNeedle(String, String)}、时间桶方法等拼装可移植 SQL，
 * 避免在每个 Provider 中重复 {@code RuntimeSqlDialectRegistry.get()} 与 {@code d()} 样板代码。
 * <p>
 * 多库兼容纪律、Wrapper 边界与推荐分层见仓库内 {@code docs/AI_DATABASE.md}（相对仓库根）；PostgreSQL 专项见 {@code docs/AI_POSTGRESQL.md}。
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
     *
     * @see WrapperColumns 仅继承 {@link com.baomidou.mybatisplus.extension.service.impl.ServiceImpl} 时用静态入口
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

    /**
     * 与 {@link #enabledTrueSqlLiteral()} 对偶：关/假字面量（PG boolean 等用 {@code FALSE}）。
     */
    public String enabledFalseSqlLiteral() {
        return dialect().enabledFalseSqlLiteral();
    }

    /**
     * 将可空布尔（或 0/1 开关）列规范为聚合用 0/1；{@code quotedColumn} 须已由 {@link #quote(String)} 处理。
     *
     * @see RuntimeSqlDialect#sqlBooleanColumnAsTinyInt01(String)
     */
    public String booleanColumnAsTinyInt01(String quotedColumn) {
        return dialect().sqlBooleanColumnAsTinyInt01(quotedColumn);
    }

    /**
     * 手写分页 SQL 末尾后缀（不含 {@code ORDER BY}）；SQL Server 等须在拼接前保证主查询已有 {@code ORDER BY}。
     *
     * @see RuntimeSqlDialect#sqlLimitOffsetSuffix(long, long)
     */
    public String limitOffsetSuffix(long limit, long offset) {
        return dialect().sqlLimitOffsetSuffix(limit, offset);
    }

    /**
     * 大小写不敏感子串条件；{@code mybatisNeedleParam} 如 {@code #{q}}，绑定值建议小写。
     *
     * @see RuntimeSqlDialect#sqlLowerColumnContainsNeedle(String, String)
     */
    public String lowerColumnContainsNeedle(String quotedColumn, String mybatisNeedleParam) {
        return dialect().sqlLowerColumnContainsNeedle(quotedColumn, mybatisNeedleParam);
    }

    /**
     * 日桶键 {@code yyyy-MM-dd}，{@code quotedColumn} 须已由 {@link #quote(String)} 处理。
     *
     * @see RuntimeSqlDialect#sqlTimestampBucketDay(String)
     */
    public String timestampBucketDay(String quotedColumn) {
        return dialect().sqlTimestampBucketDay(quotedColumn);
    }

    /**
     * 月桶键 {@code yyyy-MM}。
     */
    public String timestampBucketMonth(String quotedColumn) {
        return dialect().sqlTimestampBucketMonth(quotedColumn);
    }

    /**
     * 年桶键 {@code yyyy}。
     */
    public String timestampBucketYear(String quotedColumn) {
        return dialect().sqlTimestampBucketYear(quotedColumn);
    }

    /**
     * ISO 周桶键 {@code yyyy-Www}。
     */
    public String timestampBucketIsoWeek(String quotedColumn) {
        return dialect().sqlTimestampBucketIsoWeek(quotedColumn);
    }
}
