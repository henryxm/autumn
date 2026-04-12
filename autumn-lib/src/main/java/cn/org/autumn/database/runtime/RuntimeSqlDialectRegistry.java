package cn.org.autumn.database.runtime;

import cn.org.autumn.config.Config;
import cn.org.autumn.database.DatabaseHolder;
import cn.org.autumn.database.DatabaseType;
import cn.org.autumn.database.H2EmbeddedMysqlDialect;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;

/**
 * 供 MyBatis {@code SqlProvider} 等非 Spring 托管类在启动后取得当前方言（由 {@link RuntimeSqlDialectBootstrap} 或
 * {@link RoutingRuntimeSqlDialect} 注入 {@link RoutingRuntimeSqlDialect}）。
 * <p>
 * 运行期注入后，{@link RoutingRuntimeSqlDialect} 按 {@link DatabaseHolder#readCurrentRoutingJdbcUrl} 随动态数据源线程键切换，
 * 避免主库方言套在从库连接上。启动早期无 Spring Bean 时，{@link #resolveStatelessFromEnvironment()} 仅按<strong>主库</strong> URL
 * 回退（与 {@link DatabaseHolder#readPrimaryJdbcUrl} 一致），因彼时通常尚无路由上下文。
 * <p>
 * MyBatis 在构建 {@code SqlSessionFactory} 时可能早于 {@link RoutingRuntimeSqlDialect} 的初始化解析并缓存 Provider SQL；
 * 此时若仍使用默认 MySQL 方言会生成反引号等错误语法。
 */
public final class RuntimeSqlDialectRegistry {

    private static final RuntimeSqlDialect MYSQL_STATELESS = new MysqlRuntimeSqlDialect();
    private static final RuntimeSqlDialect POSTGRESQL_STATELESS = new PostgresqlRuntimeSqlDialect();
    private static final RuntimeSqlDialect ORACLE_STATELESS = new OracleRuntimeSqlDialect();
    private static final RuntimeSqlDialect SQLSERVER_STATELESS = new SqlServerRuntimeSqlDialect();
    private static final RuntimeSqlDialect SQLITE_STATELESS = new SqliteRuntimeSqlDialect();
    private static final RuntimeSqlDialect H2_STATELESS = new H2RuntimeSqlDialect();
    private static final RuntimeSqlDialect DB2_DERBY_STATELESS = new Db2DerbyRuntimeSqlDialect();
    private static final RuntimeSqlDialect FIREBIRD_STATELESS = new FirebirdRuntimeSqlDialect();
    private static final RuntimeSqlDialect INFORMIX_STATELESS = new InformixRuntimeSqlDialect();
    private static final RuntimeSqlDialect KINGBASE_STATELESS = new KingbaseRuntimeSqlDialect();
    private static final RuntimeSqlDialect TIDB_STATELESS = new TidbRuntimeSqlDialect();
    private static final RuntimeSqlDialect OCEANBASE_MYSQL_STATELESS = new OceanBaseMysqlRuntimeSqlDialect();
    private static final RuntimeSqlDialect OCEANBASE_ORACLE_STATELESS = new OceanBaseOracleRuntimeSqlDialect();

    private static volatile RuntimeSqlDialect dialect = MYSQL_STATELESS;

    private RuntimeSqlDialectRegistry() {
    }

    public static RuntimeSqlDialect get() {
        RuntimeSqlDialect d = dialect;
        if (d instanceof RoutingRuntimeSqlDialect) {
            return d;
        }
        try {
            Object r = Config.getBean(RoutingRuntimeSqlDialect.class);
            if (r instanceof RoutingRuntimeSqlDialect) {
                return (RuntimeSqlDialect) r;
            }
        } catch (Exception ignored) {
        }
        RuntimeSqlDialect fromEnv = resolveStatelessFromEnvironment();
        if (fromEnv != null) {
            return fromEnv;
        }
        return d;
    }

    /**
     * 与 {@link DatabaseHolder#getType()} 对齐。
     */
    private static RuntimeSqlDialect resolveStatelessFromEnvironment() {
        try {
            Environment env = Config.getInstance().getEnvironment();
            if (env == null) {
                return null;
            }
            String url = DatabaseHolder.readPrimaryJdbcUrl(env);
            if (H2EmbeddedMysqlDialect.isActive(url)) {
                return H2_STATELESS;
            }
            DatabaseType t = DatabaseHolder.resolveType(url, env.getProperty("autumn.database"));
            return statelessForType(t);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static RuntimeSqlDialect statelessForType(DatabaseType t) {
        if (t == null) {
            return null;
        }
        switch (t) {
            case POSTGRESQL:
                return POSTGRESQL_STATELESS;
            case KINGBASE:
                return KINGBASE_STATELESS;
            case ORACLE:
                return ORACLE_STATELESS;
            case OCEANBASE_ORACLE:
                return OCEANBASE_ORACLE_STATELESS;
            case SQLSERVER:
                return SQLSERVER_STATELESS;
            case DAMENG:
                return ORACLE_STATELESS;
            case TIDB:
                return TIDB_STATELESS;
            case OCEANBASE_MYSQL:
                return OCEANBASE_MYSQL_STATELESS;
            case SQLITE:
                return SQLITE_STATELESS;
            case H2:
            case HSQLDB:
                return H2_STATELESS;
            case DB2:
            case DERBY:
                return DB2_DERBY_STATELESS;
            case FIREBIRD:
                return FIREBIRD_STATELESS;
            case INFORMIX:
                return INFORMIX_STATELESS;
            case MYSQL:
            case MARIADB:
            case OTHER:
            default:
                return MYSQL_STATELESS;
        }
    }

    public static void set(RuntimeSqlDialect d) {
        if (d != null) {
            dialect = d;
        }
    }
}
