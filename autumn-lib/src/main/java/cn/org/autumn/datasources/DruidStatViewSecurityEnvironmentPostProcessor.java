package cn.org.autumn.datasources;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Druid StatView 安全加固（早于 Bean 创建）：
 * <ul>
 *   <li>启用但账号/密码为空 → 强制关闭（空账号=关闭 Druid 自身认证=裸奔）</li>
 *   <li>生产环境弱密码（如 changeme）→ 强制关闭</li>
 *   <li>{@code allow} 配成空串（全网开放）→ 回退为 127.0.0.1</li>
 * </ul>
 */
@Slf4j
@Order(DruidStatViewSecurityEnvironmentPostProcessor.ORDER)
public class DruidStatViewSecurityEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 60;

    private static final String PS_NAME = "autumnDruidStatViewSecurity";
    private static final String ENABLED = "spring.datasource.druid.stat-view-servlet.enabled";
    private static final String LOGIN_USER = "spring.datasource.druid.stat-view-servlet.login-username";
    private static final String LOGIN_PASS = "spring.datasource.druid.stat-view-servlet.login-password";
    private static final String ALLOW = "spring.datasource.druid.stat-view-servlet.allow";
    private static final String DEFAULT_ALLOW = "127.0.0.1";

    private static final Set<String> WEAK_PASSWORDS = new HashSet<>(Arrays.asList(
            "changeme", "admin", "password", "123456", "druid", "root", "12345678"
    ));

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        boolean enabled = Boolean.parseBoolean(environment.getProperty(ENABLED, "false"));
        if (!enabled) {
            return;
        }

        Map<String, Object> patch = new HashMap<>(4);
        String username = StringUtils.trimToEmpty(environment.getProperty(LOGIN_USER));
        String password = StringUtils.trimToEmpty(environment.getProperty(LOGIN_PASS));

        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            patch.put(ENABLED, "false");
            log.warn("Druid 监控页已启用但 login-username/login-password 为空，已强制关闭（空账号等于关闭认证）；请设置 DRUID_LOGIN_USERNAME/DRUID_LOGIN_PASSWORD 后再开 DRUID_STAT_VIEW_ENABLED");
        } else if (isProdLike(environment) && isWeakPassword(password)) {
            patch.put(ENABLED, "false");
            log.warn("生产环境 Druid 监控页使用弱密码，已强制关闭；请设置强密码环境变量 DRUID_LOGIN_PASSWORD 后再开 DRUID_STAT_VIEW_ENABLED");
        }

        // allow 显式空串表示「全部 IP」，危险；未配置时留给 starter 默认 127.0.0.1
        if (environment.getProperty(ALLOW) != null && StringUtils.isBlank(environment.getProperty(ALLOW))) {
            patch.put(ALLOW, DEFAULT_ALLOW);
            log.warn("Druid 监控页 allow 为空串（全网开放），已回退为 {}", DEFAULT_ALLOW);
        }

        if (patch.isEmpty()) {
            return;
        }
        environment.getPropertySources().addFirst(new MapPropertySource(PS_NAME, patch));
    }

    static boolean isProdLike(ConfigurableEnvironment environment) {
        for (String p : environment.getActiveProfiles()) {
            if (p == null) {
                continue;
            }
            String n = p.toLowerCase(Locale.ROOT);
            if ("prod".equals(n) || "prod-docker".equals(n) || n.startsWith("prod-")) {
                return true;
            }
        }
        return false;
    }

    static boolean isWeakPassword(String password) {
        return WEAK_PASSWORDS.contains(password.toLowerCase(Locale.ROOT));
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
