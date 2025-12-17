package cn.org.autumn.service;

import cn.org.autumn.config.CacheConfig;
import cn.org.autumn.config.ClearHandler;
import cn.org.autumn.config.EhCacheManager;
import lombok.extern.slf4j.Slf4j;
import org.ehcache.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 缓存服务工具类
 * 提供便捷的缓存操作方法
 */
@Slf4j
@Component
public class CacheService implements ClearHandler {

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

    @Autowired(required = false)
    private EhCacheManager ehCacheManager;

    /**
     * 获取缓存值，如果不存在则通过 supplier 获取并缓存
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @param supplier  值提供者（如果缓存不存在时调用）
     * @param <K>       Key 类型
     * @param <V>       Value 类型
     * @return 缓存值
     */
    public <K, V> V compute(String cacheName, K key, Supplier<V> supplier) {
        return compute(cacheName, key, supplier, null, null, null, null, null, false);
    }

    /**
     * 获取缓存值，如果不存在则通过 supplier 获取并缓存
     * 支持指定 Key 和 Value 类型，如果未指定则自动推断
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @param supplier  值提供者（如果缓存不存在时调用）
     * @param keyType   Key 类型（可选，如果为 null 则自动推断）
     * @param valueType Value 类型（可选，如果为 null 则自动推断）
     * @param <K>       Key 类型
     * @param <V>       Value 类型
     * @return 缓存值
     */
    public <K, V> V compute(String cacheName, K key, Supplier<V> supplier, Class<K> keyType, Class<V> valueType) {
        return compute(cacheName, key, supplier, keyType, valueType, null, null, null, false);
    }

    /**
     * 获取缓存值，如果不存在则通过 supplier 获取并缓存
     * 支持指定 Key 和 Value 类型，以及过期时间
     *
     * @param cacheName      缓存名称
     * @param key            缓存键
     * @param supplier       值提供者（如果缓存不存在时调用）
     * @param keyType        Key 类型（可选，如果为 null 则自动推断）
     * @param valueType      Value 类型（可选，如果为 null 则自动推断）
     * @param expireTime     过期时间（可选，如果为 null 则使用默认值或已注册配置的值）
     * @param expireTimeUnit 过期时间单位（可选，如果为 null 则使用默认值或已注册配置的值）
     * @param <K>            Key 类型
     * @param <V>            Value 类型
     * @return 缓存值
     */
    public <K, V> V compute(String cacheName, K key, Supplier<V> supplier, Class<K> keyType, Class<V> valueType, Long expireTime, TimeUnit expireTimeUnit) {
        return compute(cacheName, key, supplier, keyType, valueType, expireTime, expireTimeUnit, null, false);
    }

    /**
     * 获取缓存值，如果不存在则通过 supplier 获取并缓存
     * 支持指定 Key 和 Value 类型，以及过期时间
     * 支持缓存 null 值，避免重复查询
     *
     * @param cacheName      缓存名称
     * @param key            缓存键
     * @param supplier       值提供者（如果缓存不存在时调用）
     * @param keyType        Key 类型（可选，如果为 null 则自动推断）
     * @param valueType      Value 类型（可选，如果为 null 则自动推断）
     * @param expireTime     过期时间（可选，如果为 null 则使用默认值或已注册配置的值）
     * @param expireTimeUnit 过期时间单位（可选，如果为 null 则使用默认值或已注册配置的值）
     * @param cacheNull      是否缓存 null 值（true：缓存 null，避免重复查询；false：不缓存 null，每次都重新查询）
     * @param <K>            Key 类型
     * @param <V>            Value 类型
     * @return 缓存值
     */
    public <K, V> V compute(String cacheName, K key, Supplier<V> supplier, Class<K> keyType, Class<V> valueType, Long expireTime, TimeUnit expireTimeUnit, boolean cacheNull) {
        return compute(cacheName, key, supplier, keyType, valueType, expireTime, expireTimeUnit, null, cacheNull);
    }

    @SuppressWarnings("unchecked")
    public <K, V> V compute(K key, Supplier<V> supplier, CacheConfig config) {
        return compute(config.getCacheName(), key, supplier, (Class<K>) config.getKeyType(), (Class<V>) config.getValueType(), config.getExpireTime(), config.getExpireTimeUnit(), config.getMaxEntries(), config.isCacheNull());
    }

