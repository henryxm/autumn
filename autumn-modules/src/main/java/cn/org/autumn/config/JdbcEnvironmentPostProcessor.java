package cn.org.autumn.config;

import cn.org.autumn.database.DatabaseHolder;
import cn.org.autumn.database.DatabaseType;
import cn.org.autumn.database.H2EmbeddedMysqlDialect;
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
 *   <li>{@code mybatis-plus.global-config.identifier-quote}（未显式配置时）：MP 对保留字列名等转义；须与 JDBC 方言一致</li>
 *   <li>{@code mybatis-plus.global-config.db-config.column-format} / {@code table-format}（未显式配置且主库为 {@code jdbc:h2:} 时）：
 *   内嵌 H2 禁止沿用 {@link DatabaseType#MYSQL} 的反引号；表名亦须双引号，与注解 DDL 一致</li>
 * </ul>
 * 与 {@link DatabaseHolder#getType()}、{@link MybatisPlusConfig} 对齐。
 * <p>
 * {@code jdbc:h2:} 且 {@code MODE=MySQL} 时 {@link DatabaseHolder#resolveType} 仍为 {@link DatabaseType#MYSQL}（注解建表），
 * 但运行期 SQL 引号与 PageHelper 方言必须按 H2 处理。
 */
public class JdbcEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PAGEHELPER_HELPER_DIALECT = "pagehelper.helper-dialect";

    private static final String MYBATIS_PLUS_IDENTIFIER_QUOTE = "mybatis-plus.global-config.identifier-quote";

    private static final String MYBATIS_PLUS_COLUMN_FORMAT = "mybatis-plus.global-config.db-config.column-format";

    private static final String MYBATIS_PLUS_TABLE_FORMAT = "mybatis-plus.global-config.db-config.table-format";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String url = DatabaseHolder.readPrimaryJdbcUrl(environment);
        String autumnDb = environment.getProperty("autumn.database");
        if (StringUtils.isBlank(url) && StringUtils.isBlank(autumnDb)) {
            return;
        }
        DatabaseType t = DatabaseHolder.resolveType(url, autumnDb);
        boolean jdbcH2 = H2EmbeddedMysqlDialect.isJdbcH2(url);

        Map<String, Object> map = new HashMap<>(8);
        if (StringUtils.isBlank(environment.getProperty(PAGEHELPER_HELPER_DIALECT)) && t != DatabaseType.OTHER) {
            map.put(PAGEHELPER_HELPER_DIALECT, jdbcH2 ? "h2" : t.pageHelperDialectName());
        }
        if (StringUtils.isBlank(environment.getProperty(MYBATIS_PLUS_IDENTIFIER_QUOTE))) {
            String quotePattern = jdbcH2 ? "\"%s\"" : t.mybatisPlusIdentifierQuotePattern();
            if (quotePattern != null) {
                map.put(MYBATIS_PLUS_IDENTIFIER_QUOTE, quotePattern);
            }
        }
        if (jdbcH2 && StringUtils.isBlank(environment.getProperty(MYBATIS_PLUS_COLUMN_FORMAT))) {
            map.put(MYBATIS_PLUS_COLUMN_FORMAT, "\"%s\"");
        }
        if (jdbcH2 && StringUtils.isBlank(environment.getProperty(MYBATIS_PLUS_TABLE_FORMAT))) {
            map.put(MYBATIS_PLUS_TABLE_FORMAT, "\"%s\"");
        }
        if (map.isEmpty()) {
            return;
        }
        environment.getPropertySources().addFirst(new MapPropertySource("jdbcInferredPageHelper", map));
    }
}
