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

    /**
     * 与 {@link #enabledTrueSqlLiteral()} 对偶：关/假在 PG/DB2 布尔列上宜用 {@code FALSE}，MySQL 系常用 {@code 0}。
     */
    default String enabledFalseSqlLiteral() {
        return "0";
    }

    /**
     * 将可空布尔（或 0/1 数值开关）列规范为聚合用 0/1：{@code null → 0}，真/非零 → 1，假/零 → 0。
     * <p>
     * 适用于 {@code boolean}、{@code tinyint(1)}、{@code NUMBER(1)} 等；对任意字符串列的语义未定义。
     */
    default String sqlBooleanColumnAsTinyInt01(String quotedColumn) {
        String c = quotedColumn;
        return "(CASE WHEN " + c + " IS NULL THEN 0 WHEN " + c + " THEN 1 ELSE 0 END)";
    }

    /**
     * 非 PageHelper 的手写分页后缀（<b>不含</b> {@code ORDER BY}）。
     * <p>
     * <b>SQL Server</b>：返回 {@code OFFSET … FETCH …} 时，<b>必须</b>保证主查询在拼接本后缀前已含 {@code ORDER BY}，否则语法非法。
     * <p>
     * <b>Firebird</b>：使用 {@code ROWS m TO n}（闭区间）。
     * <p>
     * <b>Informix</b>：使用 {@code SKIP offset FIRST limit}。
     */
    String sqlLimitOffsetSuffix(long limit, long offset);

    /**
     * 大小写不敏感子串匹配（{@code needle} 绑定值建议已由调用方转为小写）。
     * <p>
     * 返回可作为 {@code WHERE} 条件的布尔表达式片段（不含 {@code AND}）；{@code mybatisNeedleParam} 形如 {@code #{q}}。
     */
    String sqlLowerColumnContainsNeedle(String quotedColumn, String mybatisNeedleParam);

    /**
     * 将时间戳/日期列格式化为「日」桶展示键 {@code yyyy-MM-dd}，可直接用于 {@code SELECT} / {@code GROUP BY} / {@code ORDER BY}。
     * <p>
     * 调用方传入的列须已由 {@link #quote(String)} 包裹（或等价的安全标识符片段）。
     *
     * @param quotedColumn 已引用的单列，如 {@code "create"}
     */
    String sqlTimestampBucketDay(String quotedColumn);

    /**
     * 「月」桶键 {@code yyyy-MM}。
     */
    String sqlTimestampBucketMonth(String quotedColumn);

    /**
     * 「年」桶键 {@code yyyy}（四位年）。
     */
    String sqlTimestampBucketYear(String quotedColumn);

    /**
     * ISO 8601 周展示键 {@code yyyy-Www}（周一为周首），同一表达式可用于 {@code SELECT}、{@code GROUP BY}、{@code ORDER BY}。
     * <p>
     * 年界附近的 ISO 周年与日历年在少数数据库上存在实现差异，业务若强依赖与 MySQL {@code YEARWEEK(...,3)} 完全一致，需在集成侧校验。
     */
    String sqlTimestampBucketIsoWeek(String quotedColumn);
}
