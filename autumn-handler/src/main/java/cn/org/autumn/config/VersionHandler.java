package cn.org.autumn.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * 依赖库版本信息处理器。
 * <p>
 * 各模块实现此接口并注册为 Spring Bean，框架启动时将自动收集所有实现，
 * 统一打印版本号信息。
 */
@Component
@ConditionalOnMissingBean(VersionHandler.class)
public interface VersionHandler {

    /**
     * 返回组件/库的显示名称，如 "Spring Boot"、"MyBatis-Plus"
     */
    String name();

    /**
     * 返回组件/库的版本号
     */
    String version();

    /**
     * 工具方法：从指定 Class 的 MANIFEST.MF 中读取 Implementation-Version
     */
    default String version(Class<?> clazz) {
        String version = clazz.getPackage().getImplementationVersion();
        return version != null ? version : "unknown";
    }
}
