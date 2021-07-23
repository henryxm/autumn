package cn.org.autumn.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

@Component
@ConditionalOnMissingBean(ResolverHandler.class)
public interface ResolverHandler {
    default HandlerMethodArgumentResolver getResolver() {
        return null;
    }
}
