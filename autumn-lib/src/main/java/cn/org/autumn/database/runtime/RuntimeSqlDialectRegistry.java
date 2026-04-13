package cn.org.autumn.database.runtime;

import cn.org.autumn.config.Config;
import cn.org.autumn.database.DatabaseHolder;
import cn.org.autumn.database.DatabaseType;
import cn.org.autumn.install.InstallMode;
import org.apache.commons.lang.StringUtils;
import org.springframework.core.env.Environment;

/**
 * 供 MyBatis {@code SqlProvider} 等非 Spring 托管类在启动后取得当前方言（由 {@link RuntimeSqlDialectBootstrap} 或
 * {@link RoutingRuntimeSqlDialect} 注入 {@link RoutingRuntimeSqlDialect}）。
 * <p>
 * MyBatis 在构建 {@code SqlSessionFactory} 时可能早于 {@link RoutingRuntimeSqlDialect} 的初始化解析并缓存 Provider SQL；
 * 此时若仍使用默认 MySQL 方言会生成反引号等错误语法。故在尚未注入 {@link RoutingRuntimeSqlDialect} 时，根据
 * {@link Config#getEnvironment()} 中的 JDBC URL / {@code autumn.database} 回退到无状态的具体方言实例（与
 * {@link DatabaseHolder#getType()} 解析顺序一致）。
 * <p>
 * {@link #resolveStatelessFromEnvironment()} 仅使用<b>首数据源</b> URL，与 {@link cn.org.autumn.datasources.DynamicDataSource} 线程 key 无关；
 * 与 {@link DatabaseHolder} 在线程已绑定路由键时的行为并存：启动期 Provider 解析仍按首源，运行期经 Spring Bean 的
 * {@link RoutingRuntimeSqlDialect} 则随线程变化。
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
            if (InstallMode.isActive(env)) {
                return MYSQL_STATELESS;
            }
            String url = env.getProperty("spring.datasource.druid.first.url");
            if (StringUtils.isBlank(url)) {
                url = env.getProperty("spring.datasource.url");
            }
            DatabaseType t = DatabaseHolder.resolveType(url, env.getProperty("autumn.database"));
            return statelessForType(t);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 与 {@link DatabaseType} 对应的<b>无状态</b> {@link RuntimeSqlDialect}，不依赖 {@link DatabaseHolder} 线程路由。
     * 供逻辑库类型与物理 JDBC 不一致时的导出/工具（例如 H2 {@code MODE=MySQL}）使用。
     */
    public static RuntimeSqlDialect statelessDialectFor(DatabaseType t) {
        if (t == null) {
            return MYSQL_STATELESS;
        }
        RuntimeSqlDialect d = statelessForType(t);
        return d != null ? d : MYSQL_STATELESS;
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

    /**
     * 进程内 Spring 重启后丢弃可能仍指向旧 {@link RoutingRuntimeSqlDialect} 的静态引用，由新一轮启动再次 {@link #set}。
     */
    public static void resetForJvmRestart() {
        dialect = MYSQL_STATELESS;
    }
}
