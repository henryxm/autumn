package cn.org.autumn.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

@Component
@ConditionalOnClass(ResolverHandler.class)
public interface ResolverHandler {
    default HandlerMethodArgumentResolver getResolver() {
        return null;
    }
}
