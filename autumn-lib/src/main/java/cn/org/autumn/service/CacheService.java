package cn.org.autumn.service;

import cn.org.autumn.config.CacheConfig;
import cn.org.autumn.config.ClearHandler;
import cn.org.autumn.config.EhCacheManager;
import cn.org.autumn.model.Invalidation;
import cn.org.autumn.site.LoadFactory;
import cn.org.autumn.utils.RedisUtils;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.ehcache.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 缓存服务工具类
 * 提供便捷的缓存操作方法
 */
@Slf4j
@Component
public class CacheService implements ClearHandler, LoadFactory.Must {

    /**
     * Null 值占位符
     * 用于在缓存中表示 null 值，因为 EhCache 不支持直接存储 null
     */
    private static final Object NULL_PLACEHOLDER = new Object() {
        @Override
        public String toString() {
            return "NULL_PLACEHOLDER";
        }
    };

    @Autowired
    private EhCacheManager ehCacheManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private RedisListenerService redisListenerService;

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
        redisListenerService.initListener();
        // 订阅缓存失效频道
        boolean success = redisListenerService.subscribe(CACHE_INVALIDATION_CHANNEL, (channel, messageBody) -> {
            try {
                // 检查消息体是否为空
                if (messageBody == null || messageBody.trim().isEmpty()) {
                    if (log.isDebugEnabled()) {
                        log.debug("收到空的缓存失效消息，忽略");
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
                        log.debug("JSON解析失败，尝试其他方式解析消息: {}", messageBody.substring(0, Math.min(100, messageBody.length())));
                    }
                    // 如果消息不是以{开头，可能是被序列化的字符串，尝试直接反序列化
                    if (!messageBody.trim().startsWith("{")) {
                        log.warn("缓存失效消息格式异常，不是有效的JSON格式: {}", messageBody.substring(0, Math.min(200, messageBody.length())));
                        return;
                    }
                    throw jsonException; // 重新抛出异常
                }
                // 验证解析后的消息是否有效
                if (invalidation == null) {
                    if (log.isDebugEnabled()) {
                        log.debug("解析后的缓存失效消息为空，忽略");
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
                log.error("处理缓存失效消息失败: {}, 消息内容: {}", e.getMessage(), messageBody != null ? messageBody.substring(0, Math.min(200, messageBody.length())) : "null", e);
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
            String key = message.getKey();
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
                    log.debug("接收变更: name={}, key={}, operation={}", name, key, operation);
                }
            }
        } catch (Exception e) {
            log.error("处理失败: name={}, key={}, operation={}, error={}", message.getCacheName(), message.getKey(), message.getOperation(), e.getMessage(), e);
        }
    }

    /**
     * 发布缓存失效消息到Redis
     *
     * @param name      缓存名称
     * @param key       缓存键
     * @param operation 操作类型
     */
    private void publish(String name, String key, String operation) {
        try {
            if (null == key) {
                return;
            }
            Invalidation message = new Invalidation(name, key, operation);
            message.setInstanceId(redisListenerService.getInstanceId());
            boolean success = redisListenerService.publish(CACHE_INVALIDATION_CHANNEL, message);
            if (log.isDebugEnabled() && success) {
                log.debug("发布失效: name={}, key={}, operation={}", name, key, operation);
            }
        } catch (Exception e) {
            log.error("发布失败: name={}, key={}, operation={}, error={}", name, key, operation, e.getMessage());
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
        return compute(name, key, supplier, keyType, valueType, expireTime, redisTime, expireTimeUnit, null, false);
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
        return compute(name, key, supplier, keyType, valueType, expireTime, redisTime, expireTimeUnit, null, cacheNull);
    }

    @SuppressWarnings("unchecked")
    public <K, V> V compute(K key, Supplier<V> supplier, CacheConfig config) {
        return compute(config.getName(), key, supplier, (Class<K>) config.getKey(), (Class<V>) config.getValue(), config.getExpire(), config.getRedis(), config.getUnit(), config.getMax(), config.isNull());
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
        return compute(name, key, supplier, keyType, valueType, expireTime, redisTime, expireTimeUnit, maxEntries, cacheNull);
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
        return compute(name, key, supplier, String.class, null, null, null, null, null, false);
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
        return compute(name, key, supplier, String.class, valueType, expireTime, redisTime, expireTimeUnit, maxEntries, cacheNull);
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
        return compute(name, key, supplier, keyType, valueType, null, null, null, null, false);
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
        return compute(name, key, supplier, keyType, valueType, expireTime, redisTime, expireTimeUnit, maxEntries, cacheNull);
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
     * @param maxEntries     最大条目数（可选，如果为 null 则使用默认值 1000 或已注册配置的值）
     * @param <K>            Key 类型
     * @param <V>            Value 类型
     * @return 缓存值
     */
    public <K, V> V compute(String name, K key, Supplier<V> supplier, Class<K> keyType, Class<V> valueType, Long expireTime, Long redisTime, TimeUnit expireTimeUnit, Long maxEntries, boolean cacheNull) {
        CacheConfig config = ehCacheManager.getConfig(name);
        // 如果配置不存在，尝试根据传入的类型和过期时间创建配置
        if (config == null) {
            if (keyType != null && valueType != null) {
                // 如果 cacheNull 为 true，需要使用 Object 作为 Value 类型以支持存储 NULL_PLACEHOLDER
                Class<?> finalValueType = cacheNull ? Object.class : valueType;
                // 使用传入的过期时间，如果未指定则使用默认值
                long expire = expireTime != null ? expireTime : 24 * 60;
                TimeUnit unit = expireTimeUnit != null ? expireTimeUnit : TimeUnit.MINUTES;
                // 使用传入的最大条目数，如果未指定则使用默认值 1000
                long max = maxEntries != null ? maxEntries : 10000;
                long redis = redisTime != null ? redisTime : 24 * 60;
                config = CacheConfig.builder().name(name).key(keyType).value(finalValueType).max(max).expire(expire).redis(redis).unit(unit).build();
                config.validate();
                ehCacheManager.register(config);
            } else {
                if (log.isDebugEnabled())
                    log.warn("Cache config not found for: {} and types not specified, returning value from supplier", name);
                return supplier.get();
            }
        } else if (cacheNull && config.getValue() != Object.class) {
            // 如果已存在配置但需要缓存 null，且 Value 类型不是 Object
            // 我们需要使用 Object 类型来获取缓存，这样可以兼容存储 NULL_PLACEHOLDER 和实际值
            // 注意：这里不创建新配置，而是直接使用 Object.class 来获取缓存
            if (log.isDebugEnabled())
                log.debug("Cache config exists for: {} but valueType is not Object, using Object.class for null caching", name);
        }
        // 如果 cacheNull 为 true，使用 Object.class 作为 Value 类型以支持存储 NULL_PLACEHOLDER
        // 否则使用配置中的 Value 类型
        Class<?> cacheValueType = cacheNull ? Object.class : config.getValue();
        @SuppressWarnings("unchecked")
        Cache<Object, Object> cache = (Cache<Object, Object>) ehCacheManager.getCache(name, (Class<K>) config.getKey(), cacheValueType);
        if (cache == null) {
            // 如果缓存不存在，需要创建。如果 cacheNull 为 true，确保使用 Object.class
            // getOrCreateCache 方法已经处理了缓存已存在的情况，这里直接调用即可
            if (cacheNull && config.getValue() != Object.class) {
                // 创建临时配置，使用 Object.class 作为 Value 类型
                // 使用传入的最大条目数，如果未指定则使用配置中的值
                long finalMaxEntries = maxEntries != null ? maxEntries : config.getMax();
                CacheConfig tempConfig = CacheConfig.builder().name(name).key(config.getKey()).value(Object.class).max(finalMaxEntries).expire(expireTime != null ? expireTime : config.getExpire()).unit(expireTimeUnit != null ? expireTimeUnit : config.getUnit()).build();
                tempConfig.validate();
                cache = ehCacheManager.getOrCreate(tempConfig);
            } else {
                cache = ehCacheManager.getOrCreate(config);
            }
            // 如果创建后仍然为 null（理论上不应该发生，因为 getOrCreateCache 会抛出异常或返回缓存）
            if (cache == null) {
                log.error("Failed to create or retrieve cache '{}', returning value from supplier", name);
                return supplier.get();
            }
        }
        Object cached = cache.get(key);
        if (cached == null) {
            // 本地缓存中不存在，检查Redis二级缓存
            V value = getFromRedis(key, config);
            if (value != null) {
                // Redis缓存中存在，写入本地缓存并返回
                cache.put(key, value);
                return value;
            } else if (isRedisEnabled()) {
                // 检查Redis中是否有null占位符
                Object redisValue = getRedisValue(name, key);
                if (redisValue != null && redisValue.toString().equals(NULL_PLACEHOLDER.toString())) {
                    // Redis中有null占位符，写入本地缓存并返回null
                    if (cacheNull) {
                        cache.put(key, NULL_PLACEHOLDER);
                    }
                    return null;
                }
            }
            // Redis缓存中也不存在，调用 supplier 获取值
            value = supplier.get();
            if (value != null) {
                // 值不为 null，同时写入本地缓存和Redis缓存
                cache.put(key, value);
                putToRedis(name, key, value, config);
                // 发布缓存失效消息，通知其他实例清除本地缓存（可选，因为其他实例会从Redis读取）
                publish(name, key.toString(), Invalidation.Operation.PUT);
                return value;
            } else if (cacheNull) {
                // 值为 null 且允许缓存 null，使用占位符缓存
                cache.put(key, NULL_PLACEHOLDER);
                putToRedis(name, key, NULL_PLACEHOLDER, config);
                // 发布缓存失效消息（可选）
                publish(name, key.toString(), Invalidation.Operation.PUT);
                return null;
            } else {
                // 值为 null 但不允许缓存 null，直接返回 null
                return null;
            }
        } else if (cached == NULL_PLACEHOLDER) {
            // 缓存中是 null 占位符，返回 null
            return null;
        } else {
            // 缓存中存在有效值
            @SuppressWarnings("unchecked")
            V value = (V) cached;
            return value;
        }
    }

    /**
     * 获取缓存值
     * 如果本地缓存不存在，则从Redis中读取并缓存到本地
     *
     * @param name 缓存名称
     * @param key  缓存键
     * @param <K>  Key 类型
     * @param <V>  Value 类型
     * @return 缓存值，如果不存在返回 null
     */
    @SuppressWarnings("unchecked")
    public <K, V> V get(String name, K key) {
        CacheConfig config = ehCacheManager.getConfig(name);
        if (config == null) {
            // 如果配置不存在，无法确定类型，直接返回null
            return null;
        }
        // 获取缓存实例
        Cache<Object, Object> cache = (Cache<Object, Object>) ehCacheManager.getCache(name, (Class<K>) config.getKey(), (Class<V>) config.getValue());
        if (cache == null) {
            // 如果缓存不存在，尝试创建
            cache = ehCacheManager.getOrCreate(config);
            if (cache == null) {
                return null;
            }
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
            cache.put(key, value);
            return value;
        } else if (isRedisEnabled()) {
            // 检查Redis中是否有null占位符
            Object redisValue = getRedisValue(name, key);
            if (redisValue != null && redisValue.toString().equals(NULL_PLACEHOLDER.toString())) {
                // Redis中有null占位符，写入本地缓存并返回null
                cache.put(key, NULL_PLACEHOLDER);
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
        Cache<K, V> cache = ehCacheManager.getCache(name, (Class<K>) config.getKey(), (Class<V>) config.getValue());
        if (cache == null) {
            cache = ehCacheManager.getOrCreate(config);
        }
        cache.put(key, value);
        // 同时写入Redis缓存
        putToRedis(name, key, value, config);
        // 发布缓存失效消息，通知其他实例清除本地缓存
        publish(name, key.toString(), Invalidation.Operation.PUT);
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
        publish(name, key != null ? key.toString() : null, Invalidation.Operation.REMOVE);
    }

    /**
     * 清空缓存
     *
     * @param name 缓存名称
     */
    public void clear(String name) {
        ehCacheManager.clear(name);
        // 同时清空Redis缓存
        if (isRedisEnabled()) {
            try {
                String pattern = "cache:" + name + ":*";
                redisTemplate.delete(Objects.requireNonNull(redisTemplate.keys(pattern)));
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
            valueType = java.util.List.class;
        } else if (lowerName.contains("map")) {
            valueType = java.util.Map.class;
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
        // 同时清空所有Redis缓存
        if (isRedisEnabled()) {
            try {
                String pattern = "cache:*";
                redisTemplate.delete(Objects.requireNonNull(redisTemplate.keys(pattern)));
            } catch (Exception e) {
                log.warn("Failed to clear all Redis cache: {}", e.getMessage());
            }
        }
    }

    /**
     * 检查Redis是否启用
     *
     * @return 如果Redis启用返回true，否则返回false
     */
    public boolean isRedisEnabled() {
        return redisUtils.isOpen();
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