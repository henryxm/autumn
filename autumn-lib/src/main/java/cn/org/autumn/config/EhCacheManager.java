package cn.org.autumn.config;

import lombok.extern.slf4j.Slf4j;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.expiry.Duration;
import org.ehcache.expiry.Expirations;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EhCache 缓存管理器
 * 提供统一的缓存管理功能，支持动态创建和管理多个缓存实例
 */
@Slf4j
@Component
public class EhCacheManager {
    /**
     * 默认的 CacheManager 实例
     */
    private CacheManager defaultCacheManager;

    /**
     * 缓存配置注册表
     */
    private final ConcurrentHashMap<String, CacheConfig> cacheConfigs = new ConcurrentHashMap<>();

    /**
     * 已创建的缓存实例
     */
    private final ConcurrentHashMap<String, Cache<?, ?>> caches = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 创建默认的 CacheManager
        defaultCacheManager = CacheManagerBuilder.newCacheManagerBuilder().build(true);
    }

    @PreDestroy
    public void destroy() {
        // 关闭所有缓存
        caches.clear();
        if (defaultCacheManager != null) {
            defaultCacheManager.close();
        }
    }

    /**
     * 注册缓存配置
     *
     * @param config 缓存配置
     */
    public void registerCacheConfig(CacheConfig config) {
        if (config == null || config.getCacheName() == null) {
            throw new IllegalArgumentException("CacheConfig cannot be null and cacheName is required");
        }
        cacheConfigs.put(config.getCacheName(), config);
    }

    /**
     * 根据配置创建或获取缓存实例
     *
     * @param config 缓存配置
     * @param <K>    Key 类型
     * @param <V>    Value 类型
     * @return 缓存实例
     */
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getOrCreateCache(CacheConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("CacheConfig cannot be null");
        }
        String cacheName = config.getCacheName();
        // 先检查本地缓存映射中是否已存在
        Cache<?, ?> existingCache = caches.get(cacheName);
        if (existingCache != null) {
            return (Cache<K, V>) existingCache;
        }
        // 尝试从 CacheManager 获取已存在的缓存（可能在其他地方已创建）
        if (defaultCacheManager != null) {
            try {
                Cache<K, V> managerCache = defaultCacheManager.getCache(cacheName, (Class<K>) config.getKeyType(), (Class<V>) config.getValueType());
                if (managerCache != null) {
                    // 缓存已存在，添加到本地映射中
                    caches.put(cacheName, managerCache);
                    // 确保配置已注册
                    if (!cacheConfigs.containsKey(cacheName)) {
                        registerCacheConfig(config);
                    }
                    if (log.isDebugEnabled())
                    log.debug("Cache '{}' already exists in CacheManager, reusing it", cacheName);
                    return managerCache;
                }
            } catch (Exception e) {
                if (log.isDebugEnabled())
                log.debug("Cache '{}' not found in CacheManager, will create new one", cacheName);
            }
        }
        // 注册配置
        registerCacheConfig(config);
        // 创建缓存配置
        CacheConfigurationBuilder<K, V> cacheConfigBuilder = CacheConfigurationBuilder
                .newCacheConfigurationBuilder(
                        (Class<K>) config.getKeyType(),
                        (Class<V>) config.getValueType(),
                        ResourcePoolsBuilder.heap(config.getMaxEntries()))
                .withExpiry(Expirations.timeToLiveExpiration(
                        Duration.of(config.getExpireTime(), config.getExpireTimeUnit())));
        // 如果启用磁盘持久化
        if (config.isDiskPersistent() && config.getDiskPath() != null) {
            cacheConfigBuilder = cacheConfigBuilder.withResourcePools(
                    ResourcePoolsBuilder.newResourcePoolsBuilder()
                            .heap(config.getMaxEntries(), EntryUnit.ENTRIES)
                            .disk(100, MemoryUnit.MB, true)
                            .build());
        }
        // 在 CacheManager 中创建缓存
        if (defaultCacheManager == null) {
            throw new IllegalStateException("CacheManager is not initialized");
        }
        if (!defaultCacheManager.getStatus().toString().equals("AVAILABLE")) {
            defaultCacheManager.init();
        }
        Cache<K, V> cache;
        try {
            cache = defaultCacheManager.createCache(cacheName, cacheConfigBuilder.build());
        } catch (IllegalStateException e) {
            // 如果缓存已存在（可能由于并发创建），尝试获取已存在的缓存
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                if (log.isDebugEnabled())
                    log.debug("Cache '{}' already exists, attempting to retrieve existing cache", cacheName);
                try {
                    cache = defaultCacheManager.getCache(cacheName, (Class<K>) config.getKeyType(), (Class<V>) config.getValueType());
                    if (cache != null) {
                        // 缓存已存在，添加到本地映射中
                        caches.put(cacheName, cache);
                        if (log.isDebugEnabled())
                            log.debug("Successfully retrieved existing cache '{}'", cacheName);
                        return cache;
                    }
                } catch (Exception ex) {
                    log.error("Failed to retrieve existing cache '{}': {}", cacheName, ex.getMessage());
                    throw new IllegalStateException("Cache '" + cacheName + "' already exists but cannot be retrieved", e);
                }
            }
            // 其他类型的 IllegalStateException，重新抛出
            throw e;
        }
        // 缓存实例
        caches.put(cacheName, cache);
        return cache;
    }

    /**
     * 根据缓存名称获取缓存实例
     *
     * @param cacheName 缓存名称
     * @param keyType   Key 类型
     * @param valueType Value 类型
     * @param <K>       Key 类型
     * @param <V>       Value 类型
     * @return 缓存实例，如果不存在返回 null
     */
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getCache(String cacheName, Class<K> keyType, Class<V> valueType) {
        Cache<?, ?> cache = caches.get(cacheName);
        if (cache != null) {
            return (Cache<K, V>) cache;
        }
        // 尝试从 CacheManager 获取
        if (defaultCacheManager != null) {
            try {
                Cache<K, V> managerCache = defaultCacheManager.getCache(cacheName, keyType, valueType);
                if (managerCache != null) {
                    caches.put(cacheName, managerCache);
                    return managerCache;
                }
            } catch (Exception e) {
                log.debug("Cache {} not found in CacheManager", cacheName);
            }
        }
        return null;
    }

    /**
     * 根据缓存名称获取缓存实例（使用已注册的配置）
     *
     * @param cacheName 缓存名称
     * @param <K>       Key 类型
     * @param <V>       Value 类型
     * @return 缓存实例，如果不存在返回 null
     */
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getCache(String cacheName) {
        CacheConfig config = cacheConfigs.get(cacheName);
        if (config == null) {
            log.warn("Cache config not found for: {}", cacheName);
            return null;
        }
        return getCache(cacheName, (Class<K>) config.getKeyType(), (Class<V>) config.getValueType());
    }

    /**
     * 移除缓存
     *
     * @param cacheName 缓存名称
     */
    public void removeCache(String cacheName) {
        Cache<?, ?> cache = caches.remove(cacheName);
        if (cache != null && defaultCacheManager != null) {
            defaultCacheManager.removeCache(cacheName);
        }
    }

    /**
     * 清空指定缓存的所有数据
     *
     * @param cacheName 缓存名称
     */
    public void clearCache(String cacheName) {
        Cache<?, ?> cache = caches.get(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    /**
     * 清空所有缓存的所有数据
     * 遍历所有已创建的缓存实例并清空它们
     */
    public void clearAllCaches() {
        if (caches.isEmpty()) {
            return;
        }
        for (Cache<?, ?> cache : caches.values()) {
            if (cache != null) {
                try {
                    cache.clear();
                } catch (Exception e) {
                    log.warn("Failed to clear cache: {}", e.getMessage());
                }
            }
        }
        log.info("Cleared all caches, total: {}", caches.size());
    }

    /**
     * 获取 CacheManager 实例
     *
     * @return CacheManager 实例
     */
    public CacheManager getCacheManager() {
        return defaultCacheManager;
    }

    /**
     * 获取已注册的缓存配置
     *
     * @param cacheName 缓存名称
     * @return 缓存配置，如果不存在返回 null
     */
    public CacheConfig getCacheConfig(String cacheName) {
        return cacheConfigs.get(cacheName);
    }

    /**
     * 检查缓存配置是否已注册
     *
     * @param cacheName 缓存名称
     * @return 是否已注册
     */
    public boolean hasCacheConfig(String cacheName) {
        return cacheConfigs.containsKey(cacheName);
    }
}