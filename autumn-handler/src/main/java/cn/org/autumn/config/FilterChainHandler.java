package cn.org.autumn.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import java.util.Map;

@Component
@ConditionalOnMissingBean(FilterChainHandler.class)
public interface FilterChainHandler {
    default void definition(Map<String, String> map) {
    }

    default void filter(Map<String, Filter> map) {
    }

    default String login(String last) {
        return null;
    }

    default String unauthorized(String last) {
        return null;
    }

    default String success(String last) {
        return null;
    }
}