package cn.org.autumn.table.relational.provider;

import cn.org.autumn.config.Config;
import cn.org.autumn.database.DatabaseHolder;
import cn.org.autumn.database.DatabaseType;
import cn.org.autumn.database.H2EmbeddedMysqlDialect;
import cn.org.autumn.datasources.DataSourceDialectRegistry;
import cn.org.autumn.datasources.DynamicDataSource;
import org.apache.commons.lang.StringUtils;
import org.springframework.core.env.Environment;

/**
 * 内嵌 H2 是否按 MySQL 兼容使用：根据<b>当前线程路由到的数据源</b>对应 JDBC URL 是否含 {@code MODE=MySQL} 判断
 * （与 {@link H2EmbeddedMysqlDialect}、{@link DatabaseHolder#getRoutedJdbcUrl()} / {@link DatabaseHolder#getType()} 语义一致）。
 * <p>
 * 优先使用 Spring 中的 {@link DatabaseHolder#getRoutedJdbcUrl()}，避免 {@code Config#getBean} 取 Registry 失败时
 * 仅回退 first URL 与 {@link DatabaseHolder} 已注入的 Registry 路由不一致。
 * 无 {@link DatabaseHolder} 且无 {@link DataSourceDialectRegistry} 时回退为仅读
 * {@code spring.datasource.druid.first.url} / {@code spring.datasource.url}。
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
            String url = resolveJdbcUrlLegacy(e);
            return H2EmbeddedMysqlDialect.isActive(url);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String resolveJdbcUrlLegacy(Environment e) {
        try {
            Object regBean = Config.getBean(DataSourceDialectRegistry.class);
            if (regBean instanceof DataSourceDialectRegistry) {
                String key = DynamicDataSource.getDataSource();
                return ((DataSourceDialectRegistry) regBean).resolveJdbcUrlForLookupKey(key);
            }
        } catch (Exception ignored) {
        }
        String url = e.getProperty("spring.datasource.druid.first.url", "");
        if (StringUtils.isBlank(url)) {
            url = e.getProperty("spring.datasource.url", "");
        }
        return url == null ? "" : url;
    }
}
