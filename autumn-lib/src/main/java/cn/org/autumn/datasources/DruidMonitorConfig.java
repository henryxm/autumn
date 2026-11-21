package cn.org.autumn.datasources;

import com.alibaba.druid.support.jakarta.StatViewServlet;
import com.alibaba.druid.support.jakarta.WebStatFilter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Druid 监控页 / Web 统计的<strong>安全兜底</strong>注册。
 * <p>
 * Boot 3 + {@code druid-spring-boot-3-starter} 已按
 * {@code spring.datasource.druid.stat-view-servlet.enabled} /
 * {@code web-stat-filter.enabled} 条件注册；本类仅在 starter Bean 缺失时生效
 * （例如未来 Boot 4 无对应 starter 自动配置）。
 * <p>
 * 历史实现曾<strong>无条件</strong>注册 {@code /druid/*} 且 {@code allow=""}、无登录，
 * 会绕过 yml 关闭开关并对外裸奔，已废弃。
 */
@Configuration
@ConditionalOnWebApplication
public class DruidMonitorConfig {

    private static final String DEFAULT_ALLOW = "127.0.0.1";

    /**
     * 仅在启用且 starter 未注册时兜底；强制非空账号并默认本机 allow。
     */
    @Bean
    @ConditionalOnProperty(name = "spring.datasource.druid.stat-view-servlet.enabled", havingValue = "true")
    @ConditionalOnMissingBean(name = "statViewServletRegistrationBean")
    public ServletRegistrationBean<StatViewServlet> druidStatViewServlet(Environment environment) {
        String username = StringUtils.trimToEmpty(environment.getProperty("spring.datasource.druid.stat-view-servlet.login-username"));
        String password = StringUtils.trimToEmpty(environment.getProperty("spring.datasource.druid.stat-view-servlet.login-password"));
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            throw new IllegalStateException("Druid stat-view-servlet 已启用但 login-username/login-password 为空；请设置 DRUID_LOGIN_* 或关闭 DRUID_STAT_VIEW_ENABLED");
        }
        String allow = StringUtils.trimToEmpty(environment.getProperty("spring.datasource.druid.stat-view-servlet.allow"));
        if (StringUtils.isBlank(allow)) {
            allow = DEFAULT_ALLOW;
        }
        String urlPattern = environment.getProperty("spring.datasource.druid.stat-view-servlet.url-pattern", "/druid/*");
        String resetEnable = environment.getProperty("spring.datasource.druid.stat-view-servlet.reset-enable", "false");

        ServletRegistrationBean<StatViewServlet> registration = new ServletRegistrationBean<>(new StatViewServlet(), urlPattern);
        registration.addInitParameter("allow", allow);
        registration.addInitParameter("loginUsername", username);
        registration.addInitParameter("loginPassword", password);
        registration.addInitParameter("resetEnable", resetEnable);
        String deny = StringUtils.trimToNull(environment.getProperty("spring.datasource.druid.stat-view-servlet.deny"));
        if (deny != null) {
            registration.addInitParameter("deny", deny);
        }
        return registration;
    }

    @Bean
    @ConditionalOnProperty(name = "spring.datasource.druid.web-stat-filter.enabled", havingValue = "true")
    @ConditionalOnMissingBean(name = "webStatFilterRegistrationBean")
    public FilterRegistrationBean<WebStatFilter> druidWebStatFilter(Environment environment) {
        FilterRegistrationBean<WebStatFilter> registration = new FilterRegistrationBean<>(new WebStatFilter());
        String urlPattern = environment.getProperty("spring.datasource.druid.web-stat-filter.url-pattern", "/*");
        registration.addUrlPatterns(urlPattern);
        registration.setAsyncSupported(true);
        String exclusions = environment.getProperty("spring.datasource.druid.web-stat-filter.exclusions",
                "*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*");
        registration.addInitParameter("exclusions", exclusions);
        return registration;
    }
}
