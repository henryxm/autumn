package cn.org.autumn.config;

import cn.org.autumn.model.UserContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(ContextHandler.class)
public interface ContextHandler {
    UserContext getUserContext(String uuid);
}