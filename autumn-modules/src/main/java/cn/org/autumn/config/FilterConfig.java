package cn.org.autumn.config;

import cn.org.autumn.modules.sys.shiro.HostSessionCookieFilter;
import cn.org.autumn.xss.XssFilter;
import javax.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.DelegatingFilterProxy;

@Configuration
public class FilterConfig {

    @Autowired
    private HostSessionCookieFilter hostSessionCookieFilter;

    @Bean
    public FilterRegistrationBean hostSessionCookieFilterRegistration() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(hostSessionCookieFilter);
        registration.addUrlPatterns("/*");
        registration.setName("hostSessionCookieFilter");
        registration.setOrder(Integer.MAX_VALUE - 2);
        registration.setAsyncSupported(true);
        return registration;
    }

    @Bean
    public FilterRegistrationBean shiroFilterRegistration() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(new DelegatingFilterProxy("shiroFilter"));
        //该值缺省为false，表示生命周期由SpringApplicationContext管理，设置为true则表示由ServletContainer管理
        registration.addInitParameter("targetFilterLifecycle", "true");
        registration.setEnabled(true);
        registration.setOrder(Integer.MAX_VALUE - 1);
        registration.addUrlPatterns("/*");
        registration.setAsyncSupported(true);
        return registration;
    }

    @Bean
    public FilterRegistrationBean xssFilterRegistration() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setDispatcherTypes(DispatcherType.REQUEST);
        registration.setFilter(new XssFilter());
        registration.addUrlPatterns("/*");
        registration.setName("xssFilter");
        registration.setOrder(Integer.MAX_VALUE);
        registration.setAsyncSupported(true);
        return registration;
    }
}
