package cn.org.autumn.database;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 持有当前生效的 {@link DatabaseType}，供路由数据源、分页方言、建表适配等使用。
 * <p>
 * <b>解析顺序</b>（与主数据源 JDBC URL 对齐，避免 autumn.database 与实际连接不一致）：
 * <ol>
 *   <li>若 {@code spring.datasource.druid.first.url}（或 {@code spring.datasource.url}）可识别，则以其为准；</li>
 *   <li>否则使用 {@code autumn.database} 配置（可为空，空则视为 mysql）。</li>
 * </ol>
 * 仍可通过 {@code autumn.database} 在无 URL 或 JDBC 非常见形态时手工指定。
 */
@Component
public class DatabaseHolder {

    @Value("${autumn.database:}")
    private String databaseRaw;

    @Autowired(required = false)
    private Environment environment;

    public DatabaseType getType() {
        if (environment != null) {
            String url = environment.getProperty("spring.datasource.druid.first.url");
            if (StringUtils.isBlank(url)) {
                url = environment.getProperty("spring.datasource.url");
            }
            DatabaseType fromUrl = inferFromJdbcUrl(url);
            if (fromUrl != null) {
                return fromUrl;
            }
        }
        return DatabaseType.fromConfig(databaseRaw);
    }

    /**
     * 从 JDBC URL 推断库类型；无法识别时返回 null。
     */
    public static DatabaseType inferFromJdbcUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return null;
        }
        String u = url.trim().toLowerCase(Locale.ROOT);
        if (u.startsWith("jdbc:postgresql:")) {
            return DatabaseType.POSTGRESQL;
        }
        if (u.startsWith("jdbc:oracle:")) {
            return DatabaseType.ORACLE;
        }
        if (u.startsWith("jdbc:sqlserver:") || u.startsWith("jdbc:microsoft:sqlserver:")) {
            return DatabaseType.SQLSERVER;
        }
        if (u.startsWith("jdbc:mariadb:")) {
            return DatabaseType.MARIADB;
        }
        if (u.startsWith("jdbc:mysql:")) {
            return DatabaseType.MYSQL;
        }
        return null;
    }
}
