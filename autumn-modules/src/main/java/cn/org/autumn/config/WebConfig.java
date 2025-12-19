package cn.org.autumn.config;

import cn.org.autumn.modules.oauth.resolver.EncryptArgumentResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.lang.NonNull;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.config.annotation.*;

import javax.annotation.PostConstruct;
import java.util.List;

@Slf4j
@Configuration
@DependsOn({"env"})
@org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
public class WebConfig implements WebMvcConfigurer {

    @Autowired(required = false)
    List<ResourceHandler> resourceHandlers;

    @Autowired(required = false)
    List<InterceptorHandler> interceptorHandlers;

    @Autowired(required = false)
    List<ResolverHandler> resolverHandlers;

    @Autowired(required = false)
    EncryptArgumentResolver encryptArgumentResolver;

    @Autowired(required = false)
    RequestMappingHandlerAdapter requestMappingHandlerAdapter;

    @PostConstruct
    public void ensureEncryptArgumentResolverFirst() {
        if (requestMappingHandlerAdapter != null && encryptArgumentResolver != null) {
            try {
                List<HandlerMethodArgumentResolver> argumentResolvers = requestMappingHandlerAdapter.getArgumentResolvers();
                if (argumentResolvers != null && !argumentResolvers.isEmpty()) {
                    List<HandlerMethodArgumentResolver> newResolvers = new java.util.ArrayList<>(argumentResolvers);
                    newResolvers.removeIf(resolver -> resolver instanceof EncryptArgumentResolver);
                    newResolvers.add(0, encryptArgumentResolver);
                    requestMappingHandlerAdapter.setArgumentResolvers(newResolvers);
                }
            } catch (Exception e) {
                log.error("设置解析器失败", e);
            }
        }
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
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
    public void addArgumentResolvers(@NonNull List<HandlerMethodArgumentResolver> argumentResolvers) {
        if (null != resolverHandlers && !resolverHandlers.isEmpty()) {
            for (ResolverHandler resolverHandler : resolverHandlers) {
                if (resolverHandler instanceof EncryptArgumentResolver) {
                    continue;
                }
                HandlerMethodArgumentResolver resolver = resolverHandler.getResolver();
                if (null != resolver) {
                    argumentResolvers.add(0, resolver);
                }
            }
        }
        if (encryptArgumentResolver != null) {
            argumentResolvers.add(0, encryptArgumentResolver);
        }
    }

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
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