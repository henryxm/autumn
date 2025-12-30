package cn.org.autumn.config;

import cn.org.autumn.modules.oauth.resolver.EncryptArgumentResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.lang.NonNull;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;
import org.springframework.web.servlet.config.annotation.*;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
@DependsOn({"env"})
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
                    // 移除已存在的EncryptArgumentResolver（如果存在）
                    newResolvers.removeIf(resolver -> resolver instanceof EncryptArgumentResolver);
                    // 查找RequestResponseBodyMethodProcessor的位置
                    // 注意：EncryptArgumentResolver也继承自RequestResponseBodyMethodProcessor，需要排除
                    int index = -1;
                    for (int i = 0; i < newResolvers.size(); i++) {
                        HandlerMethodArgumentResolver resolver = newResolvers.get(i);
                        // 只匹配真正的RequestResponseBodyMethodProcessor，排除EncryptArgumentResolver
                        if (resolver instanceof RequestResponseBodyMethodProcessor && !(resolver instanceof EncryptArgumentResolver)) {
                            index = i;
                            break;
                        }
                    }
                    // 如果找到了RequestResponseBodyMethodProcessor，将EncryptArgumentResolver插入到它之前
                    // 如果没有找到，添加到列表末尾
                    if (index >= 0) {
                        newResolvers.add(index, encryptArgumentResolver);
                    } else {
                        newResolvers.add(encryptArgumentResolver);
                    }
                    requestMappingHandlerAdapter.setArgumentResolvers(newResolvers);
                }
            } catch (Exception e) {
                log.error("设置解析器失败:{}", e.getMessage());
            }
        }
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        if (null == interceptorHandlers || interceptorHandlers.isEmpty())
            return;

        // 统一的排除路径，所有拦截器都会排除这些路径
        List<String> commonExcludePatterns = new ArrayList<>();
        commonExcludePatterns.add("/favicon.ico");

        for (InterceptorHandler interceptorHandler : interceptorHandlers) {
            if (null == interceptorHandler.getHandlerInterceptor())
                continue;
            InterceptorRegistration tmp = registry.addInterceptor(interceptorHandler.getHandlerInterceptor());
            if (null != interceptorHandler.getPatterns() && !interceptorHandler.getPatterns().isEmpty()) {
                tmp.addPathPatterns(interceptorHandler.getPatterns());
            }

            // 合并通用排除路径和拦截器自定义的排除路径
            List<String> allExcludePatterns = new ArrayList<>(commonExcludePatterns);
            if (null != interceptorHandler.getExcludePatterns() && !interceptorHandler.getExcludePatterns().isEmpty()) {
                allExcludePatterns.addAll(interceptorHandler.getExcludePatterns());
            }

            if (!allExcludePatterns.isEmpty()) {
                tmp.excludePathPatterns(allExcludePatterns);
            }
        }
    }

    @Override
    public void addArgumentResolvers(@NonNull List<HandlerMethodArgumentResolver> argumentResolvers) {
        if (null != resolverHandlers && !resolverHandlers.isEmpty()) {
            for (ResolverHandler resolverHandler : resolverHandlers) {
                argumentResolvers.add(resolverHandler.getResolver());
            }
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