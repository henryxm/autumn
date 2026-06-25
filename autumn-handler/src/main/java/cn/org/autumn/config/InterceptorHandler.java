package cn.org.autumn.config;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

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
