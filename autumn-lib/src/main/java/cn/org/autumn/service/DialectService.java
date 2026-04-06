package cn.org.autumn.service;

import cn.org.autumn.database.runtime.RuntimeSql;
import cn.org.autumn.database.runtime.RuntimeSqlDialect;
import cn.org.autumn.model.Parameterized;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;

/**
 * 服务层方言封装：与 {@link RuntimeSql} 同源能力，供 {@code EntityWrapper}、手写 SQL 条件等统一使用，
 * 避免在具体 Service 中重复 {@code RuntimeSqlDialectRegistry.get()} 与样板方法。
 * <p>
 * 已接入继承链：{@code BaseQueueService} → … → {@link cn.org.autumn.base.ModuleService}，
 * 业务实现类可直接调用 {@link #columnInWrapper(String)}、{@link #quote(String)}、{@link #sql()} 等。
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
        return sql.columnInWrapper(name);
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
}
