package cn.org.autumn.config;

import cn.org.autumn.database.DatabaseHolder;
import cn.org.autumn.database.DatabaseType;
import org.apache.commons.lang.StringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 按主数据源 JDBC URL（及 {@code autumn.database}、H2 URL 中 {@code MODE=MySQL}）推断并注入：
 * <ul>
 *   <li>{@code pagehelper.helper-dialect}（未显式配置时）</li>
 *   <li>{@code mybatis-plus.global-config.identifier-quote}（未显式配置时）：供 MP 2.x
 *   {@code SqlReservedWords} 对保留字列名按库转义，避免在实体上写死 {@code @TableField("`order`")} 等与 PostgreSQL 等不兼容的写法</li>
 *   <li><b>仅 SQLite</b>：主数据源 URL 解析为 SQLite 时，若 {@code spring.datasource.druid.first.*} / {@code second.*}
 *   未配置连接池项，则注入 Druid 安全默认值（关闭 PSCache、小池、同步初始化），避免池参数写在 druid 与 first 同级不生效</li>
 *   <li><b>仅 SQLite 文件库</b>：在 Spring 刷新前按 JDBC URL 创建 .db 父目录；非 SQLite 不执行</li>
 * </ul>
 * 与 {@link DatabaseHolder#getType()}、{@link MybatisPlusConfig#paginationInterceptor()} 对齐。
 */
public class JdbcEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PAGEHELPER_HELPER_DIALECT = "pagehelper.helper-dialect";

    private static final String MYBATIS_PLUS_IDENTIFIER_QUOTE = "mybatis-plus.global-config.identifier-quote";

    private static final String DRUID_FIRST_PREFIX = "spring.datasource.druid.first.";

    private static final String DRUID_SECOND_PREFIX = "spring.datasource.druid.second.";

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

        Map<String, Object> map = new HashMap<>(24);
        if (StringUtils.isBlank(environment.getProperty(PAGEHELPER_HELPER_DIALECT)) && t != DatabaseType.OTHER) {
            map.put(PAGEHELPER_HELPER_DIALECT, t.pageHelperDialectName());
        }
        if (StringUtils.isBlank(environment.getProperty(MYBATIS_PLUS_IDENTIFIER_QUOTE))) {
            String quotePattern = t.mybatisPlusIdentifierQuotePattern();
            if (quotePattern != null) {
                map.put(MYBATIS_PLUS_IDENTIFIER_QUOTE, quotePattern);
            }
        }
        if (t == DatabaseType.SQLITE) {
            ensureSqliteFileParentDirectories(url);
            String secondUrl = environment.getProperty("spring.datasource.druid.second.url");
            if (StringUtils.isNotBlank(secondUrl)) {
                ensureSqliteFileParentDirectories(secondUrl);
            }
            putSqliteDruidDefaultsIfAbsent(environment, map);
        }
        if (map.isEmpty()) {
            return;
        }
        environment.getPropertySources().addFirst(new MapPropertySource("jdbcInferredPageHelper", map));
    }

    /**
     * 仅当对应 {@code spring.datasource.druid.first.*} / {@code second.*} 键未配置时写入，
     * 不覆盖 application 中已写在正确前缀下的值。
     */
    private static void putSqliteDruidDefaultsIfAbsent(ConfigurableEnvironment environment, Map<String, Object> map) {
        putIfPropertyBlank(environment, map, DRUID_FIRST_PREFIX + "initial-size", 1);
        putIfPropertyBlank(environment, map, DRUID_FIRST_PREFIX + "min-idle", 1);
        putIfPropertyBlank(environment, map, DRUID_FIRST_PREFIX + "max-active", 16);
        putIfPropertyBlank(environment, map, DRUID_FIRST_PREFIX + "max-wait", 10000);
        putIfPropertyBlank(environment, map, DRUID_FIRST_PREFIX + "pool-prepared-statements", false);
        putIfPropertyBlank(environment, map, DRUID_FIRST_PREFIX + "max-pool-prepared-statement-per-connection-size", 0);
        putIfPropertyBlank(environment, map, DRUID_FIRST_PREFIX + "async-init", false);

        putIfPropertyBlank(environment, map, DRUID_SECOND_PREFIX + "initial-size", 1);
        putIfPropertyBlank(environment, map, DRUID_SECOND_PREFIX + "min-idle", 1);
        putIfPropertyBlank(environment, map, DRUID_SECOND_PREFIX + "max-active", 16);
        putIfPropertyBlank(environment, map, DRUID_SECOND_PREFIX + "max-wait", 10000);
        putIfPropertyBlank(environment, map, DRUID_SECOND_PREFIX + "pool-prepared-statements", false);
        putIfPropertyBlank(environment, map, DRUID_SECOND_PREFIX + "max-pool-prepared-statement-per-connection-size", 0);
        putIfPropertyBlank(environment, map, DRUID_SECOND_PREFIX + "async-init", false);
    }

    /**
     * 文件型 SQLite：确保 .db 所在父目录存在（相对路径相对 {@code user.dir}）。
     * 内存库、{@code :resource:} 等跳过。
     */
    private static void ensureSqliteFileParentDirectories(String jdbcUrl) {
        if (StringUtils.isBlank(jdbcUrl)) {
            return;
        }
        String u = jdbcUrl.trim();
        final String prefix = "jdbc:sqlite:";
        if (u.length() < prefix.length() || !u.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return;
        }
        String loc = u.substring(prefix.length());
        int q = loc.indexOf('?');
        if (q >= 0) {
            loc = loc.substring(0, q);
        }
        loc = loc.trim();
        if (loc.isEmpty()) {
            return;
        }
        String ll = loc.toLowerCase(Locale.ROOT);
        if (":memory:".equals(ll) || ll.startsWith(":resource:")) {
            return;
        }
        if (loc.startsWith(":")) {
            return;
        }
        try {
            Path p = Paths.get(loc);
            if (!p.isAbsolute()) {
                Path cwd = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
                p = cwd.resolve(p).normalize();
            }
            Path parent = p.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (Exception ignored) {
            // 连接阶段会抛出 SQLException，便于排查权限等问题
        }
    }

    private static void putIfPropertyBlank(ConfigurableEnvironment environment, Map<String, Object> map, String key, Object value) {
        if (StringUtils.isBlank(environment.getProperty(key))) {
            map.put(key, value);
        }
    }
}
