package cn.org.autumn.table.relational.provider;

import cn.org.autumn.config.Config;
import cn.org.autumn.database.DatabaseHolder;
import cn.org.autumn.database.DatabaseType;
import cn.org.autumn.database.H2EmbeddedMysqlDialect;
import org.springframework.core.env.Environment;

/**
 * 内嵌 H2 是否按 MySQL 兼容使用：根据<strong>当前线程路由到的数据源</strong> JDBC URL 中的 {@code MODE=MySQL} 判断
 * （与 {@link H2EmbeddedMysqlDialect}、{@link DatabaseHolder#getRoutedJdbcUrl()} / {@link DatabaseHolder#readCurrentRoutingJdbcUrl} 一致）。
 * <p>
 * 优先使用 Spring 中的 {@link DatabaseHolder#getRoutedJdbcUrl()}；无 Bean 时回退
 * {@link DatabaseHolder#readCurrentRoutingJdbcUrl(Environment)}，避免仅读 first URL 与路由不一致。
 */
public final class EmbeddedH2MysqlMode {

    public static boolean active() {
        try {
            Object holderBean = Config.getBean(DatabaseHolder.class);
            if (holderBean instanceof DatabaseHolder) {
                DatabaseHolder holder = (DatabaseHolder) holderBean;
                if (holder.getType() == DatabaseType.OTHER) {
                    return false;
                }
                return H2EmbeddedMysqlDialect.isActive(holder.getRoutedJdbcUrl());
            }
        } catch (Exception ignored) {
        }
        try {
            Environment e = Config.getInstance().getEnvironment();
            if (e == null) {
                return false;
            }
            return H2EmbeddedMysqlDialect.isActive(DatabaseHolder.readCurrentRoutingJdbcUrl(e));
        } catch (Exception ignored) {
            return false;
        }
    }

    private EmbeddedH2MysqlMode() {
    }
}
