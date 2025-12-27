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
    private final String name;

    /**
     * Key 类型
     */
    private final Class<?> key;

    /**
     * Value 类型
     */
    private final Class<?> value;

    /**
     * 最大条目数
     */
    @Builder.Default
    private final long max = 10000;

    /**
     * 过期时间
     */
    @Builder.Default
    private final long expire = 60;

    /**
     * Redis二级缓存过期时间倍数，本地缓存会大量消耗内存，并且有最大条目限制，
     * 因此需要通过Redis进行二级缓存，提高效率，并且不限制最大个数
     */
    @Builder.Default
    private final long redis = 3 * 60;

    /**
     * 过期时间单位
     */
    @Builder.Default
    private final TimeUnit unit = TimeUnit.MINUTES;

    /**
     * 缓存null值
     */
    @Builder.Default
    private final boolean Null = true;

    /**
     * 是否启用磁盘持久化
     */
    @Builder.Default
    private final boolean persistent = false;

    /**
     * 磁盘持久化路径（如果启用）
     */
    private final String path;

    /**
     * 验证必填字段
     * 在使用 Builder 构建后调用此方法进行验证
     */
    public void validate() {
        if (name == null || key == null || value == null) {
            throw new IllegalArgumentException("cacheName, keyType, and valueType are required");
        }
    }
}

