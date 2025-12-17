package cn.org.autumn.config;

import lombok.Builder;
import lombok.Getter;

import java.util.concurrent.TimeUnit;

/**
 * 缓存配置类
 * 用于定义缓存的元数据信息
 */
@Getter
@Builder
public class CacheConfig {
    /**
     * 缓存名称
     */
    private final String cacheName;

    /**
     * Key 类型
     */
    private final Class<?> keyType;

    /**
     * Value 类型
     */
    private final Class<?> valueType;

    /**
     * 最大条目数
     */
    @Builder.Default
    private final long maxEntries = 1000;

    /**
     * 过期时间
     */
    @Builder.Default
    private final long expireTime = 10;

    /**
     * 过期时间单位
     */
    @Builder.Default
    private final TimeUnit expireTimeUnit = TimeUnit.MINUTES;

    /**
     * 缓存null值
     */
    @Builder.Default
    private final boolean cacheNull = true;

    /**
     * 是否启用磁盘持久化
     */
    @Builder.Default
    private final boolean diskPersistent = false;

    /**
     * 磁盘持久化路径（如果启用）
     */
    private final String diskPath;

    /**
     * 验证必填字段
     * 在使用 Builder 构建后调用此方法进行验证
     */
    public void validate() {
        if (cacheName == null || keyType == null || valueType == null) {
            throw new IllegalArgumentException("cacheName, keyType, and valueType are required");
        }
    }
}

