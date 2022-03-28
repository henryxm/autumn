package cn.org.autumn.cluster;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(VersionHandler.class)
public interface VersionHandler {
    default String name() {
        return getClass().getName();
    }

    default Object version() {
        return "2.0.0.0";
    }
}
