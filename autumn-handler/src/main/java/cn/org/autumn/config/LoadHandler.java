package cn.org.autumn.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(LoadHandler.class)
public interface LoadHandler {
    default boolean isBefore() {
        return true;
    }

    default boolean isLoad() {
        return true;
    }

    default boolean isAfter() {
        return true;
    }

    default boolean isPost() {
        return true;
    }
}
