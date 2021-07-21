package cn.org.autumn.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

@Component
@ConditionalOnMissingBean(ResourceHandler.class)
public interface ResourceHandler {
    default void apply(ResourceHandlerRegistry registry) {
    }
}