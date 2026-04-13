package cn.org.autumn.database.runtime;

import cn.org.autumn.database.DatabaseHolder;
import cn.org.autumn.database.DatabaseType;
import cn.org.autumn.database.H2EmbeddedMysqlDialect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 根据<strong>当前路由数据源</strong>的 JDBC URL（{@link DatabaseHolder#readCurrentRoutingJdbcUrl(Environment)}）与
 * {@link DatabaseHolder#resolveType(String, String)} 选择具体方言；无 {@link Environment} 时退化为 {@link DatabaseHolder#getType()}。
 * <p>
 * 与 {@link DatabaseHolder#getType()} 一致：在配置了 {@link cn.org.autumn.datasources.DataSourceDialectRegistry} 时，
 * 随 {@link cn.org.autumn.datasources.DynamicDataSource} 当前线程 lookup key 切换具体方言实现。
 * <p>
 * <b>内嵌 H2 + {@code MODE=MySQL}</b>：对该 URL 必须选用 {@link H2RuntimeSqlDialect} 的双引号与 H2 函数形态，勿用
 * {@link MysqlRuntimeSqlDialect} 的反引号 / {@code FIND_IN_SET}。
 * <p>
 * {@link DatabaseType#MYSQL}、{@link DatabaseType#MARIADB}、{@link DatabaseType#OTHER} 及未单独接线的枚举值在按类型路由时回退
 * {@link MysqlRuntimeSqlDialect}；{@link DatabaseType#OTHER} 生产环境应尽量避免，须显式配置 {@code autumn.database}。
 */
@Primary
@Component
public class RoutingRuntimeSqlDialect implements RuntimeSqlDialect {

    @Autowired
    private DatabaseHolder databaseHolder;

    @Autowired(required = false)
    private Environment environment;

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
        Environment env = environment != null ? environment : databaseHolder.getEnvironment();
        DatabaseType t;
        if (env != null) {
            String url = DatabaseHolder.readCurrentRoutingJdbcUrl(env);
            if (H2EmbeddedMysqlDialect.isActive(url)) {
                return h2RuntimeSqlDialect;
            }
            t = DatabaseHolder.resolveType(url, env.getProperty("autumn.database", ""));
        } else {
            t = databaseHolder.getType();
        }
        return dialectForType(t);
    }

    private RuntimeSqlDialect dialectForType(DatabaseType t) {
        if (t == null) {
            return mysqlRuntimeSqlDialect;
        }
        switch (t) {
            case POSTGRESQL:
                return postgresqlRuntimeSqlDialect;
            case KINGBASE:
                return kingbaseRuntimeSqlDialect;
            case ORACLE:
                return oracleRuntimeSqlDialect;
            case OCEANBASE_ORACLE:
                return oceanBaseOracleRuntimeSqlDialect;
            case SQLSERVER:
                return sqlServerRuntimeSqlDialect;
            case DAMENG:
                return oracleRuntimeSqlDialect;
            case TIDB:
                return tidbRuntimeSqlDialect;
            case OCEANBASE_MYSQL:
                return oceanBaseMysqlRuntimeSqlDialect;
            case SQLITE:
                return sqliteRuntimeSqlDialect;
            case H2:
            case HSQLDB:
                return h2RuntimeSqlDialect;
            case DB2:
            case DERBY:
                return db2DerbyRuntimeSqlDialect;
            case FIREBIRD:
                return firebirdRuntimeSqlDialect;
            case INFORMIX:
                return informixRuntimeSqlDialect;
            case MYSQL:
            case MARIADB:
            case OTHER:
                return mysqlRuntimeSqlDialect;
        }
        // Java 8 switch 对枚举不做穷尽校验；新增 DatabaseType 时须在上文接线
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

    @Override
    public String enabledFalseSqlLiteral() {
        return delegate().enabledFalseSqlLiteral();
    }

    @Override
    public String sqlBooleanColumnAsTinyInt01(String quotedColumn) {
        return delegate().sqlBooleanColumnAsTinyInt01(quotedColumn);
    }

    @Override
    public String sqlLimitOffsetSuffix(long limit, long offset) {
        return delegate().sqlLimitOffsetSuffix(limit, offset);
    }

    @Override
    public String sqlLowerColumnContainsNeedle(String quotedColumn, String mybatisNeedleParam) {
        return delegate().sqlLowerColumnContainsNeedle(quotedColumn, mybatisNeedleParam);
    }

    @Override
    public String sqlTimestampBucketDay(String quotedColumn) {
        return delegate().sqlTimestampBucketDay(quotedColumn);
    }

    @Override
    public String sqlTimestampBucketMonth(String quotedColumn) {
        return delegate().sqlTimestampBucketMonth(quotedColumn);
    }

    @Override
    public String sqlTimestampBucketYear(String quotedColumn) {
        return delegate().sqlTimestampBucketYear(quotedColumn);
    }

    @Override
    public String sqlTimestampBucketIsoWeek(String quotedColumn) {
        return delegate().sqlTimestampBucketIsoWeek(quotedColumn);
    }
}
