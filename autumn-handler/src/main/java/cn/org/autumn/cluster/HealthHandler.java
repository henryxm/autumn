package cn.org.autumn.cluster;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(HealthHandler.class)
public interface HealthHandler {
    default String name() {
        return getClass().getName();
    }

    default Object value() {
        return "";
    }
}
