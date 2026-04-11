package cn.org.autumn.table.relational.provider;

import cn.org.autumn.config.Config;
import cn.org.autumn.database.H2EmbeddedMysqlDialect;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;

/**
 * 内嵌 H2 是否按 MySQL 兼容使用：仅根据主数据源 JDBC URL 中的 {@code MODE=MySQL} 判断（与 {@link H2EmbeddedMysqlDialect} 一致）。
 */
public final class EmbeddedH2MysqlMode {

    public static boolean active() {
        try {
            Environment e = Config.getInstance().getEnvironment();
            if (e == null) {
                return false;
            }
            String url = e.getProperty("spring.datasource.druid.first.url", "");
            if (StringUtils.isBlank(url)) {
                url = e.getProperty("spring.datasource.url", "");
            }
            return H2EmbeddedMysqlDialect.isActive(url);
        } catch (Exception ignored) {
            return false;
        }
    }
}
