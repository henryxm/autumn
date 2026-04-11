package cn.org.autumn.config;

import cn.org.autumn.database.DatabaseHolder;
import cn.org.autumn.database.DatabaseType;
import org.apache.commons.lang.StringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * 未显式配置 {@code pagehelper.helper-dialect} 时，按主数据源 JDBC URL 推断并注入，
 * 与 {@link DatabaseHolder#getType()}、MyBatis-Plus {@link MybatisPlusConfig#paginationInterceptor()} 对齐。
 */
public class JdbcEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PAGEHELPER_HELPER_DIALECT = "pagehelper.helper-dialect";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (StringUtils.isNotBlank(environment.getProperty(PAGEHELPER_HELPER_DIALECT))) {
            return;
        }
        String url = environment.getProperty("spring.datasource.druid.first.url");
        if (StringUtils.isBlank(url)) {
            url = environment.getProperty("spring.datasource.url");
        }
        String autumnDb = environment.getProperty("autumn.database");
        if (StringUtils.isBlank(url) && StringUtils.isBlank(autumnDb)) {
            return;
        }
        DatabaseType t = DatabaseHolder.resolveType(url, autumnDb);
        if (t == DatabaseType.OTHER) {
            return;
        }
        Map<String, Object> map = new HashMap<>(2);
        map.put(PAGEHELPER_HELPER_DIALECT, t.pageHelperDialectName());
        environment.getPropertySources().addFirst(new MapPropertySource("jdbcInferredPageHelper", map));
    }
}
