package cn.org.autumn.database.runtime;

import cn.org.autumn.database.DatabaseHolder;
import cn.org.autumn.database.DatabaseType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 根据 {@link DatabaseHolder#getType()} 选择具体 {@link RuntimeSqlDialect} 实现。
 * <p>
 * 与 {@link DatabaseHolder} 一致：反映的是<b>首数据源</b>方言，非多数据源路由下的「当前连接」方言。
 * 异构 second 源上的 SQL 仍可能按 first 方言生成（限 Provider/工具层调用路径）；线程级方言属增强需求，见
 * {@link DatabaseHolder} 类注释。
 */
@Primary
@Component
public class RoutingRuntimeSqlDialect implements RuntimeSqlDialect {

    @Autowired
    private DatabaseHolder databaseHolder;

    @Autowired
    private MysqlRuntimeSqlDialect mysqlRuntimeSqlDialect;

    @Autowired
    private PostgresqlRuntimeSqlDialect postgresqlRuntimeSqlDialect;

    @Autowired
    private OracleRuntimeSqlDialect oracleRuntimeSqlDialect;

    @Autowired
    private SqlServerRuntimeSqlDialect sqlServerRuntimeSqlDialect;

    @Autowired
    private SqliteRuntimeSqlDialect sqliteRuntimeSqlDialect;

    @Autowired
    private H2RuntimeSqlDialect h2RuntimeSqlDialect;

    @Autowired
    private Db2DerbyRuntimeSqlDialect db2DerbyRuntimeSqlDialect;

    @Autowired
    private FirebirdRuntimeSqlDialect firebirdRuntimeSqlDialect;

    @Autowired
    private InformixRuntimeSqlDialect informixRuntimeSqlDialect;

    @Autowired
    private KingbaseRuntimeSqlDialect kingbaseRuntimeSqlDialect;

    @Autowired
    private TidbRuntimeSqlDialect tidbRuntimeSqlDialect;

    @Autowired
    private OceanBaseMysqlRuntimeSqlDialect oceanBaseMysqlRuntimeSqlDialect;

    @Autowired
    private OceanBaseOracleRuntimeSqlDialect oceanBaseOracleRuntimeSqlDialect;

    private RuntimeSqlDialect delegate() {
        DatabaseType t = databaseHolder.getType();
        if (t == DatabaseType.POSTGRESQL) {
            return postgresqlRuntimeSqlDialect;
        }
        if (t == DatabaseType.KINGBASE) {
            return kingbaseRuntimeSqlDialect;
        }
        if (t == DatabaseType.ORACLE) {
            return oracleRuntimeSqlDialect;
        }
        if (t == DatabaseType.OCEANBASE_ORACLE) {
            return oceanBaseOracleRuntimeSqlDialect;
        }
        if (t == DatabaseType.SQLSERVER) {
            return sqlServerRuntimeSqlDialect;
        }
        if (t == DatabaseType.DAMENG) {
            return oracleRuntimeSqlDialect;
        }
        if (t == DatabaseType.TIDB) {
            return tidbRuntimeSqlDialect;
        }
        if (t == DatabaseType.OCEANBASE_MYSQL) {
            return oceanBaseMysqlRuntimeSqlDialect;
        }
        if (t == DatabaseType.SQLITE) {
            return sqliteRuntimeSqlDialect;
        }
        if (t == DatabaseType.H2 || t == DatabaseType.HSQLDB) {
            return h2RuntimeSqlDialect;
        }
        if (t == DatabaseType.DB2 || t == DatabaseType.DERBY) {
            return db2DerbyRuntimeSqlDialect;
        }
        if (t == DatabaseType.FIREBIRD) {
            return firebirdRuntimeSqlDialect;
        }
        if (t == DatabaseType.INFORMIX) {
            return informixRuntimeSqlDialect;
        }
        // MYSQL、MARIADB、OTHER 等与 MySQL 规则一致
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
    public String truncateTable(String tableName) {
        return delegate().truncateTable(tableName);
    }

    @Override
    public String likeContainsAny(String mybatisParamPlaceholder) {
        return delegate().likeContainsAny(mybatisParamPlaceholder);
    }

    @Override
    public String columnValueInCommaSeparatedList(String qualifiedColumn, String csvInner) {
        return delegate().columnValueInCommaSeparatedList(qualifiedColumn, csvInner);
    }

    @Override
    public String enabledTrueSqlLiteral() {
        return delegate().enabledTrueSqlLiteral();
    }
}
