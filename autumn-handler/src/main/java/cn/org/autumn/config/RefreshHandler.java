package cn.org.autumn.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(RefreshHandler.class)
public interface RefreshHandler {
    default boolean isRefresh() {
        return true;
    }
}
