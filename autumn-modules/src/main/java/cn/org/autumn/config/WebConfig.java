package cn.org.autumn.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.*;

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
        if (null == interceptorHandlers || interceptorHandlers.isEmpty())
            return;
        for (InterceptorHandler interceptorHandler : interceptorHandlers) {
            if (null == interceptorHandler.getHandlerInterceptor())
                continue;
            InterceptorRegistration tmp = registry.addInterceptor(interceptorHandler.getHandlerInterceptor());
            if (null != interceptorHandler.getPatterns() && !interceptorHandler.getPatterns().isEmpty()) {
                tmp.addPathPatterns(interceptorHandler.getPatterns());
            }
            if (null != interceptorHandler.getExcludePatterns() && !interceptorHandler.getExcludePatterns().isEmpty()) {
                tmp.excludePathPatterns(interceptorHandler.getExcludePatterns());
            }
        }
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        if (null != resolverHandlers && !resolverHandlers.isEmpty()) {
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
        if (null != resourceHandlers && !resourceHandlers.isEmpty()) {
            for (ResourceHandler resourceHandler : resourceHandlers) {
                resourceHandler.apply(registry);
            }
        }
    }
}