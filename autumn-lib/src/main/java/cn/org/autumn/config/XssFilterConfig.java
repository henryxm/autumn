package cn.org.autumn.config;

import cn.org.autumn.xss.XssFilterInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * XSS过滤配置
 */
@Configuration
public class XssFilterConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(new XssFilterInterceptor()).addPathPatterns("/**").order(1); // 设置优先级，确保在其他拦截器之前执行
    }
}