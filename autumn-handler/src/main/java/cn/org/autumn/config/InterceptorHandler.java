package cn.org.autumn.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

@Component
@ConditionalOnMissingBean(InterceptorHandler.class)
public interface InterceptorHandler {
    default HandlerInterceptor getHandlerInterceptor() {
        return null;
    }

    default List<String> getPatterns() {
        return null;
    }

    default List<String> getExcludePatterns() {
        return null;
    }
}
