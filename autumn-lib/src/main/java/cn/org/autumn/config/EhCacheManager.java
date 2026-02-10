package cn.org.autumn.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.PersistentCacheManager;
import org.ehcache.config.ResourcePools;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.expiry.ExpiryPolicy;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.impl.serialization.PlainJavaSerializer;

import java.io.File;
import java.time.Duration;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * EhCache 缓存管理器
 * 提供统一的缓存管理功能，支持动态创建和管理多个缓存实例
 */
@Slf4j
@Component
public class EhCacheManager implements ClearHandler {

    /**
     * 内存缓存管理器（不支持磁盘持久化）
     */
    private final CacheManager manager = CacheManagerBuilder.newCacheManagerBuilder().build(true);

    /**
     * 持久化缓存管理器映射，按磁盘路径分组
     * 每个不同的 path 对应一个独立的 PersistentCacheManager
     */
    private final ConcurrentHashMap<String, PersistentCacheManager> persistentManagers = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, CacheConfig> configs = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Cache<?, ?>> caches = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public void register(CacheConfig config) {
        if (config == null || config.getName() == null) {
            throw new IllegalArgumentException("CacheConfig cannot be null and cacheName is required");
        }
        configs.put(config.getName(), config);
    }

    /**
     * 根据配置创建或获取缓存实例
     * 使用锁机制防止并发创建时的竞态条件
     *
     * @param config 缓存配置
     * @param <K>    Key 类型
     * @param <V>    Value 类型
     * @return 缓存实例
     */
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getOrCreate(CacheConfig config) {
        String name = config.getName();
        // 第一次检查：快速路径，如果缓存已存在直接返回
        Cache<?, ?> existing = caches.get(name);
        if (existing != null) {
            return (Cache<K, V>) existing;
        }
        // 获取或创建该缓存名称对应的锁
        ReentrantLock lock = locks.computeIfAbsent(name, k -> new ReentrantLock());
        lock.lock();
        try {
            // 双重检查：在获取锁后再次检查缓存是否已存在（可能被其他线程创建）
            existing = caches.get(name);
            if (existing != null) {
                return (Cache<K, V>) existing;
            }
            // 尝试从 CacheManager 获取已存在的缓存（可能在其他地方已创建）
            try {
                Cache<K, V> cache = manager.getCache(name, (Class<K>) config.getKey(), (Class<V>) config.getValue());
                if (cache != null) {
                    // 缓存已存在，添加到本地映射中
                    caches.put(name, cache);
                    // 确保配置已注册
                    if (!configs.containsKey(name)) {
                        register(config);
                    }
                    return cache;
                }
            } catch (Exception e) {
                log.error("Cache '{}' not found in CacheManager, will create new one", name);
            }
            // 注册配置
            register(config);
            // 创建缓存配置
            // 使用新的 ExpiryPolicyBuilder API 替代已废弃的 Expirations
            // 将 TimeUnit 转换为 java.time.Duration
            Duration duration = convert(config.getExpire(), config.getUnit());
            ExpiryPolicy<Object, Object> policy = ExpiryPolicyBuilder.timeToLiveExpiration(duration);
            CacheConfigurationBuilder<K, V> builder = CacheConfigurationBuilder.newCacheConfigurationBuilder((Class<K>) config.getKey(), (Class<V>) config.getValue(), ResourcePoolsBuilder.heap(config.getMax())).withExpiry(policy);

            Cache<K, V> cache;
            // 如果启用磁盘持久化
            if (config.isPersistent() && StringUtils.isNotBlank(config.getPath())) {
                // 配置堆内存 + 磁盘持久化的资源池
                ResourcePools pools = ResourcePoolsBuilder.newResourcePoolsBuilder()
                        .heap(config.getMax(), EntryUnit.ENTRIES)
                        .disk(1000, MemoryUnit.MB, true)
                        .build();
                builder = builder.withResourcePools(pools);
                // 获取或创建该路径对应的持久化 CacheManager
                PersistentCacheManager persistentMgr = getOrCreatePersistentManager(config.getPath());
                cache = persistentMgr.createCache(name, builder.build());
                if (log.isDebugEnabled())
                    log.debug("Created persistent cache '{}' with disk path: {}", name, config.getPath());
            } else {
                // 非持久化缓存，使用内存缓存管理器
                if (!manager.getStatus().toString().equals("AVAILABLE")) {
                    manager.init();
                }
                cache = manager.createCache(name, builder.build());
            }
            caches.put(name, cache);
            return cache;
        } catch (Exception e) {
            log.error("创建失败:{}", e.getMessage());
            throw e;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 根据缓存名称获取缓存实例
     *
     * @param name      缓存名称
     * @param keyType   Key 类型
     * @param valueType Value 类型
     * @param <K>       Key 类型
     * @param <V>       Value 类型
     * @return 缓存实例，如果不存在返回 null
     */
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getCache(String name, Class<K> keyType, Class<V> valueType) {
        Cache<?, ?> cache = caches.get(name);
        if (cache != null) {
            return (Cache<K, V>) cache;
        }
        // 先检查是否是持久化缓存，从对应的 PersistentCacheManager 中查找
        CacheConfig config = configs.get(name);
        if (config != null && config.isPersistent() && StringUtils.isNotBlank(config.getPath())) {
            PersistentCacheManager pcm = persistentManagers.get(config.getPath());
            if (pcm != null) {
                try {
                    Cache<K, V> pcmCache = pcm.getCache(name, keyType, valueType);
                    if (pcmCache != null) {
                        caches.put(name, pcmCache);
                        return pcmCache;
                    }
                } catch (Exception e) {
                    log.debug("Cache {} not found in PersistentCacheManager", name);
                }
            }
        }
        // 尝试从内存 CacheManager 获取
        if (manager != null) {
            try {
                Cache<K, V> managerCache = manager.getCache(name, keyType, valueType);
                if (managerCache != null) {
                    caches.put(name, managerCache);
                    return managerCache;
                }
            } catch (Exception e) {
                log.debug("Cache {} not found in CacheManager", name);
            }
        }
        return null;
    }

    /**
     * 根据缓存名称获取缓存实例（使用已注册的配置）
     *
     * @param name 缓存名称
     * @param <K>  Key 类型
     * @param <V>  Value 类型
     * @return 缓存实例，如果不存在返回 null
     */
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getCache(String name) {
        CacheConfig config = configs.get(name);
        if (config == null) {
            if (log.isDebugEnabled())
                log.debug("Cache config not found for: {}", name);
            return null;
        }
        return getCache(name, (Class<K>) config.getKey(), (Class<V>) config.getValue());
    }

    /**
     * 移除缓存
     *
     * @param name 缓存名称
     */
    public void remove(String name) {
        Cache<?, ?> cache = caches.remove(name);
        if (cache == null) {
            return;
        }
        // 先尝试从配置中判断是否是持久化缓存
        CacheConfig config = configs.get(name);
        if (config != null && config.isPersistent() && StringUtils.isNotBlank(config.getPath())) {
            // 从对应的持久化 CacheManager 中移除
            PersistentCacheManager pcm = persistentManagers.get(config.getPath());
            if (pcm != null) {
                try {
                    pcm.removeCache(name);
                } catch (Exception e) {
                    log.warn("Failed to remove persistent cache '{}': {}", name, e.getMessage());
                }
            }
        } else if (manager != null) {
            try {
                manager.removeCache(name);
            } catch (Exception e) {
                log.warn("Failed to remove cache '{}': {}", name, e.getMessage());
            }
        }
    }

    /**
     * 清空指定缓存的所有数据
     *
     * @param name 缓存名称
     */
    public void clear(String name) {
        Cache<?, ?> cache = caches.get(name);
        if (cache != null) {
            cache.clear();
        }
    }

    /**
     * 清空所有缓存的所有数据
     * 遍历所有已创建的缓存实例并清空它们
     */
    public void clear() {
        locks.clear();
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
        if (log.isDebugEnabled())
            log.debug("Cleared all caches, total: {}", caches.size());
    }

    /**
     * 获取 CacheManager 实例
     *
     * @return CacheManager 实例
     */
    public CacheManager getManager() {
        return manager;
    }

    /**
     * 获取已注册的缓存配置
     *
     * @param name 缓存名称
     * @return 缓存配置，如果不存在返回 null
     */
    public CacheConfig getConfig(String name) {
        if (StringUtils.isBlank(name))
            return null;
        return configs.get(name);
    }

    /**
     * 检查缓存配置是否已注册
     *
     * @param name 缓存名称
     * @return 是否已注册
     */
    public boolean hasConfig(String name) {
        if (StringUtils.isBlank(name))
            return false;
        return configs.containsKey(name);
    }

    /**
     * 获取所有已注册的缓存名称
     *
     * @return 缓存名称集合
     */
    public Set<String> getAllNames() {
        return configs.keySet();
    }

    /**
     * 获取所有已创建的缓存实例名称
     *
     * @return 缓存实例名称集合
     */
    public Set<String> getAllInstanceNames() {
        return caches.keySet();
    }

    /**
     * 获取或创建指定路径对应的持久化 CacheManager
     * 同一个路径共享同一个 PersistentCacheManager
     *
     * @param path 磁盘持久化路径
     * @return PersistentCacheManager 实例
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private PersistentCacheManager getOrCreatePersistentManager(String path) {
        return persistentManagers.computeIfAbsent(path, p -> {
            File diskDir = new File(p);
            if (!diskDir.exists()) {
                boolean created = diskDir.mkdirs();
                if (created && log.isDebugEnabled()) {
                    log.debug("Created disk persistence directory: {}", p);
                }
            }
            // 注册 Object.class 的序列化器，解决磁盘持久化时 Object 类型无默认序列化器的问题
            // 当 cacheNull=true 时 value 类型会被替换为 Object.class 以支持存储 NULL_PLACEHOLDER
            // PlainJavaSerializer 使用 Java 标准序列化，要求实际存储的对象实现 Serializable 接口
            // 注意：withSerializer 使用 raw type 会擦除泛型信息，因此需要显式强转 build 的返回值
            PersistentCacheManager pcm = (PersistentCacheManager) CacheManagerBuilder.newCacheManagerBuilder()
                    .with(CacheManagerBuilder.persistence(diskDir))
                    .withSerializer(Object.class, (Class) PlainJavaSerializer.class)
                    .build(true);
            if (log.isDebugEnabled())
                log.debug("Created PersistentCacheManager for path: {}", p);
            return pcm;
        });
    }

    /**
     * 将 TimeUnit 转换为 java.time.Duration
     *
     * @param amount 时间数量
     * @param unit   时间单位
     * @return java.time.Duration
     */
    private Duration convert(long amount, TimeUnit unit) {
        switch (unit) {
            case NANOSECONDS:
                return Duration.ofNanos(amount);
            case MICROSECONDS:
                return Duration.ofNanos(amount * 1000);
            case MILLISECONDS:
                return Duration.ofMillis(amount);
            case SECONDS:
                return Duration.ofSeconds(amount);
            case MINUTES:
                return Duration.ofMinutes(amount);
            case HOURS:
                return Duration.ofHours(amount);
            case DAYS:
                return Duration.ofDays(amount);
            default:
                return Duration.ofMinutes(amount);
        }
    }
}