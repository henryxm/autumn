package cn.org.autumn.config;

import cn.org.autumn.service.BaseHttpProxyService;
import cn.org.autumn.xss.XssFilterInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * XSS 过滤拦截器配置
 * <p>
 * 对代理、静态资源等路径排除，避免无效执行并减少对透传请求的影响。
 */
@Configuration
public class XssFilterConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(new XssFilterInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(
                        BaseHttpProxyService.DEFAULT + "/**",
                        "/static/**",
                        "/statics/**",
                        "/js/**",
                        "/css/**",
                        "/images/**",
                        "/files/**",
                        "/actuator/**",
                        "/favicon.ico"
                )
                .order(1);
    }
}