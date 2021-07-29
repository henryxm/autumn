package cn.org.autumn.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * 向系统注入变量
 */
@Component
@ConditionalOnMissingBean(VariablesHandler.class)
public interface VariablesHandler {
    default String getName() {
        String name = getClass().getSimpleName();
        if (Character.isLowerCase(name.charAt(0)))
            return name;
        else
            return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
}