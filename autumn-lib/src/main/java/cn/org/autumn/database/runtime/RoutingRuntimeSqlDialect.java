package cn.org.autumn.database.runtime;

import cn.org.autumn.database.AutumnDatabaseHolder;
import cn.org.autumn.database.AutumnDatabaseType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 根据 {@code autumn.database} 选择具体方言；未单独实现的数据库类型暂回退到 MySQL 规则（仅保证启动，语义可能需后续修正）。
 */
@Primary
@Component
public class RoutingRuntimeSqlDialect implements RuntimeSqlDialect {

    @Autowired
    private AutumnDatabaseHolder autumnDatabaseHolder;

    @Autowired
    private MysqlRuntimeSqlDialect mysqlRuntimeSqlDialect;

    @Autowired
    private PostgresqlRuntimeSqlDialect postgresqlRuntimeSqlDialect;

    private RuntimeSqlDialect delegate() {
        AutumnDatabaseType t = autumnDatabaseHolder.getType();
        if (t == AutumnDatabaseType.POSTGRESQL) {
            return postgresqlRuntimeSqlDialect;
        }
        return mysqlRuntimeSqlDialect;
    }

    @Override
    public String quote(String identifier) {
        return delegate().quote(identifier);
    }

    @Override
    public String columnInWrapper(String name) {
        return delegate().columnInWrapper(name);
    }

    @Override
    public String columnValueInCommaSeparatedList(String qualifiedColumn, String csvInner) {
        return delegate().columnValueInCommaSeparatedList(qualifiedColumn, csvInner);
    }
}
