package cn.org.autumn.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(PluginHandler.class)
@Order(Integer.MAX_VALUE / 100)
public interface PluginHandler {

    void installPlugin();
}
