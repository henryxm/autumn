package cn.org.autumn.database.runtime;

import cn.org.autumn.database.AutumnDatabaseHolder;
import cn.org.autumn.database.AutumnDatabaseType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 根据 {@code autumn.database} 选择具体方言；PostgreSQL / Oracle / SQL Server 使用独立实现；MySQL、MariaDB 及其它回退到 MySQL 规则。
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

    @Autowired
    private OracleRuntimeSqlDialect oracleRuntimeSqlDialect;

    @Autowired
    private SqlServerRuntimeSqlDialect sqlServerRuntimeSqlDialect;

    private RuntimeSqlDialect delegate() {
        AutumnDatabaseType t = autumnDatabaseHolder.getType();
        if (t == AutumnDatabaseType.POSTGRESQL) {
            return postgresqlRuntimeSqlDialect;
        }
        if (t == AutumnDatabaseType.ORACLE) {
            return oracleRuntimeSqlDialect;
        }
        if (t == AutumnDatabaseType.SQLSERVER) {
            return sqlServerRuntimeSqlDialect;
        }
        // MYSQL、MARIADB、OTHER 等与 MySQL 规则一致（MariaDB 与 MySQL 共用 FIND_IN_SET / 反引号）
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
    public String limitOne() {
        return delegate().limitOne();
    }

    @Override
    public String currentTimestamp() {
        return delegate().currentTimestamp();
    }

    @Override
    public String likeContainsAny(String mybatisParamPlaceholder) {
        return delegate().likeContainsAny(mybatisParamPlaceholder);
    }

    @Override
    public String columnValueInCommaSeparatedList(String qualifiedColumn, String csvInner) {
        return delegate().columnValueInCommaSeparatedList(qualifiedColumn, csvInner);
    }
}
