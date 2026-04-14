package cn.org.autumn.table.relational.provider;

import cn.org.autumn.config.Config;
import cn.org.autumn.database.H2EmbeddedMysqlDialect;
import cn.org.autumn.datasources.DataSourceDialectRegistry;
import cn.org.autumn.datasources.DynamicDataSource;
import org.apache.commons.lang.StringUtils;
import org.springframework.core.env.Environment;

/**
 * 内嵌 H2 是否按 MySQL 兼容使用：根据<b>当前线程路由到的数据源</b>对应 JDBC URL 是否含 {@code MODE=MySQL} 判断
 * （与 {@link H2EmbeddedMysqlDialect}、{@link cn.org.autumn.database.DatabaseHolder} 的 first/second 语义一致）。
 * <p>
 * 无 {@link DataSourceDialectRegistry} 时回退为仅读 {@code spring.datasource.druid.first.url} / {@code spring.datasource.url}。
 */
public final class EmbeddedH2MysqlMode {

    public static boolean active() {
        try {
            Environment e = Config.getInstance().getEnvironment();
            if (e == null) {
                return false;
            }
            String url = resolveJdbcUrlAlignedWithDatabaseHolder(e);
            return H2EmbeddedMysqlDialect.isActive(url);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String resolveJdbcUrlAlignedWithDatabaseHolder(Environment e) {
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
