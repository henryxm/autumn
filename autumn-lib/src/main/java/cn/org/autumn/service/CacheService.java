package cn.org.autumn.service;

import cn.org.autumn.config.CacheConfig;
import cn.org.autumn.config.ClearHandler;
import cn.org.autumn.config.EhCacheManager;
import cn.org.autumn.model.Invalidation;
import cn.org.autumn.redis.resilience.RedisResilience;
import cn.org.autumn.site.LoadFactory;
import cn.org.autumn.utils.RedisUtils;
import com.google.gson.Gson;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.ehcache.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

/**
 * 缓存服务工具类
 * 提供便捷的缓存操作方法
 */
@Slf4j
@Component
public class CacheService implements ClearHandler, LoadFactory.Must {

    /**
     * Null 值占位符（可序列化）
     * 用于在缓存中表示 null 值，因为 EhCache 不支持直接存储 null
     * 使用静态内部类实现 Serializable，以支持 Redis 序列化
     */
    private static final class NullPlaceholder implements Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public String toString() {
            return "NULL_PLACEHOLDER";
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof NullPlaceholder;
        }

        @Override
        public int hashCode() {
            return NullPlaceholder.class.hashCode();
        }
    }

    private static final NullPlaceholder NULL_PLACEHOLDER = new NullPlaceholder();

    @Autowired
    private EhCacheManager ehCacheManager;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private RedisListenerService redisListenerService;

    @Autowired(required = false)
    private RedisResilience redisResilience;

    @Autowired
    private Gson gson;

    /**
     * Redis Pub/Sub 频道名称
     */
    private static final String CACHE_INVALIDATION_CHANNEL = "cache:invalidation";

    /**
     * 应用上下文刷新完成后订阅缓存失效频道
     */
    @Override
    public void must() {
        if (!isRedisEnabled()) {
            if (log.isDebugEnabled()) {
                log.debug("Redis disabled or bean not wired, skipping cross-instance cache invalidation subscription");
            }
            return;
        }
        redisListenerService.initListener();
        // 订阅缓存失效频道
        boolean success = redisListenerService.subscribe(CACHE_INVALIDATION_CHANNEL, (channel, messageBody) -> {
            try {
                // 检查消息体是否为空
                if (messageBody == null || messageBody.trim().isEmpty()) {
                    if (log.isDebugEnabled()) {
                        log.debug("Empty cache invalidation message received, ignored");
                    }
                    return;
                }
                // 尝试解析JSON消息
                Invalidation invalidation = null;
                try {
                    invalidation = gson.fromJson(messageBody, Invalidation.class);
                } catch (Exception jsonException) {
                    // 如果JSON解析失败，可能是消息被JDK序列化了，尝试其他方式
                    if (log.isDebugEnabled()) {
                        log.debug("JSON parse failed, trying alternate parse: {}", messageBody.substring(0, Math.min(100, messageBody.length())));
                    }
                    // 如果消息不是以{开头，可能是被序列化的字符串，尝试直接反序列化
                    if (!messageBody.trim().startsWith("{")) {
                        log.warn("Invalid cache invalidation message format, not valid JSON: {}", messageBody.substring(0, Math.min(200, messageBody.length())));
                        return;
                    }
                    throw jsonException; // 重新抛出异常
                }
                // 验证解析后的消息是否有效
                if (invalidation == null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Parsed cache invalidation message empty, ignored");
                    }
                    return;
                }
                // 如果是自己发布的消息，忽略
                if (redisListenerService.getInstanceId().equals(invalidation.getInstanceId())) {
                    return;
                }
                // 处理缓存失效消息
                handle(invalidation);
            } catch (Exception e) {
                log.error("Failed to handle cache invalidation message: {}, body: {}", e.getMessage(), messageBody != null ? messageBody.substring(0, Math.min(200, messageBody.length())) : "null", e);
            }
        });
        if (success)
            log.info("Subscribe Redis message");
    }

    /**
     * 处理缓存失效消息
     *
     * @param message 失效消息
     */
    private void handle(Invalidation message) {
        if (message == null) {
            return;
        }
        try {
            String name = message.getCacheName();
            String operation = message.getOperation();
            Object key = convertInvalidationKey(name, message.getKey());
            if (Invalidation.Operation.CLEAR.equals(operation)) {
                // 清空整个缓存
                ehCacheManager.clear(name);
            } else if (Invalidation.Operation.REMOVE.equals(operation) || Invalidation.Operation.PUT.equals(operation)) {
                // 删除指定键
                Cache<Object, Object> cache = ehCacheManager.getCache(name);
                if (cache != null && key != null) {
                    cache.remove(key);
                }
                if (log.isDebugEnabled()) {
                    log.debug("Received change: name={}, key={}, operation={}", name, key, operation);
                }
            }
        } catch (Exception e) {
            log.error("Handle failed: name={}, key={}, operation={}, error={}", message.getCacheName(), message.getKey(), message.getOperation(), e.getMessage(), e);
        }
    }

    /**
     * 发布缓存失效消息到Redis
     *
     * @param name      缓存名称
     * @param key       缓存键
     * @param operation 操作类型
     */
    private void publish(String name, Object key, String operation) {
        try {
            if (!isRedisEnabled()) {
                return;
            }
            if (null == key) {
                return;
            }
            Invalidation message = new Invalidation(name, key, operation);
            message.setInstanceId(redisListenerService.getInstanceId());
            if (redisResilience != null) {
                redisResilience.execute(() -> {
                    boolean ok = redisListenerService.publish(CACHE_INVALIDATION_CHANNEL, message);
                    if (log.isDebugEnabled() && ok) {
                        log.debug("Published invalidation: name={}, key={}, operation={}", name, key, operation);
                    }
                    return null;
                }, () -> null);
            } else {
                boolean success = redisListenerService.publish(CACHE_INVALIDATION_CHANNEL, message);
                if (log.isDebugEnabled() && success) {
                    log.debug("Published invalidation: name={}, key={}, operation={}", name, key, operation);
                }
            }
        } catch (Exception e) {
            log.error("Publish failed: name={}, key={}, operation={}, error={}", name, key, operation, e.getMessage());
        }
    }

    /**
     * 获取缓存值，如果不存在则通过 supplier 获取并缓存
     *
     * @param name     缓存名称
     * @param key      缓存键
     * @param supplier 值提供者（如果缓存不存在时调用）
     * @param <K>      Key 类型
     * @param <V>      Value 类型
     * @return 缓存值
     */
    public <K, V> V compute(String name, K key, Supplier<V> supplier) {
        return compute(name, key, supplier, null, null, null, null, null, false);
    }

    /**
     * 获取缓存值，如果不存在则通过 supplier 获取并缓存
     * 支持指定 Key 和 Value 类型，如果未指定则自动推断
     *
     * @param name      缓存名称
     * @param key       缓存键
     * @param supplier  值提供者（如果缓存不存在时调用）
     * @param keyType   Key 类型（可选，如果为 null 则自动推断）
     * @param valueType Value 类型（可选，如果为 null 则自动推断）
     * @param <K>       Key 类型
     * @param <V>       Value 类型
     * @return 缓存值
     */
    public <K, V> V compute(String name, K key, Supplier<V> supplier, Class<K> keyType, Class<V> valueType) {
        return compute(name, key, supplier, keyType, valueType, null, null, null, false);
    }

    /**
     * 获取缓存值，如果不存在则通过 supplier 获取并缓存
     * 支持指定 Key 和 Value 类型，以及过期时间
     *
     * @param name           缓存名称
     * @param key            缓存键
     * @param supplier       值提供者（如果缓存不存在时调用）
     * @param keyType        Key 类型（可选，如果为 null 则自动推断）
     * @param valueType      Value 类型（可选，如果为 null 则自动推断）
     * @param expireTime     过期时间（可选，如果为 null 则使用默认值或已注册配置的值）
     * @param redisTime      Redis过期时间（可选，如果为 null 则使用默认值或已注册配置的值）
     * @param expireTimeUnit 过期时间单位（可选，如果为 null 则使用默认值或已注册配置的值）
     * @param <K>            Key 类型
     * @param <V>            Value 类型
     * @return 缓存值
     */
    public <K, V> V compute(String name, K key, Supplier<V> supplier, Class<K> keyType, Class<V> valueType, Long expireTime, Long redisTime, TimeUnit expireTimeUnit) {
        return compute(name, key, supplier, keyType, valueType, expireTime, redisTime, expireTimeUnit, null, false, null, null);
    }

    /**
     * 获取缓存值，如果不存在则通过 supplier 获取并缓存
     * 支持指定 Key 和 Value 类型，以及过期时间
     * 支持缓存 null 值，避免重复查询
     *
     * @param name           缓存名称
     * @param key            缓存键
     * @param supplier       值提供者（如果缓存不存在时调用）
     * @param keyType        Key 类型（可选，如果为 null 则自动推断）
     * @param valueType      Value 类型（可选，如果为 null 则自动推断）
     * @param expireTime     过期时间（可选，如果为 null 则使用默认值或已注册配置的值）
     * @param redisTime      Redis过期时间（可选，如果为 null 则使用默认值或已注册配置的值）
     * @param expireTimeUnit 过期时间单位（可选，如果为 null 则使用默认值或已注册配置的值）
     * @param cacheNull      是否缓存 null 值（true：缓存 null，避免重复查询；false：不缓存 null，每次都重新查询）
     * @param <K>            Key 类型
     * @param <V>            Value 类型
     * @return 缓存值
     */
    public <K, V> V compute(String name, K key, Supplier<V> supplier, Class<K> keyType, Class<V> valueType, Long expireTime, Long redisTime, TimeUnit expireTimeUnit, boolean cacheNull) {
        return compute(name, key, supplier, keyType, valueType, expireTime, redisTime, expireTimeUnit, null, cacheNull, null, null);
    }

    /**
     * 获取缓存值，如果不存在则通过 supplier 获取并缓存
     * 这是核心实现方法，直接使用 CacheConfig 配置实例，避免参数的二次转换
     *
     * @param key      缓存键
     * @param supplier 值提供者（如果缓存不存在时调用）
     * @param config   缓存配置
     * @param <K>      Key 类型
     * @param <V>      Value 类型
     * @return 缓存值
     */
    @SuppressWarnings("unchecked")
    public <K, V> V compute(K key, Supplier<V> supplier, CacheConfig config) {
        if (config == null) {
            log.warn("CacheConfig is null, returning value from supplier");
            return supplier.get();
        }
        String name = config.getName();
        boolean cacheNull = config.isNull();
        Cache<Object, Object> cache = getOrCreateLocalCache(config);
        if (cache == null) {
            log.error("Failed to create or retrieve cache '{}', returning value from supplier", name);
            return supplier.get();
        }
        Object cached = cache.get(key);
        if (cached == null) {
            // 本地缓存中不存在，检查Redis二级缓存
            V value = getFromRedis(key, config);
            if (value != null) {
                // Redis缓存中存在，写入本地缓存并返回
                putLocalResilient(cache, name, key, value, config);
                return value;
            } else if (isRedisEnabled()) {
                // 检查Redis中是否有null占位符
                Object redisValue = getRedisValue(name, key);
                if (redisValue != null && redisValue.toString().equals(NULL_PLACEHOLDER.toString())) {
                    // Redis中有null占位符，写入本地缓存并返回null
                    if (cacheNull) {
                        putLocalResilient(cache, name, key, NULL_PLACEHOLDER, config);
                    }
                    return null;
                }
            }
            // Redis缓存中也不存在，调用 supplier 获取值
            value = supplier.get();
            if (value != null) {
                // 值不为 null，同时写入本地缓存和Redis缓存
                putLocalResilient(cache, name, key, value, config);
                putToRedis(name, key, value, config);
                // 发布缓存失效消息，通知其他实例清除本地缓存
                publish(name, key, Invalidation.Operation.PUT);
                return value;
            } else if (cacheNull) {
                // 值为 null 且允许缓存 null，使用占位符缓存
                putLocalResilient(cache, name, key, NULL_PLACEHOLDER, config);
                putToRedis(name, key, NULL_PLACEHOLDER, config);
                // 发布缓存失效消息
                publish(name, key, Invalidation.Operation.PUT);
                return null;
            } else {
                // 值为 null 但不允许缓存 null，直接返回 null
                return null;
            }
        } else if (cached == NULL_PLACEHOLDER) {
            // 缓存中是 null 占位符，返回 null
            return null;
        } else if (isStaleClassLoaderValue(cached, config)) {
            // 堆内残留旧 ClassLoader 实例（FQN 相同、身份不同）：丢弃条目后重载
            log.warn("Cache '{}' hit stale ClassLoader value for key {}; evicting and reloading", name, key);
            try {
                cache.remove(key);
            } catch (Exception ignored) {
            }
            V value = supplier.get();
            if (value != null) {
                putLocalResilient(cache, name, key, value, config);
                putToRedis(name, key, value, config);
                publish(name, key, Invalidation.Operation.PUT);
            }
            return value;
        } else {
            // 缓存中存在有效值
            return (V) cached;
        }
    }

    /**
     * cacheNull 时本地 Ehcache 运行时 value 为 Object，以兼容 NULL_PLACEHOLDER；
     * 注册表仍保留业务声明类型（{@link CacheConfig#getValue()}）。
     */
    private Class<?> resolveLocalValueType(CacheConfig config) {
        if (config == null) {
            return Object.class;
        }
        if (config.isNull()) {
            return Object.class;
        }
        return config.getValue() != null ? config.getValue() : Object.class;
    }

    /**
     * FQN 相同但 Class 身份不同（典型 DevTools 热重启），即使本地缓存以 Object 扩宽也能识别。
     */
    private boolean isStaleClassLoaderValue(Object cached, CacheConfig config) {
        if (cached == null || cached instanceof NullPlaceholder || config == null) {
            return false;
        }
        Class<?> declared = config.getValue();
        if (declared == null || declared == Object.class) {
            return false;
        }
        if (declared.isInstance(cached)) {
            return false;
        }
        return declared.getName().equals(cached.getClass().getName());
    }

    /**
     * 按与 compute/get/put 一致的运行时 value 类型获取或创建本地缓存；
     * 创建后把注册表写回声明配置，避免 Object 扩宽覆盖业务类型。
     */
    @SuppressWarnings("unchecked")
    private Cache<Object, Object> getOrCreateLocalCache(CacheConfig config) {
        if (config == null) {
            return null;
        }
        String name = config.getName();
        Class<?> keyType = config.getKey();
        Class<?> cacheValueType = resolveLocalValueType(config);
        Cache<Object, Object> cache = (Cache<Object, Object>) ehCacheManager.getCache(name, keyType, cacheValueType);
        if (cache != null) {
            restoreDeclaredConfig(config);
            return cache;
        }
        if (config.isNull() && config.getValue() != null && config.getValue() != Object.class) {
            CacheConfig runtimeConfig = config.toBuilder().value(Object.class).build();
            runtimeConfig.validate();
            cache = ehCacheManager.getOrCreate(runtimeConfig);
        } else {
            cache = ehCacheManager.getOrCreate(config);
        }
        restoreDeclaredConfig(config);
        return cache;
    }

    /**
     * getOrCreate 可能用 Object 运行时配置 register；写回调用方声明配置，供 getCache(name)/运维查询。
     */
    private void restoreDeclaredConfig(CacheConfig declared) {
        if (declared == null || declared.getName() == null) {
            return;
        }
        ehCacheManager.register(declared);
    }

    /**
     * 本地 put；遇 EhCache Class 身份不匹配（常见于 DevTools 热重启）时按运行时类型重建后重试，
     * 并恢复声明配置，避免注册表被 Object/运行时 Class 长期覆盖。
     */
    @SuppressWarnings("unchecked")
    private <K> void putLocalResilient(Cache<Object, Object> cache, String name, K key, Object value, CacheConfig config) {
        if (cache == null || key == null || value == null) {
            return;
        }
        try {
            cache.put(key, value);
            return;
        } catch (ClassCastException e) {
            log.warn("Ehcache put ClassCastException for '{}': {}; recreating cache", name, e.getMessage());
        }
        try {
            ehCacheManager.remove(name);
            Class<?> valueType = resolveLocalValueType(config);
            // DevTools：声明/运行时 FQN 相同但 Class 身份不同时，非 Object 路径用新 Class 重建
            if (valueType != Object.class && !(value instanceof NullPlaceholder)
                    && valueType.getName().equals(value.getClass().getName()) && valueType != value.getClass()) {
                valueType = value.getClass();
            }
            CacheConfig fresh = config.toBuilder().value(valueType).build();
            fresh.validate();
            Cache<Object, Object> rebuilt = ehCacheManager.getOrCreate(fresh);
            restoreDeclaredConfig(config);
            if (rebuilt != null) {
                rebuilt.put(key, value);
            }
        } catch (ClassCastException e2) {
            log.warn("Ehcache put still fails after recreate for '{}'; skip local put: {}", name, e2.getMessage());
            restoreDeclaredConfig(config);
            evictRedisKey(name, key);
        } catch (Exception e) {
            log.warn("Ehcache recreate after ClassCastException failed for '{}': {}", name, e.getMessage());
            restoreDeclaredConfig(config);
        }
    }

    private void evictRedisKey(String name, Object key) {
        try {
            if (!isRedisEnabled() || key == null) {
                return;
            }
            redisTemplate.delete(buildRedisKey(name, key));
        } catch (Exception e) {
            log.warn("Failed to evict Redis cache key after ClassCastException: {}", e.getMessage());
        }
    }

    /**
     * 获取缓存值，如果不存在则通过 supplier 获取并缓存
     * 支持通过 keySupplier 函数动态生成 key
     *
     * @param name        缓存名称
     * @param keySupplier key 生成函数（从参数生成 key）
     * @param supplier    值提供者（如果缓存不存在时调用）
     * @param keyType     Key 类型（可选，如果为 null 则自动推断）
     * @param valueType   Value 类型（可选，如果为 null 则自动推断）
     * @param <K>         Key 类型
     * @param <V>         Value 类型
     * @return 缓存值
     */
    public <K, V> V compute(String name, Supplier<K> keySupplier, Supplier<V> supplier, Class<K> keyType, Class<V> valueType) {
        K key = keySupplier.get();
        return compute(name, key, supplier, keyType, valueType, null, null, null, false);
    }

    /**
     * 获取缓存值，如果不存在则通过 supplier 获取并缓存
     * 支持通过 keySupplier 函数动态生成 key，并指定过期时间和最大条目数
     *
     * @param name           缓存名称
     * @param keySupplier    key 生成函数（从参数生成 key）
     * @param supplier       值提供者（如果缓存不存在时调用）
     * @param keyType        Key 类型（可选，如果为 null 则自动推断）
     * @param valueType      Value 类型（可选，如果为 null 则自动推断）
     * @param expireTime     过期时间（可选，如果为 null 则使用默认值或已注册配置的值）
     * @param redisTime      Redis过期时间（可选，如果为 null 则使用默认值或已注册配置的值）
     * @param expireTimeUnit 过期时间单位（可选，如果为 null 则使用默认值或已注册配置的值）
     * @param maxEntries     最大条目数（可选，如果为 null 则使用默认值 1000 或已注册配置的值）
     * @param cacheNull      是否缓存 null 值（true：缓存 null，避免重复查询；false：不缓存 null，每次都重新查询）
     * @param <K>            Key 类型
     * @param <V>            Value 类型
     * @return 缓存值
     */
    public <K, V> V compute(String name, Supplier<K> keySupplier, Supplier<V> supplier, Class<K> keyType, Class<V> valueType, Long expireTime, Long redisTime, TimeUnit expireTimeUnit, Long maxEntries, boolean cacheNull) {
        K key = keySupplier.get();
        return compute(name, key, supplier, keyType, valueType, expireTime, redisTime, expireTimeUnit, maxEntries, cacheNull, null, null);
    }

    /**
     * 获取缓存值，如果不存在则通过 supplier 获取并缓存
     * 支持通过多个参数组合生成字符串 key（使用分隔符连接）
     *
     * @param name     缓存名称
     * @param supplier 值提供者（如果缓存不存在时调用）
     * @param keyParts key 的组成部分（多个参数）
     * @param <V>      Value 类型
     * @return 缓存值
     */
    public <V> V compute(String name, Supplier<V> supplier, Object... keyParts) {
        String key = buildCompositeKey(keyParts);
        return compute(name, key, supplier, String.class, null, null, null, null, null, false, null, null);
    }

    /**
     * 获取缓存值，如果不存在则通过 supplier 获取并缓存
     * 支持通过多个参数组合生成字符串 key（使用分隔符连接），并指定过期时间和最大条目数
     *
     * @param name           缓存名称
     * @param supplier       值提供者（如果缓存不存在时调用）
     * @param valueType      Value 类型（可选，如果为 null 则自动推断）
     * @param expireTime     过期时间（可选，如果为 null 则使用默认值或已注册配置的值）
     * @param redisTime      Redis过期时间（可选，如果为 null 则使用默认值或已注册配置的值）
     * @param expireTimeUnit 过期时间单位（可选，如果为 null 则使用默认值或已注册配置的值）
     * @param maxEntries     最大条目数（可选，如果为 null 则使用默认值 1000 或已注册配置的值）
     * @param cacheNull      是否缓存 null 值（true：缓存 null，避免重复查询；false：不缓存 null，每次都重新查询）
     * @param keyParts       key 的组成部分（多个参数）
     * @param <V>            Value 类型
     * @return 缓存值
     */
    public <V> V compute(String name, Supplier<V> supplier, Class<V> valueType, Long expireTime, Long redisTime, TimeUnit expireTimeUnit, Long maxEntries, boolean cacheNull, Object... keyParts) {
        String key = buildCompositeKey(keyParts);
        return compute(name, key, supplier, String.class, valueType, expireTime, redisTime, expireTimeUnit, maxEntries, cacheNull, null, null);
    }

    /**
     * 获取缓存值，如果不存在则通过 supplier 获取并缓存
     * 支持通过 keyFunction 函数从参数数组生成 key
     *
     * @param name        缓存名称
     * @param keyFunction key 生成函数（从参数数组生成 key）
     * @param supplier    值提供者（如果缓存不存在时调用）
     * @param keyType     Key 类型（可选，如果为 null 则自动推断）
     * @param valueType   Value 类型（可选，如果为 null 则自动推断）
     * @param params      用于生成 key 的参数数组
     * @param <K>         Key 类型
     * @param <V>         Value 类型
     * @return 缓存值
     */
    public <K, V> V compute(String name, Function<Object[], K> keyFunction, Supplier<V> supplier, Class<K> keyType, Class<V> valueType, Object... params) {
        K key = keyFunction.apply(params);
        return compute(name, key, supplier, keyType, valueType, null, null, null, null, false, null, null);
    }

    /**
     * 获取缓存值，如果不存在则通过 supplier 获取并缓存
     * 支持通过 keyFunction 函数从参数数组生成 key，并指定过期时间和最大条目数
     *
     * @param name           缓存名称
     * @param keyFunction    key 生成函数（从参数数组生成 key）
     * @param supplier       值提供者（如果缓存不存在时调用）
     * @param keyType        Key 类型（可选，如果为 null 则自动推断）
     * @param valueType      Value 类型（可选，如果为 null 则自动推断）
     * @param expireTime     过期时间（可选，如果为 null 则使用默认值或已注册配置的值）
     * @param redisTime      Redis过期时间（可选，如果为 null 则使用默认值或已注册配置的值）
     * @param expireTimeUnit 过期时间单位（可选，如果为 null 则使用默认值或已注册配置的值）
     * @param maxEntries     最大条目数（可选，如果为 null 则使用默认值 1000 或已注册配置的值）
     * @param cacheNull      是否缓存 null 值（true：缓存 null，避免重复查询；false：不缓存 null，每次都重新查询）
     * @param params         用于生成 key 的参数数组
     * @param <K>            Key 类型
     * @param <V>            Value 类型
     * @return 缓存值
     */
    public <K, V> V compute(String name, Function<Object[], K> keyFunction, Supplier<V> supplier, Class<K> keyType, Class<V> valueType, Long expireTime, Long redisTime, TimeUnit expireTimeUnit, Long maxEntries, boolean cacheNull, Object... params) {
        K key = keyFunction.apply(params);
        return compute(name, key, supplier, keyType, valueType, expireTime, redisTime, expireTimeUnit, maxEntries, cacheNull, null, null);
    }

    /**
     * 构建复合 key（将多个参数组合成字符串）
     * 使用 ":" 作为分隔符
     *
     * @param keyParts key 的组成部分
     * @return 组合后的 key 字符串
     */
    private String buildCompositeKey(Object... keyParts) {
        if (keyParts == null || keyParts.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keyParts.length; i++) {
            if (i > 0) {
                sb.append(":");
            }
            sb.append(keyParts[i] != null ? keyParts[i].toString() : "null");
        }
        return sb.toString();
    }

    public CacheConfig getConfig(String name) {
        return ehCacheManager.getConfig(name);
    }

    /**
     * 获取缓存值，如果不存在则通过 supplier 获取并缓存
     * 支持指定 Key 和 Value 类型，以及过期时间和最大条目数
     * 将参数包装为 CacheConfig 后委托给核心实现 {@link #compute(Object, Supplier, CacheConfig)}
     *
     * @param name           缓存名称
     * @param key            缓存键
     * @param supplier       值提供者（如果缓存不存在时调用）
     * @param keyType        Key 类型（可选，如果为 null 则自动推断）
     * @param valueType      Value 类型（可选，如果为 null 则自动推断）
     * @param expireTime     过期时间（可选，如果为 null 则使用默认值或已注册配置的值）
     * @param redisTime      Redis过期时间（可选，如果为 null 则使用默认值或已注册配置的值）
     * @param expireTimeUnit 过期时间单位（可选，如果为 null 则使用默认值或已注册配置的值）
     * @param maxEntries     最大条目数（可选，如果为 null 则使用默认值 10000 或已注册配置的值）
     * @param cacheNull      是否缓存 null 值（true：缓存 null，避免重复查询；false：不缓存 null，每次都重新查询）
     * @param persistent     是否启用磁盘持久化（可选，如果为 null 则使用默认值 false 或已注册配置的值）
     * @param path           磁盘持久化路径（可选，如果为 null 则使用已注册配置的值）
     * @param <K>            Key 类型
     * @param <V>            Value 类型
     * @return 缓存值
     */
    public <K, V> V compute(String name, K key, Supplier<V> supplier, Class<K> keyType, Class<V> valueType, Long expireTime, Long redisTime, TimeUnit expireTimeUnit, Long maxEntries, boolean cacheNull, Boolean persistent, String path) {
        CacheConfig config = resolveConfig(name, keyType, valueType, expireTime, redisTime, expireTimeUnit, maxEntries, cacheNull, persistent, path);
        if (config == null) {
            if (log.isDebugEnabled())
                log.warn("Cache config not found for: {} and types not specified, returning value from supplier", name);
            return supplier.get();
        }
        return compute(key, supplier, config);
    }

    /**
     * 根据参数解析或创建缓存配置
     * 先从 ehCacheManager 获取已注册配置，如果不存在且有足够的类型信息则创建新配置
     * 支持 CacheConfig 中的所有配置项，确保不遗漏配置信息
     *
     * @param name           缓存名称
     * @param keyType        Key 类型（可选）
     * @param valueType      Value 类型（可选）
     * @param expireTime     过期时间（可选）
     * @param redisTime      Redis过期时间（可选）
     * @param expireTimeUnit 过期时间单位（可选）
     * @param maxEntries     最大条目数（可选）
     * @param cacheNull      是否缓存 null 值
     * @param persistent     是否启用磁盘持久化（可选，如果为 null 则使用默认值 false 或已注册配置的值）
     * @param path           磁盘持久化路径（可选，如果为 null 则使用已注册配置的值）
     * @return 缓存配置，如果无法创建返回 null
     */
    private CacheConfig resolveConfig(String name, Class<?> keyType, Class<?> valueType,
                                      Long expireTime, Long redisTime, TimeUnit expireTimeUnit,
                                      Long maxEntries, boolean cacheNull,
                                      Boolean persistent, String path) {
        CacheConfig config = ehCacheManager.getConfig(name);
        if (config != null) {
            // Null 一经注册不可翻转，避免 Object/具体类型运行时来回拆建
            if (config.isNull() != cacheNull && log.isDebugEnabled()) {
                log.debug("Ignore cacheNull override for '{}': registered={}, requested={}", name, config.isNull(), cacheNull);
            }
            boolean needsOverride = (persistent != null && config.isPersistent() != persistent)
                    || (path != null && !path.equals(config.getPath()));
            if (needsOverride) {
                CacheConfig.CacheConfigBuilder builder = config.toBuilder();
                if (persistent != null) builder.persistent(persistent);
                if (path != null) builder.path(path);
                CacheConfig overridden = builder.build();
                ehCacheManager.register(overridden);
                return overridden;
            }
            return config;
        }
        // 配置不存在，尝试根据传入的类型和过期时间创建新配置
        if (keyType != null && valueType != null) {
            // 注册表保留业务声明类型；Null 占位扩宽仅在 getOrCreateLocalCache 运行时使用 Object
            long expire = expireTime != null ? expireTime : 24 * 60;
            TimeUnit unit = expireTimeUnit != null ? expireTimeUnit : TimeUnit.MINUTES;
            long max = maxEntries != null ? maxEntries : 10000;
            long redis = redisTime != null ? redisTime : 24 * 60;
            config = CacheConfig.builder()
                    .name(name).key(keyType).value(valueType)
                    .max(max).expire(expire).redis(redis).unit(unit)
                    .Null(cacheNull)
                    .persistent(persistent != null ? persistent : false)
                    .path(path)
                    .build();
            config.validate();
            ehCacheManager.register(config);
            return config;
        }
        return null;
    }

    public <K, V> V get(String name, K key) {
        CacheConfig config = ehCacheManager.getConfig(name);
        return get(config, key);
    }

    /**
     * 获取缓存值
     * 如果本地缓存不存在，则从Redis中读取并缓存到本地
     *
     * @param config 缓存名称
     * @param key    缓存键
     * @param <K>    Key 类型
     * @param <V>    Value 类型
     * @return 缓存值，如果不存在返回 null
     */
    @SuppressWarnings("unchecked")
    public <K, V> V get(CacheConfig config, K key) {
        if (null == config)
            return null;
        String name = config.getName();
        Cache<Object, Object> cache = getOrCreateLocalCache(config);
        if (cache == null) {
            return null;
        }
        // 先从本地缓存获取
        Object cached = cache.get(key);
        if (cached != null) {
            // 本地缓存中存在
            if (cached == NULL_PLACEHOLDER) {
                // 缓存中是 null 占位符，返回 null
                return null;
            } else {
                // 缓存中存在有效值
                return (V) cached;
            }
        }
        // 本地缓存中不存在，检查Redis二级缓存
        V value = getFromRedis(key, config);
        if (value != null) {
            // Redis缓存中存在，写入本地缓存并返回
            putLocalResilient(cache, name, key, value, config);
            return value;
        } else if (isRedisEnabled()) {
            // 检查Redis中是否有null占位符
            Object redisValue = getRedisValue(name, key);
            if (redisValue != null && redisValue.toString().equals(NULL_PLACEHOLDER.toString())) {
                // Redis中有null占位符，写入本地缓存并返回null
                putLocalResilient(cache, name, key, NULL_PLACEHOLDER, config);
                return null;
            }
        }
        // Redis缓存中也不存在，返回null
        return null;
    }

    /**
     * 设置缓存值
     *
     * @param config 缓存配置
     * @param key    缓存键
     * @param value  缓存值
     * @param <K>    Key 类型
     * @param <V>    Value 类型
     */
    @SuppressWarnings("unchecked")
    public <K, V> void put(CacheConfig config, K key, V value) {
        if (key == null || value == null || null == config) {
            return;
        }
        String name = config.getName();
        Cache<Object, Object> cache = getOrCreateLocalCache(config);
        putLocalResilient(cache, name, key, value, config);
        // 同时写入Redis缓存
        putToRedis(name, key, value, config);
        // 发布缓存失效消息，通知其他实例清除本地缓存
        publish(name, key, Invalidation.Operation.PUT);
    }

    /**
     * 移除缓存值
     *
     * @param name 缓存名称
     * @param key  缓存键
     * @param <K>  Key 类型
     */
    public <K> void remove(String name, K key) {
        Cache<K, ?> cache = ehCacheManager.getCache(name);
        if (cache != null) {
            cache.remove(key);
        }
        // 同时移除Redis缓存
        if (isRedisEnabled()) {
            try {
                String redisKey = buildRedisKey(name, key);
                redisTemplate.delete(redisKey);
            } catch (Exception e) {
                log.warn("Failed to remove value from Redis cache: {}", e.getMessage());
            }
        }
        // 发布缓存失效消息，通知其他实例清除本地缓存
        publish(name, key, Invalidation.Operation.REMOVE);
    }

    /**
     * 清空缓存
     *
     * @param name 缓存名称
     */
    public void clear(String name) {
        ehCacheManager.clear(name);
        // 同时清空Redis缓存（SCAN 分批删，禁止 KEYS）
        if (isRedisEnabled()) {
            try {
                deleteByScan("cache:" + name + ":*");
            } catch (Exception e) {
                log.warn("Failed to clear Redis cache: {}", e.getMessage());
            }
        }
        // 发布缓存失效消息，通知其他实例清空本地缓存
        publish(name, null, Invalidation.Operation.CLEAR);
    }

    /**
     * 根据缓存名称创建默认配置
     * 根据常见的缓存命名模式推断 Key/Value 类型
     *
     * @param name 缓存名称
     * @return 默认缓存配置
     */
    private CacheConfig createDefault(String name) {
        // 根据缓存名称推断类型
        Class<?> keyType = String.class; // 默认 Key 类型为 String
        Class<?> valueType = Object.class; // 默认 Value 类型为 Object
        // 根据缓存名称后缀推断 Value 类型
        String lowerName = name.toLowerCase();
        if (lowerName.endsWith("cache") || lowerName.endsWith("caches")) {
            // 通用缓存，使用 Object 类型
        } else if (lowerName.contains("boolean") || lowerName.contains("bool")) {
            valueType = Boolean.class;
        } else if (lowerName.contains("string") || lowerName.contains("str")) {
            valueType = String.class;
        } else if (lowerName.contains("integer") || lowerName.contains("int")) {
            valueType = Integer.class;
        } else if (lowerName.contains("long")) {
            valueType = Long.class;
        } else if (lowerName.contains("list")) {
            valueType = List.class;
        } else if (lowerName.contains("map")) {
            valueType = Map.class;
        }
        // 创建默认配置
        CacheConfig config = CacheConfig.builder().name(name).key(keyType).value(valueType).build();
        config.validate();
        return config;
    }

    /**
     * 清空所有缓存
     * 实现 ClearHandler 接口，用于系统级别的缓存清理
     */
    @Override
    public void clear() {
        ehCacheManager.clear();
        // 同时清空所有Redis缓存（SCAN 分批删，禁止 KEYS）
        if (isRedisEnabled()) {
            try {
                deleteByScan("cache:*");
            } catch (Exception e) {
                log.warn("Failed to clear all Redis cache: {}", e.getMessage());
            }
        }
    }

    /**
     * SCAN 分批删除匹配键，避免 KEYS 在大键空间下阻塞 Redis。
     */
    private void deleteByScan(String pattern) {
        if (redisTemplate == null || pattern == null) {
            return;
        }
        List<String> batch = new ArrayList<>(100);
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(200).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                batch.add(cursor.next());
                if (batch.size() >= 100) {
                    redisTemplate.delete(batch);
                    batch.clear();
                }
            }
        }
        if (!batch.isEmpty()) {
            redisTemplate.delete(batch);
        }
    }

    /**
     * 检查Redis是否启用
     *
     * @return 如果Redis启用返回true，否则返回false
     */
    public boolean isRedisEnabled() {
        return redisUtils != null && redisUtils.isOpen() && redisTemplate != null;
    }

    /**
     * 生成Redis缓存key
     *
     * @param name 缓存名称
     * @param key  缓存键
     * @return Redis key字符串
     */
    private String buildRedisKey(String name, Object key) {
        return "cache:" + name + ":" + (key != null ? key.toString() : "null");
    }

    /**
     * 将失效消息中的 key 转换为缓存定义的 Key 类型，避免消息反序列化后类型不匹配。
     */
    private Object convertInvalidationKey(String cacheName, Object rawKey) {
        if (rawKey == null || cacheName == null) {
            return rawKey;
        }
        CacheConfig config = ehCacheManager.getConfig(cacheName);
        if (config == null || config.getKey() == null) {
            return rawKey;
        }
        Class<?> keyType = toWrapperType(config.getKey());
        if (keyType.isInstance(rawKey)) {
            return rawKey;
        }
        try {
            if (String.class == keyType) {
                return String.valueOf(rawKey);
            }
            if (rawKey instanceof Number) {
                Number number = (Number) rawKey;
                if (Long.class == keyType) return number.longValue();
                if (Integer.class == keyType) return number.intValue();
                if (Short.class == keyType) return number.shortValue();
                if (Byte.class == keyType) return number.byteValue();
                if (Double.class == keyType) return number.doubleValue();
                if (Float.class == keyType) return number.floatValue();
            }
            String stringValue = String.valueOf(rawKey);
            if (Long.class == keyType) return new BigDecimal(stringValue).longValue();
            if (Integer.class == keyType) return new BigDecimal(stringValue).intValue();
            if (Short.class == keyType) return new BigDecimal(stringValue).shortValue();
            if (Byte.class == keyType) return new BigDecimal(stringValue).byteValue();
            if (Double.class == keyType) return Double.parseDouble(stringValue);
            if (Float.class == keyType) return Float.parseFloat(stringValue);
            if (Boolean.class == keyType) return Boolean.parseBoolean(stringValue);
            if (Character.class == keyType && !stringValue.isEmpty()) return stringValue.charAt(0);
        } catch (Exception e) {
            log.warn("Cache key type conversion failed: cache={}, key={}, expectedType={}, error={}", cacheName, rawKey, keyType.getName(), e.getMessage());
        }
        return rawKey;
    }

    private Class<?> toWrapperType(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == short.class) return Short.class;
        if (type == byte.class) return Byte.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        if (type == boolean.class) return Boolean.class;
        if (type == char.class) return Character.class;
        return type;
    }

    /**
     * 从Redis获取缓存值
     *
     * @param key    缓存键
     * @param config 缓存配置
     * @param <K>    Key类型
     * @param <V>    Value类型
     * @return 缓存值，如果不存在返回null
     */
    @SuppressWarnings("unchecked")
    private <K, V> V getFromRedis(K key, CacheConfig config) {
        try {
            if (!isRedisEnabled()) {
                return null;
            }
            String redisKey = buildRedisKey(config.getName(), key);
            Object value = redisTemplate.opsForValue().get(redisKey);
            if (value == null) {
                return null;
            }
            // 如果是null占位符，返回null
            if (value == NULL_PLACEHOLDER || value.toString().equals(NULL_PLACEHOLDER.toString())) {
                return null;
            }
            return (V) value;
        } catch (Exception e) {
            log.warn("Failed to get value from Redis cache: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从Redis获取原始值（包括null占位符）
     *
     * @param name 缓存名称
     * @param key  缓存键
     * @return Redis中的值，如果不存在返回null
     */
    private Object getRedisValue(String name, Object key) {
        try {
            if (!isRedisEnabled()) {
                return null;
            }
            String redisKey = buildRedisKey(name, key);
            return redisTemplate.opsForValue().get(redisKey);
        } catch (Exception e) {
            log.warn("Failed to get value from Redis cache: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 将值写入Redis缓存
     *
     * @param name   缓存名称
     * @param key    缓存键
     * @param value  缓存值
     * @param config 缓存配置
     * @param <K>    Key类型
     * @param <V>    Value类型
     */
    private <K, V> void putToRedis(String name, K key, V value, CacheConfig config) {
        try {
            if (!isRedisEnabled()) {
                return;
            }
            String redisKey = buildRedisKey(name, key);
            // 直接使用redisTime作为时间值，timeUnit作为时间单位
            redisTemplate.opsForValue().set(redisKey, value, config.getRedis(), config.getUnit());
            if (log.isDebugEnabled()) {
                log.debug("Put value to Redis cache: key={}, expireTime={}, expireTimeUnit={}", redisKey, config.getRedis(), config.getUnit());
            }
        } catch (Exception e) {
            log.warn("Failed to put value to Redis cache: {}", e.getMessage());
        }
    }
}
