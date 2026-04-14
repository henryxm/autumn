package cn.org.autumn.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(ConfigHandler.class)
public interface ConfigHandler {

    boolean hasKey(String key);

    String getValue(String key);

    boolean getBoolean(String key);

    int getInt(String key);

    <T> T getObject(String key, Class<T> clazz);
}