    /**
     * 获取缓存值，如果不存在则通过 supplier 获取并缓存
     * 支持通过 keySupplier 函数动态生成 key
     *
     * @param cacheName   缓存名称
     * @param keySupplier key 生成函数（从参数生成 key）
     * @param supplier    值提供者（如果缓存不存在时调用）
     * @param keyType     Key 类型（可选，如果为 null 则自动推断）
     * @param valueType   Value 类型（可选，如果为 null 则自动推断）
     * @param <K>         Key 类型
     * @param <V>         Value 类型
     * @return 缓存值
     */
    public <K, V> V compute(String cacheName, Supplier<K> keySupplier, Supplier<V> supplier, Class<K> keyType, Class<V> valueType) {
        K key = keySupplier.get();
        return compute(cacheName, key, supplier, keyType, valueType, null, null, null, false);
    }

    /**
     * 获取缓存值，如果不存在则通过 supplier 获取并缓存
     * 支持通过 keySupplier 函数动态生成 key，并指定过期时间和最大条目数
     *
     * @param cacheName      缓存名称
     * @param keySupplier    key 生成函数（从参数生成 key）
     * @param supplier       值提供者（如果缓存不存在时调用）
     * @param keyType        Key 类型（可选，如果为 null 则自动推断）
     * @param valueType      Value 类型（可选，如果为 null 则自动推断）
     * @param expireTime     过期时间（可选，如果为 null 则使用默认值或已注册配置的值）
     * @param expireTimeUnit 过期时间单位（可选，如果为 null 则使用默认值或已注册配置的值）
     * @param maxEntries     最大条目数（可选，如果为 null 则使用默认值 1000 或已注册配置的值）
     * @param cacheNull      是否缓存 null 值（true：缓存 null，避免重复查询；false：不缓存 null，每次都重新查询）
     * @param <K>            Key 类型
     * @param <V>            Value 类型
     * @return 缓存值
     */
    public <K, V> V compute(String cacheName, Supplier<K> keySupplier, Supplier<V> supplier, Class<K> keyType, Class<V> valueType, Long expireTime, TimeUnit expireTimeUnit, Long maxEntries, boolean cacheNull) {
        K key = keySupplier.get();
        return compute(cacheName, key, supplier, keyType, valueType, expireTime, expireTimeUnit, maxEntries, cacheNull);
    }

    /**
     * 获取缓存值，如果不存在则通过 supplier 获取并缓存
     * 支持通过多个参数组合生成字符串 key（使用分隔符连接）
     *
     * @param cacheName   缓存名称
     * @param supplier    值提供者（如果缓存不存在时调用）
     * @param keyParts    key 的组成部分（多个参数）
     * @param <V>         Value 类型
     * @return 缓存值
     */
    public <V> V compute(String cacheName, Supplier<V> supplier, Object... keyParts) {
        String key = buildCompositeKey(keyParts);
        return compute(cacheName, key, supplier, String.class, null, null, null, null, false);
    }

    /**
     * 获取缓存值，如果不存在则通过 supplier 获取并缓存
     * 支持通过多个参数组合生成字符串 key（使用分隔符连接），并指定过期时间和最大条目数
     *
     * @param cacheName      缓存名称
     * @param supplier       值提供者（如果缓存不存在时调用）
     * @param valueType      Value 类型（可选，如果为 null 则自动推断）
     * @param expireTime     过期时间（可选，如果为 null 则使用默认值或已注册配置的值）
     * @param expireTimeUnit 过期时间单位（可选，如果为 null 则使用默认值或已注册配置的值）
     * @param maxEntries     最大条目数（可选，如果为 null 则使用默认值 1000 或已注册配置的值）
     * @param cacheNull      是否缓存 null 值（true：缓存 null，避免重复查询；false：不缓存 null，每次都重新查询）
     * @param keyParts       key 的组成部分（多个参数）
     * @param <V>            Value 类型
     * @return 缓存值
     */
    public <V> V compute(String cacheName, Supplier<V> supplier, Class<V> valueType, Long expireTime, TimeUnit expireTimeUnit, Long maxEntries, boolean cacheNull, Object... keyParts) {
        String key = buildCompositeKey(keyParts);
        return compute(cacheName, key, supplier, String.class, valueType, expireTime, expireTimeUnit, maxEntries, cacheNull);
    }

