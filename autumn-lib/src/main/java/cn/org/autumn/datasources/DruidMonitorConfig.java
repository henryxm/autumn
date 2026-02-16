package cn.org.autumn.datasources;

import com.alibaba.druid.support.jakarta.StatViewServlet;
import com.alibaba.druid.support.jakarta.WebStatFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Druid 监控页面配置。
 * <p>
 * Spring Boot 4 无 druid-spring-boot-4-starter，StatViewServlet 和 WebStatFilter
 * 不再自动注册，需要手动配置。
 * <p>
 * 使用 {@code com.alibaba.druid.support.jakarta} 包中的 Jakarta Servlet 兼容类。
 */
@Configuration
public class DruidMonitorConfig {

    /**
     * 注册 Druid StatViewServlet，提供 /druid/* 监控页面。
     */
    @Bean
    public ServletRegistrationBean<StatViewServlet> druidStatViewServlet() {
        ServletRegistrationBean<StatViewServlet> registration = new ServletRegistrationBean<>(new StatViewServlet(), "/druid/*");
        // 允许访问的 IP（空字符串表示全部允许）
        registration.addInitParameter("allow", "");
        // 禁止访问的 IP
        // registration.addInitParameter("deny", "");
        // 控制台管理用户（按需开启）
        // registration.addInitParameter("loginUsername", "admin");
        // registration.addInitParameter("loginPassword", "admin");
        // 是否能够重置数据
        registration.addInitParameter("resetEnable", "false");
        return registration;
    }

    /**
     * 注册 Druid WebStatFilter，用于采集 Web 请求的监控数据。
     */
    @Bean
    public FilterRegistrationBean<WebStatFilter> druidWebStatFilter() {
        FilterRegistrationBean<WebStatFilter> registration = new FilterRegistrationBean<>(new WebStatFilter());
        registration.addUrlPatterns("/*");
        // 排除不需要统计的路径
        registration.addInitParameter("exclusions", "*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*");
        return registration;
    }
}