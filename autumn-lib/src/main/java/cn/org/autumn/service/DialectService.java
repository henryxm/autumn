package cn.org.autumn.service;

import cn.org.autumn.database.runtime.RuntimeSql;
import cn.org.autumn.database.runtime.RuntimeSqlDialect;
import cn.org.autumn.database.runtime.RuntimeSqlDialectRegistry;
import cn.org.autumn.database.runtime.WrapperColumns;
import cn.org.autumn.model.Parameterized;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;

/**
 * 服务层方言封装：与 {@link RuntimeSql} 同源能力，供 {@code EntityWrapper}、手写 SQL 条件等统一使用，
 * 避免在具体 Service 中重复 {@code RuntimeSqlDialectRegistry.get()} 与样板方法。
 * <p>
 * 已接入继承链：{@code BaseQueueService} → … → {@link cn.org.autumn.base.ModuleService}，
 * 业务实现类可直接调用 {@link #columnInWrapper(String)}、{@link #quote(String)}、{@link #sql()}、{@link #timestampBucketDay(String)}、
 * {@link #booleanColumnAsTinyInt01(String)}、{@link #limitOffsetSuffix(long, long)} 等；
 * {@link #columnInWrapper(String)} 与 {@link cn.org.autumn.database.runtime.WrapperColumns#columnInWrapper(String)} 同源。
 *
 * @see RuntimeSql
 * @see cn.org.autumn.database.runtime.RuntimeSqlDialectRegistry
 */
public abstract class DialectService<M extends BaseMapper<T>, T> extends ServiceImpl<M, T> implements Parameterized {

    private final RuntimeSql sql = new RuntimeSql();

    /**
     * 与 {@link RuntimeSql#dialect()} 一致，便于需要直接访问方言接口时使用。
     */
    protected RuntimeSqlDialect dialect() {
        return sql.dialect();
    }

    /**
     * 与 {@link RuntimeSql} 同源实例，可调用 {@link RuntimeSql#quote(String)}、{@link RuntimeSql#limitOne()} 等全部能力。
     */
    protected RuntimeSql sql() {
        return sql;
    }

    /**
     * MyBatis-Plus {@code EntityWrapper} / {@code orderBy} 等使用的列片段（已含方言引号，处理保留字列名）。
     */
    public String columnInWrapper(String name) {
        return WrapperColumns.columnInWrapper(name);
    }

    public String column(String name) {
        return RuntimeSqlDialectRegistry.get().columnInWrapper(name);
    }

    /**
     * 标识符引用（表名、列名等），与手写 SQL / Provider 侧一致。
     */
    public String quote(String identifier) {
        return sql.quote(identifier);
    }

    public String currentTimestamp() {
        return sql.currentTimestamp();
    }

    public String truncateTable(String tableName) {
        return sql.truncateTable(tableName);
    }

    public String limitOne() {
        return sql.limitOne();
    }

    public String likeContainsAny(String mybatisParamPlaceholder) {
        return sql.likeContainsAny(mybatisParamPlaceholder);
    }

    public String columnValueInCommaSeparatedList(String qualifiedColumn, String csvInner) {
        return sql.columnValueInCommaSeparatedList(qualifiedColumn, csvInner);
    }

    public String enabledTrueSqlLiteral() {
        return sql.enabledTrueSqlLiteral();
    }

    public String enabledFalseSqlLiteral() {
        return sql.enabledFalseSqlLiteral();
    }

    /** @see RuntimeSql#booleanColumnAsTinyInt01(String) */
    public String booleanColumnAsTinyInt01(String quotedColumn) {
        return sql.booleanColumnAsTinyInt01(quotedColumn);
    }

    /** @see RuntimeSql#limitOffsetSuffix(long, long) */
    public String limitOffsetSuffix(long limit, long offset) {
        return sql.limitOffsetSuffix(limit, offset);
    }

    /** @see RuntimeSql#lowerColumnContainsNeedle(String, String) */
    public String lowerColumnContainsNeedle(String quotedColumn, String mybatisNeedleParam) {
        return sql.lowerColumnContainsNeedle(quotedColumn, mybatisNeedleParam);
    }

    /**
     * 日桶键表达式 {@code yyyy-MM-dd}，与 {@link RuntimeSql#timestampBucketDay(String)} 同源；入参为已由 {@link #quote(String)} 或 {@link #column(String)} 得到的单列片段。
     * <p>
     * 可与 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper#apply(String, Object...)} 等组合，避免在 Wrapper 中手写 {@code DATE_FORMAT}/{@code to_char}。
     */
    public String timestampBucketDay(String quotedColumn) {
        return sql.timestampBucketDay(quotedColumn);
    }

    /** @see RuntimeSql#timestampBucketMonth(String) */
    public String timestampBucketMonth(String quotedColumn) {
        return sql.timestampBucketMonth(quotedColumn);
    }

    /** @see RuntimeSql#timestampBucketYear(String) */
    public String timestampBucketYear(String quotedColumn) {
        return sql.timestampBucketYear(quotedColumn);
    }

    /** @see RuntimeSql#timestampBucketIsoWeek(String) */
    public String timestampBucketIsoWeek(String quotedColumn) {
        return sql.timestampBucketIsoWeek(quotedColumn);
    }
}
