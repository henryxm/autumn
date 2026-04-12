package cn.org.autumn.table.relational.provider;

import cn.org.autumn.config.Config;
import cn.org.autumn.database.DatabaseHolder;
import cn.org.autumn.database.H2EmbeddedMysqlDialect;
import org.springframework.core.env.Environment;

/**
 * 内嵌 H2 是否按 MySQL 兼容使用：根据<strong>当前路由</strong>数据源 JDBC URL 中的 {@code MODE=MySQL} 判断（与
 * {@link H2EmbeddedMysqlDialect}、{@link DatabaseHolder#readCurrentRoutingJdbcUrl} 一致）。
 */
public final class EmbeddedH2MysqlMode {

    public static boolean active() {
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
}
