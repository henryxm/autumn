package cn.org.autumn.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@DependsOn({"env"})
public class WebConfig implements WebMvcConfigurer {

    @Autowired(required = false)
    List<ResourceHandler> resourceHandlers;

    @Autowired(required = false)
    List<InterceptorHandler> interceptorHandlers;

    @Autowired(required = false)
    List<ResolverHandler> resolverHandlers;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (null == interceptorHandlers || interceptorHandlers.size() == 0)
            return;
        for (InterceptorHandler interceptorHandler : interceptorHandlers) {
            if (null == interceptorHandler.getHandlerInterceptor())
                continue;
            InterceptorRegistration tmp = registry.addInterceptor(interceptorHandler.getHandlerInterceptor());
            if (null != interceptorHandler.getPatterns() && interceptorHandler.getPatterns().size() > 0) {
                tmp.addPathPatterns(interceptorHandler.getPatterns());
            }
            if (null != interceptorHandler.getExcludePatterns() && interceptorHandler.getExcludePatterns().size() > 0) {
                tmp.excludePathPatterns(interceptorHandler.getExcludePatterns());
            }
        }
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        if (null != resolverHandlers && resolverHandlers.size() > 0) {
            for (ResolverHandler resolverHandler : resolverHandlers) {
                if (null != resolverHandler.getResolver())
                    argumentResolvers.add(resolverHandler.getResolver());
            }
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/statics/**").addResourceLocations("classpath:/statics/");
        registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
        registry.addResourceHandler("/js/**").addResourceLocations("classpath:/js/");
        registry.addResourceHandler("/css/**").addResourceLocations("classpath:/css/");
        registry.addResourceHandler("/images/**").addResourceLocations("classpath:/images/");
        if (null != resourceHandlers && resourceHandlers.size() > 0) {
            for (ResourceHandler resourceHandler : resourceHandlers) {
                resourceHandler.apply(registry);
            }
        }
    }
}