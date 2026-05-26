package cn.org.autumn.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(AllInOneHandler.class)
public interface AllInOneHandler {
    /**
     * 是否为 All-in-One 部署模式（勿使用 {@code is()}，会导致 Spring Bean 内省失败）。
     */
    default boolean isAllInOne() {
        return false;
    }
}
