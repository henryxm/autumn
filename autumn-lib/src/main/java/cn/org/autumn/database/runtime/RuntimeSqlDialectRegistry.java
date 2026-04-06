package cn.org.autumn.database.runtime;

import cn.org.autumn.config.Config;
import cn.org.autumn.database.DatabaseHolder;
import cn.org.autumn.database.DatabaseType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;

/**
 * 供 MyBatis {@code SqlProvider} 等非 Spring 托管类在启动后取得当前方言（由 {@link RuntimeSqlDialectBootstrap} 或
 * {@link RoutingRuntimeSqlDialect} 注入 {@link RoutingRuntimeSqlDialect}）。
 * <p>
 * MyBatis 在构建 {@code SqlSessionFactory} 时可能早于 {@link RoutingRuntimeSqlDialect} 的初始化解析并缓存 Provider SQL；
 * 此时若仍使用默认 MySQL 方言会生成反引号等错误语法。故在尚未注入 {@link RoutingRuntimeSqlDialect} 时，根据
 * {@link Config#getEnvironment()} 中的 JDBC URL / {@code autumn.database} 回退到无状态的具体方言实例（与
 * {@link DatabaseHolder#getType()} 解析顺序一致）。
 */
public final class RuntimeSqlDialectRegistry {

    private static final RuntimeSqlDialect MYSQL_STATELESS = new MysqlRuntimeSqlDialect();
    private static final RuntimeSqlDialect POSTGRESQL_STATELESS = new PostgresqlRuntimeSqlDialect();
    private static final RuntimeSqlDialect ORACLE_STATELESS = new OracleRuntimeSqlDialect();
    private static final RuntimeSqlDialect SQLSERVER_STATELESS = new SqlServerRuntimeSqlDialect();

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
     * 与 {@link DatabaseHolder#getType()} 对齐：先 JDBC URL，再 {@code autumn.database}。
     */
    private static RuntimeSqlDialect resolveStatelessFromEnvironment() {
        try {
            Environment env = Config.getInstance().getEnvironment();
            if (env == null) {
                return null;
            }
            String url = env.getProperty("spring.datasource.druid.first.url");
            if (StringUtils.isBlank(url)) {
                url = env.getProperty("spring.datasource.url");
            }
            DatabaseType fromUrl = DatabaseHolder.inferFromJdbcUrl(url);
            DatabaseType t = fromUrl != null ? fromUrl : DatabaseType.fromConfig(env.getProperty("autumn.database"));
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
            case ORACLE:
                return ORACLE_STATELESS;
            case SQLSERVER:
                return SQLSERVER_STATELESS;
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
