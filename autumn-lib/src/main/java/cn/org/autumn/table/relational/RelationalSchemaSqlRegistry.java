package cn.org.autumn.table.relational;

import cn.org.autumn.config.Config;
import cn.org.autumn.database.DatabaseHolder;
import cn.org.autumn.database.DatabaseType;
import cn.org.autumn.table.relational.dialect.mysql.H2MysqlCompatSchemaSql;
import cn.org.autumn.table.relational.dialect.mysql.MysqlSchemaSql;
import cn.org.autumn.table.relational.provider.EmbeddedH2MysqlMode;
import org.springframework.core.env.Environment;

/**
 * 供 {@link cn.org.autumn.table.relational.provider.QuerySql} 等 MyBatis {@code Provider} 取得当前 {@link RelationalSchemaSql}。
 * <p>
 * {@link #get()} 仅服务 {@code TableDao}（MySQL 协议 + 内嵌 H2-MySQL）；其它库请使用 {@link #forType(DatabaseType)} 或 Spring
 * {@link RoutingRelationalSchemaSql#forCurrentDatabase()}。
 */
public final class RelationalSchemaSqlRegistry {

    private static volatile RelationalSchemaSql fallback = MysqlSchemaSql.INSTANCE;

    public static RelationalSchemaSql get() {
        try {
            Object bean = Config.getBean(RoutingRelationalSchemaSql.class);
            if (bean instanceof RoutingRelationalSchemaSql) {
                return ((RoutingRelationalSchemaSql) bean).forTableDaoProvider();
            }
        } catch (Exception ignored) {
        }
        if (EmbeddedH2MysqlMode.active()) {
            return H2MysqlCompatSchemaSql.INSTANCE;
        }
        return fallback;
    }

    /**
     * 无 Spring 时按环境与 {@link DatabaseHolder#resolveType} 解析（与分页方言等一致）。
     */
    public static RelationalSchemaSql forType(DatabaseType type) {
        if (EmbeddedH2MysqlMode.active()) {
            return H2MysqlCompatSchemaSql.INSTANCE;
        }
        return RelationalSchemaSqlCatalog.forType(type);
    }

    /**
     * 仅从 Environment 推断 {@link DatabaseType}（无 {@code DatabaseHolder} 注入场景）。
     */
    public static RelationalSchemaSql forTypeFromEnvironment() {
        try {
            Environment env = Config.getInstance().getEnvironment();
            if (env == null) {
                return get();
            }
            String url = DatabaseHolder.readCurrentRoutingJdbcUrl(env);
            DatabaseType t = DatabaseHolder.resolveType(url, env.getProperty("autumn.database"));
            return forType(t);
        } catch (Exception ignored) {
            return get();
        }
    }

    public static void setFallback(RelationalSchemaSql sql) {
        if (sql != null) {
            fallback = sql;
        }
    }
}
