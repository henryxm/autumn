package cn.org.autumn.config;

import cn.org.autumn.database.DatabaseHolder;
import cn.org.autumn.database.DatabaseType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * 按主数据源 JDBC URL（及 {@code autumn.database}、H2 URL 中 {@code MODE=MySQL}）推断并注入：
 * <ul>
 *   <li>{@code pagehelper.helper-dialect}（未显式配置时）</li>
 *   <li>{@code mybatis-plus.global-config.identifier-quote}（未显式配置时）：供 MP 2.x
 *   {@code SqlReservedWords} 对保留字列名按库转义，避免在实体上写死 {@code @TableField("`order`")} 等与 PostgreSQL 等不兼容的写法</li>
 * </ul>
 * 与 {@link DatabaseHolder#getType()}、{@link MybatisPlusConfig#paginationInterceptor()} 对齐。
 */
public class JdbcEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PAGEHELPER_HELPER_DIALECT = "pagehelper.helper-dialect";

    private static final String MYBATIS_PLUS_IDENTIFIER_QUOTE = "mybatis-plus.global-config.identifier-quote";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String url = environment.getProperty("spring.datasource.druid.first.url");
        if (StringUtils.isBlank(url)) {
            url = environment.getProperty("spring.datasource.url");
        }
        String autumnDb = environment.getProperty("autumn.database");
        if (StringUtils.isBlank(url) && StringUtils.isBlank(autumnDb)) {
            return;
        }
        DatabaseType t = DatabaseHolder.resolveType(url, autumnDb);

        Map<String, Object> map = new HashMap<>(4);
        if (StringUtils.isBlank(environment.getProperty(PAGEHELPER_HELPER_DIALECT)) && t != DatabaseType.OTHER) {
            map.put(PAGEHELPER_HELPER_DIALECT, t.pageHelperDialectName());
        }
        if (StringUtils.isBlank(environment.getProperty(MYBATIS_PLUS_IDENTIFIER_QUOTE))) {
            String quotePattern = t.mybatisPlusIdentifierQuotePattern();
            if (quotePattern != null) {
                map.put(MYBATIS_PLUS_IDENTIFIER_QUOTE, quotePattern);
            }
        }
        if (map.isEmpty()) {
            return;
        }
        environment.getPropertySources().addFirst(new MapPropertySource("jdbcInferredPageHelper", map));
    }
}
