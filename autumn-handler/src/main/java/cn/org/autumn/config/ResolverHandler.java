package cn.org.autumn.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

@Component
@ConditionalOnMissingBean(ResolverHandler.class)
public interface ResolverHandler {

    /**
     * 注册顺序（越小越先匹配）。遗留 {@code UserInfoResolver} 宜为 0，{@code UserContext} 宜更大。
     */
    default int getOrder() {
        return 0;
    }

    default HandlerMethodArgumentResolver getResolver() {
        return null;
    }
}