    /**
     * 获取缓存值，如果不存在则通过 supplier 获取并缓存
     * 支持通过 keyFunction 函数从参数数组生成 key
     *
     * @param cacheName   缓存名称
     * @param keyFunction key 生成函数（从参数数组生成 key）
     * @param supplier    值提供者（如果缓存不存在时调用）
     * @param keyType     Key 类型（可选，如果为 null 则自动推断）
     * @param valueType   Value 类型（可选，如果为 null 则自动推断）
     * @param params      用于生成 key 的参数数组
     * @param <K>         Key 类型
     * @param <V>         Value 类型
     * @return 缓存值
     */
    public <K, V> V compute(String cacheName, Function<Object[], K> keyFunction, Supplier<V> supplier, Class<K> keyType, Class<V> valueType, Object... params) {
        K key = keyFunction.apply(params);
        return compute(cacheName, key, supplier, keyType, valueType, null, null, null, false);
    }

    /**
     * 获取缓存值，如果不存在则通过 supplier 获取并缓存
     * 支持通过 keyFunction 函数从参数数组生成 key，并指定过期时间和最大条目数
     *
     * @param cacheName      缓存名称
     * @param keyFunction    key 生成函数（从参数数组生成 key）
     * @param supplier       值提供者（如果缓存不存在时调用）
     * @param keyType        Key 类型（可选，如果为 null 则自动推断）
     * @param valueType      Value 类型（可选，如果为 null 则自动推断）
     * @param expireTime     过期时间（可选，如果为 null 则使用默认值或已注册配置的值）
     * @param expireTimeUnit 过期时间单位（可选，如果为 null 则使用默认值或已注册配置的值）
     * @param maxEntries     最大条目数（可选，如果为 null 则使用默认值 1000 或已注册配置的值）
     * @param cacheNull      是否缓存 null 值（true：缓存 null，避免重复查询；false：不缓存 null，每次都重新查询）
     * @param params         用于生成 key 的参数数组
     * @param <K>            Key 类型
     * @param <V>            Value 类型
     * @return 缓存值
     */
    public <K, V> V compute(String cacheName, Function<Object[], K> keyFunction, Supplier<V> supplier, Class<K> keyType, Class<V> valueType, Long expireTime, TimeUnit expireTimeUnit, Long maxEntries, boolean cacheNull, Object... params) {
        K key = keyFunction.apply(params);
        return compute(cacheName, key, supplier, keyType, valueType, expireTime, expireTimeUnit, maxEntries, cacheNull);
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
     * @param cacheName      缓存名称
     * @param key            缓存键
     * @param supplier       值提供者（如果缓存不存在时调用）
     * @param keyType        Key 类型（可选，如果为 null 则自动推断）
     * @param valueType      Value 类型（可选，如果为 null 则自动推断）
     * @param expireTime     过期时间（可选，如果为 null 则使用默认值或已注册配置的值）
     * @param expireTimeUnit 过期时间单位（可选，如果为 null 则使用默认值或已注册配置的值）
     * @param cacheNull      是否缓存 null 值（true：缓存 null，避免重复查询；false：不缓存 null，每次都重新查询）
     * @param maxEntries     最大条目数（可选，如果为 null 则使用默认值 1000 或已注册配置的值）
     * @param <K>            Key 类型
     * @param <V>            Value 类型
     * @return 缓存值
     */
    public <K, V> V compute(String cacheName, K key, Supplier<V> supplier, Class<K> keyType, Class<V> valueType, Long expireTime, TimeUnit expireTimeUnit, Long maxEntries, boolean cacheNull) {
        if (ehCacheManager == null) {
            log.warn("EhCacheManager is not available, returning value from supplier");
            return supplier.get();
        }
        CacheConfig config = getCacheConfig(cacheName);
        // 如果配置不存在，尝试根据传入的类型和过期时间创建配置
        if (config == null) {
            if (keyType != null && valueType != null) {
                // 如果 cacheNull 为 true，需要使用 Object 作为 Value 类型以支持存储 NULL_PLACEHOLDER
                Class<?> finalValueType = cacheNull ? Object.class : valueType;
                // 使用传入的过期时间，如果未指定则使用默认值
                long finalExpireTime = expireTime != null ? expireTime : 60;
                TimeUnit finalExpireTimeUnit = expireTimeUnit != null ? expireTimeUnit : TimeUnit.MINUTES;
                // 使用传入的最大条目数，如果未指定则使用默认值 1000
                long finalMaxEntries = maxEntries != null ? maxEntries : 1000;
                config = CacheConfig.builder().cacheName(cacheName).keyType(keyType).valueType(finalValueType).maxEntries(finalMaxEntries).expireTime(finalExpireTime).expireTimeUnit(finalExpireTimeUnit).build();
                config.validate();
                ehCacheManager.registerCacheConfig(config);
            } else {
                log.warn("Cache config not found for: {} and types not specified, returning value from supplier", cacheName);
                return supplier.get();
            }
        } else if (cacheNull && config.getValueType() != Object.class) {
            // 如果已存在配置但需要缓存 null，且 Value 类型不是 Object
            // 我们需要使用 Object 类型来获取缓存，这样可以兼容存储 NULL_PLACEHOLDER 和实际值
            // 注意：这里不创建新配置，而是直接使用 Object.class 来获取缓存
            log.debug("Cache config exists for: {} but valueType is not Object, using Object.class for null caching", cacheName);
        }
        // 如果 cacheNull 为 true，使用 Object.class 作为 Value 类型以支持存储 NULL_PLACEHOLDER
        // 否则使用配置中的 Value 类型
        Class<?> cacheValueType = cacheNull ? Object.class : config.getValueType();
        @SuppressWarnings("unchecked")
        Cache<Object, Object> cache = (Cache<Object, Object>) ehCacheManager.getCache(cacheName, (Class<K>) config.getKeyType(), cacheValueType);
        if (cache == null) {
            // 如果缓存不存在，需要创建。如果 cacheNull 为 true，确保使用 Object.class
            if (cacheNull && config.getValueType() != Object.class) {
                // 创建临时配置，使用 Object.class 作为 Value 类型
                // 使用传入的最大条目数，如果未指定则使用配置中的值
                long finalMaxEntries = maxEntries != null ? maxEntries : config.getMaxEntries();
                CacheConfig tempConfig = CacheConfig.builder()
                        .cacheName(cacheName)
                        .keyType(config.getKeyType())
                        .valueType(Object.class)
                        .maxEntries(finalMaxEntries)
                        .expireTime(expireTime != null ? expireTime : config.getExpireTime())
                        .expireTimeUnit(expireTimeUnit != null ? expireTimeUnit : config.getExpireTimeUnit())
                        .build();
                tempConfig.validate();
                cache = ehCacheManager.getOrCreateCache(tempConfig);
            } else {
                cache = ehCacheManager.getOrCreateCache(config);
            }
        }
        Object cached = cache.get(key);
        if (cached == null) {
            // 缓存中不存在，调用 supplier 获取值
            V value = supplier.get();
            if (value != null) {
                // 值不为 null，直接缓存
                cache.put(key, value);
                return value;
            } else if (cacheNull) {
                // 值为 null 且允许缓存 null，使用占位符缓存
                cache.put(key, NULL_PLACEHOLDER);
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
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @param <K>       Key 类型
     * @param <V>       Value 类型
     * @return 缓存值，如果不存在返回 null
     */
    public <K, V> V get(String cacheName, K key) {
        if (ehCacheManager == null) {
            return null;
        }
        Cache<K, V> cache = ehCacheManager.getCache(cacheName);
        if (cache == null) {
            return null;
        }
        return cache.get(key);
    }

    /**
     * 设置缓存值
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @param value     缓存值
     * @param <K>       Key 类型
     * @param <V>       Value 类型
     */
    @SuppressWarnings("unchecked")
    public <K, V> void put(String cacheName, K key, V value) {
        if (ehCacheManager == null || key == null || value == null) {
            return;
        }
        CacheConfig config = getCacheConfig(cacheName);
        if (config == null) {
            // 如果配置不存在，根据实际类型创建配置
            Class<?> keyType = key.getClass();
            Class<?> valueType = value.getClass();
            config = CacheConfig.builder()
                    .cacheName(cacheName)
                    .keyType(keyType)
                    .valueType(valueType)
                    // 使用默认值：maxEntries=1000, expireTime=10, expireTimeUnit=MINUTES
                    .build();
            config.validate();
            ehCacheManager.registerCacheConfig(config);
        }
        Cache<K, V> cache = ehCacheManager.getCache(cacheName,
                (Class<K>) config.getKeyType(),
                (Class<V>) config.getValueType());
        if (cache == null) {
            cache = ehCacheManager.getOrCreateCache(config);
        }
        cache.put(key, value);
    }

    /**
     * 移除缓存值
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @param <K>       Key 类型
     */
    public <K> void remove(String cacheName, K key) {
        if (ehCacheManager == null) {
            return;
        }
        Cache<K, ?> cache = ehCacheManager.getCache(cacheName);
        if (cache != null) {
            cache.remove(key);
        }
    }

    /**
     * 清空缓存
     *
     * @param cacheName 缓存名称
     */
    public void clear(String cacheName) {
        if (ehCacheManager != null) {
            ehCacheManager.clearCache(cacheName);
        }
    }

    /**
     * 获取或创建缓存配置
     * 优先从 EhCacheManager 获取已注册的配置
     * 如果不存在，根据缓存名称推断类型并创建默认配置
     *
     * @param cacheName 缓存名称
     * @return 缓存配置，如果无法创建返回 null
     */
    private CacheConfig getCacheConfig(String cacheName) {
        if (ehCacheManager == null || cacheName == null) {
            return null;
        }
        // 1. 优先从 EhCacheManager 获取已注册的配置
        CacheConfig registeredConfig = ehCacheManager.getCacheConfig(cacheName);
        if (registeredConfig != null) {
            return registeredConfig;
        }
        // 2. 如果未注册，尝试根据缓存名称推断类型并创建默认配置
        return createDefaultCacheConfig(cacheName);
    }

    /**
     * 根据缓存名称创建默认配置
     * 根据常见的缓存命名模式推断 Key/Value 类型
     *
     * @param cacheName 缓存名称
     * @return 默认缓存配置
     */
    private CacheConfig createDefaultCacheConfig(String cacheName) {
        // 根据缓存名称推断类型
        Class<?> keyType = String.class; // 默认 Key 类型为 String
        Class<?> valueType = Object.class; // 默认 Value 类型为 Object
        // 根据缓存名称后缀推断 Value 类型
        String lowerName = cacheName.toLowerCase();
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
        CacheConfig config = CacheConfig.builder()
                .cacheName(cacheName)
                .keyType(keyType)
                .valueType(valueType)
                // 使用 @Builder.Default 设置的默认值：maxEntries=1000, expireTime=60, expireTimeUnit=MINUTES
                .build();
        config.validate();
        return config;
    }

    /**
     * 创建缓存配置的便捷方法
     * 使用 Lombok @Builder 生成
     *
     * @param cacheName      缓存名称
     * @param keyType        Key 类型
     * @param valueType      Value 类型
     * @param maxEntries     最大条目数
     * @param expireTime     过期时间
     * @param expireTimeUnit 过期时间单位
     * @return 缓存配置
     */
    public static CacheConfig createCacheConfig(String cacheName,
                                                Class<?> keyType,
                                                Class<?> valueType,
                                                long maxEntries,
                                                long expireTime,
                                                TimeUnit expireTimeUnit) {
        CacheConfig config = CacheConfig.builder()
                .cacheName(cacheName)
                .keyType(keyType)
                .valueType(valueType)
                .maxEntries(maxEntries)
                .expireTime(expireTime)
                .expireTimeUnit(expireTimeUnit)
                .build();
        config.validate();
        return config;
    }

    /**
     * 注册缓存配置
     * 如果配置已存在，则不会覆盖
     *
     * @param config 缓存配置
     */
    public void registerCacheConfig(CacheConfig config) {
        if (ehCacheManager != null && config != null) {
            if (!ehCacheManager.hasCacheConfig(config.getCacheName())) {
                ehCacheManager.registerCacheConfig(config);
            }
        }
    }

    /**
     * 检查缓存是否存在
     *
     * @param cacheName 缓存名称
     * @return 是否存在
     */
    public boolean hasCache(String cacheName) {
        if (ehCacheManager == null) {
            return false;
        }
        return ehCacheManager.hasCacheConfig(cacheName) || ehCacheManager.getCache(cacheName) != null;
    }

    /**
     * 获取缓存统计信息（如果支持）
     *
     * @param cacheName 缓存名称
     * @return 缓存大小，如果不支持返回 -1
     */
    public long getCacheSize(String cacheName) {
        if (ehCacheManager == null) {
            return -1;
        }
        Cache<Object, Object> cache = ehCacheManager.getCache(cacheName);
        if (cache == null) {
            return -1;
        }
        // EhCache 3.x 不直接支持获取大小，需要通过迭代计算
        // 这里返回 -1 表示不支持，或者可以通过其他方式实现
        return -1;
    }

    /**
     * 清空所有缓存
     * 实现 ClearHandler 接口，用于系统级别的缓存清理
     */
    @Override
    public void clear() {
        if (ehCacheManager != null) {
            ehCacheManager.clearAllCaches();
        }
    }
}