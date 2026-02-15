package cn.org.autumn.service;

import cn.org.autumn.config.CacheConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 共享缓存服务
 * <p>
 * 用于在多个相互独立的子项目之间共享缓存数据。
 * 由于子项目的实体类定义不同，但部分数据可能有共性，
 * 本服务提供了一套不依赖实体类的、自定义的共享缓存功能。
 * <p>
 * 核心设计：
 * 1. 不依赖实体类：使用 Object.class 作为默认值类型，支持存储任意类型数据
 * 2. 基于名称的共享：不同子项目通过约定相同的缓存名称来共享数据
 * 3. 数据提供者模式：子类可覆盖 getShareEntity 系列方法提供缓存数据源
 * 4. 完整的 CRUD 操作：提供 get/put/remove/clear 全套操作
 * 5. 多种缓存模式：支持单值缓存、List列表缓存、Map缓存
 * 6. 复合Key支持：支持多个值组合成复合Key
 * 7. Supplier惰性加载：支持缓存未命中时通过回调获取数据
 * 8. 自定义类型：支持指定Value类型的独立缓存配置
 * <p>
 * 数据提供者模式（参考 BaseCacheService.getEntity）：
 * <pre>
 * // getShareCache 调用链路:
 * //   getShareCache(key) → cacheService.compute(key, () → getShareEntity(key), config)
 * //
 * // 子类覆盖 getShareEntity 提供数据:
 * public class UserStatusService extends BaseService&lt;UserMapper, UserEntity&gt; {
 *     &#64;Override
 *     public String getShareCacheName() { return "shared_user_status"; }
 *
 *     &#64;Override
 *     public &lt;V&gt; V getShareEntity(Object key) {
 *         // 从数据库或其他数据源获取数据
 *         return (V) userDao.getStatus((String) key);
 *     }
 * }
 *
 * // 也可以覆盖命名版本，为不同缓存名称提供不同的数据源:
 * &#64;Override
 * public &lt;V&gt; V getShareEntity(String shareName, Object key) {
 *     if ("user_status".equals(shareName)) {
 *         return (V) getStatusFromDb(key);
 *     } else if ("user_profile".equals(shareName)) {
 *         return (V) getProfileFromApi(key);
 *     }
 *     return null;
 * }
 *
 * // 或者在调用时直接传入 Supplier（优先级高于 getShareEntity）:
 * String status = getShareCache("user123", () → fetchFromRemote("user123"));
 * </pre>
 *
 * @param <M> Mapper类型
 * @param <T> 实体类型
 */
@Slf4j
public abstract class ShareCacheService<M extends BaseMapper<T>, T> extends BaseCacheService<M, T> {

    // ==================== 配置相关方法（子类可覆盖）====================

    /**
     * 获取默认共享缓存名称
     * 子类可覆盖此方法，提供唯一的共享缓存名称
     * 不同子项目中的Service通过返回相同的名称来实现数据共享
     *
     * @return 共享缓存名称，如果返回null则必须在调用时显式传入名称
     */
    public String getShareCacheName() {
        return "share";
    }

    /**
     * 获取共享缓存Key类型
     * 默认为 String.class，适用于大多数共享缓存场景
     *
     * @return Key类型
     */
    public Class<?> getShareCacheKeyType() {
        return String.class;
    }

    /**
     * 获取共享缓存Value类型
     * 默认为 Object.class，支持存储任意类型的数据
     * 子类可覆盖为具体类型以获得更好的类型安全
     *
     * @return Value类型
     */
    public Class<?> getShareModelClass() {
        return Object.class;
    }

    /**
     * 获取共享缓存过期时间（分钟）
     * 默认10分钟，子类可覆盖以自定义过期策略
     *
     * @return 过期时间（分钟）
     */
    public long getShareCacheExpire() {
        return 10;
    }

    /**
     * 获取指定名称共享缓存的过期时间（分钟）
     * 默认使用 getShareCacheExpire() 的值
     * 子类可覆盖以为不同的共享缓存设置不同的过期时间
     *
     * @param shareName 共享缓存名称
     * @return 过期时间（分钟）
     */
    public long getShareCacheExpire(String shareName) {
        return getShareCacheExpire();
    }

    /**
     * 是否缓存null值
     * 开启后可以避免缓存穿透，但会占用额外的缓存空间
     *
     * @return true表示缓存null值，false表示不缓存
     */
    public boolean isShareCacheNull() {
        return false;
    }

    /**
     * 获取指定名称共享缓存是否缓存null值
     *
     * @param shareName 共享缓存名称
     * @return true表示缓存null值
     */
    public boolean isShareCacheNull(String shareName) {
        return isShareCacheNull();
    }

    // ==================== 配置相关方法 - 带类型参数（子类可覆盖）====================

    /**
     * 获取指定类型的共享缓存名称
     * 子类可覆盖此方法，为不同的类型提供不同的共享缓存名称
     * <p>
     * 使用场景：同一个Service管理多个不同类型的共享缓存
     * <pre>
     * &#64;Override
     * public String getShareCacheName(Class&lt;?&gt; clazz) {
     *     if (clazz == UserDto.class) return "shared_user";
     *     if (clazz == ConfigDto.class) return "shared_config";
     *     return super.getShareCacheName(clazz);
     * }
     * </pre>
     *
     * @param clazz 值类型
     * @return 共享缓存名称，默认委托给 getShareCacheName()
     */
    public String getShareCacheName(Class<?> clazz) {
        return clazz.getSimpleName().toLowerCase();
    }

    /**
     * 获取指定类型的共享缓存Key类型
     * 子类可覆盖此方法，为不同的类型提供不同的Key类型
     *
     * @param clazz 值类型
     * @return Key类型，默认委托给 getShareCacheKeyType()
     */
    public Class<?> getShareCacheKeyType(Class<?> clazz) {
        return String.class;
    }

    /**
     * 获取指定类型的共享缓存过期时间（分钟）
     * 子类可覆盖此方法，为不同的类型设置不同的过期时间
     *
     * @param clazz 值类型
     * @return 过期时间（分钟），默认委托给 getShareCacheExpire()
     */
    public long getShareCacheExpire(Class<?> clazz) {
        return getShareCacheExpire();
    }

    /**
     * 获取指定名称和类型的共享缓存过期时间（分钟）
     * 子类可覆盖此方法，根据名称和类型的组合设置过期时间
     *
     * @param shareName 共享缓存名称
     * @param clazz     值类型
     * @return 过期时间（分钟），默认委托给 getShareCacheExpire(shareName)
     */
    public long getShareCacheExpire(String shareName, Class<?> clazz) {
        return getShareCacheExpire(shareName);
    }

    /**
     * 获取指定类型的共享缓存是否缓存null值
     * 子类可覆盖此方法，为不同的类型设置不同的null缓存策略
     *
     * @param clazz 值类型
     * @return true表示缓存null值，默认委托给 isShareCacheNull()
     */
    public boolean isShareCacheNull(Class<?> clazz) {
        return isShareCacheNull();
    }

    /**
     * 获取指定名称和类型的共享缓存是否缓存null值
     *
     * @param shareName 共享缓存名称
     * @param clazz     值类型
     * @return true表示缓存null值，默认委托给 isShareCacheNull(shareName)
     */
    public boolean isShareCacheNull(String shareName, Class<?> clazz) {
        return isShareCacheNull(shareName);
    }

    // ==================== 缓存配置获取 ====================

    /**
     * 获取默认共享缓存配置
     * 使用 getShareCacheName() 返回的名称作为缓存名称
     *
     * @return 缓存配置，如果 getShareCacheName() 返回null则返回null
     */
    public CacheConfig getShareConfig() {
        String name = getShareCacheName();
        if (name == null || name.isEmpty()) {
            return null;
        }
        CacheConfig config = configs.get(name);
        if (null == config) {
            config = CacheConfig.builder()
                    .name(name)
                    .key(getShareCacheKeyType())
                    .value(getShareModelClass())
                    .expire(getShareCacheExpire())
                    .Null(isShareCacheNull())
                    .build();
            configs.put(name, config);
        }
        return config;
    }

    /**
     * 获取指定名称的共享缓存配置
     * 允许在不覆盖 getShareCacheName() 的情况下直接使用命名缓存
     *
     * @param shareName 共享缓存名称，不能为空
     * @return 缓存配置，如果 shareName 为空则回退到默认配置
     */
    public CacheConfig getShareConfig(String shareName) {
        if (shareName == null || shareName.isEmpty()) {
            return getShareConfig();
        }
        CacheConfig config = configs.get(shareName);
        if (null == config) {
            config = CacheConfig.builder()
                    .name(shareName)
                    .key(getShareCacheKeyType())
                    .value(getShareModelClass())
                    .expire(getShareCacheExpire(shareName))
                    .Null(isShareCacheNull(shareName))
                    .build();
            configs.put(shareName, config);
        }
        return config;
    }

    /**
     * 获取指定名称和Value类型的共享缓存配置
     * 为特定类型的缓存数据创建独立的缓存配置
     * 缓存名称格式为：{shareName}{valueTypeName}
     *
     * @param shareName 共享缓存名称，如果为空则使用默认名称
     * @param valueType 值类型
     * @param <V>       值类型泛型
     * @return 缓存配置
     */
    public <V> CacheConfig getShareConfig(String shareName, Class<V> valueType) {
        String baseName = (shareName != null && !shareName.isEmpty())
                ? shareName
                : (valueType != null ? getShareCacheName(valueType) : getShareCacheName());
        if (baseName == null || baseName.isEmpty()) {
            return null;
        }
        String configName = baseName + (valueType != null && valueType != Object.class
                ? valueType.getSimpleName().toLowerCase() : "");
        CacheConfig config = configs.get(configName);
        if (null == config) {
            config = CacheConfig.builder()
                    .name(configName)
                    .key(valueType != null ? getShareCacheKeyType(valueType) : getShareCacheKeyType())
                    .value(valueType != null ? valueType : getShareModelClass())
                    .expire(valueType != null ? getShareCacheExpire(baseName, valueType) : getShareCacheExpire(baseName))
                    .Null(valueType != null ? isShareCacheNull(baseName, valueType) : isShareCacheNull(baseName))
                    .build();
            configs.put(configName, config);
        }
        return config;
    }

    /**
     * 获取默认共享List缓存配置
     * 缓存名称格式为：{shareName}list
     *
     * @return List缓存配置
     */
    public CacheConfig getShareListConfig() {
        String name = getShareCacheName();
        if (name == null || name.isEmpty()) {
            return null;
        }
        String listName = name + "list";
        CacheConfig config = configs.get(listName);
        if (null == config) {
            config = CacheConfig.builder()
                    .name(listName)
                    .key(getShareCacheKeyType())
                    .value(List.class)
                    .expire(getShareCacheExpire())
                    .Null(isShareCacheNull())
                    .build();
            configs.put(listName, config);
        }
        return config;
    }

    /**
     * 获取指定名称的共享List缓存配置
     * 缓存名称格式为：{shareName}list
     *
     * @param shareName 共享缓存名称
     * @return List缓存配置
     */
    public CacheConfig getShareListConfig(String shareName) {
        if (shareName == null || shareName.isEmpty()) {
            return getShareListConfig();
        }
        String listName = shareName + "list";
        CacheConfig config = configs.get(listName);
        if (null == config) {
            config = CacheConfig.builder()
                    .name(listName)
                    .key(getShareCacheKeyType())
                    .value(List.class)
                    .expire(getShareCacheExpire(shareName))
                    .Null(isShareCacheNull(shareName))
                    .build();
            configs.put(listName, config);
        }
        return config;
    }

    /**
     * 获取指定Value类型的共享List缓存配置（使用默认名称）
     * 缓存名称格式为：{shareName}list{valueTypeName}
     *
     * @param valueType 列表元素类型
     * @param <V>       列表元素类型泛型
     * @return List缓存配置
     */
    public <V> CacheConfig getShareListConfig(Class<V> valueType) {
        String name = getShareCacheName();
        if (name == null || name.isEmpty() || valueType == null) {
            return null;
        }
        String listName = name + "list" + (valueType != Object.class
                ? valueType.getSimpleName().toLowerCase() : "");
        CacheConfig config = configs.get(listName);
        if (null == config) {
            config = CacheConfig.builder()
                    .name(listName)
                    .key(getShareCacheKeyType(valueType))
                    .value(List.class)
                    .expire(getShareCacheExpire(valueType))
                    .Null(isShareCacheNull(valueType))
                    .build();
            configs.put(listName, config);
        }
        return config;
    }

    /**
     * 获取指定名称和Value类型的共享List缓存配置
     * 缓存名称格式为：{shareName}list{valueTypeName}
     *
     * @param shareName 共享缓存名称
     * @param valueType 列表元素类型
     * @param <V>       列表元素类型泛型
     * @return List缓存配置
     */
    public <V> CacheConfig getShareListConfig(String shareName, Class<V> valueType) {
        String baseName = (shareName != null && !shareName.isEmpty())
                ? shareName
                : (valueType != null ? getShareCacheName(valueType) : getShareCacheName());
        if (baseName == null || baseName.isEmpty()) {
            return null;
        }
        String listName = baseName + "list" + (valueType != null && valueType != Object.class
                ? valueType.getSimpleName().toLowerCase() : "");
        CacheConfig config = configs.get(listName);
        if (null == config) {
            config = CacheConfig.builder()
                    .name(listName)
                    .key(valueType != null ? getShareCacheKeyType(valueType) : getShareCacheKeyType())
                    .value(List.class)
                    .expire(valueType != null ? getShareCacheExpire(baseName, valueType) : getShareCacheExpire(baseName))
                    .Null(valueType != null ? isShareCacheNull(baseName, valueType) : isShareCacheNull(baseName))
                    .build();
            configs.put(listName, config);
        }
        return config;
    }

    /**
     * 构建自定义共享缓存配置
     * 用于创建完全自定义的缓存配置，适用于特殊需求场景
     * <p>
     * 注意：同名缓存配置一旦创建不可更改，第一次创建的配置将被缓存使用
     *
     * @param name      缓存名称
     * @param keyType   Key类型
     * @param valueType Value类型
     * @param expire    过期时间（分钟）
     * @param cacheNull 是否缓存null值
     * @return 缓存配置
     */
    protected CacheConfig buildShareConfig(String name, Class<?> keyType, Class<?> valueType, long expire, boolean cacheNull) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        CacheConfig config = configs.get(name);
        if (null == config) {
            config = CacheConfig.builder()
                    .name(name)
                    .key(keyType != null ? keyType : String.class)
                    .value(valueType != null ? valueType : Object.class)
                    .expire(expire)
                    .Null(cacheNull)
                    .build();
            configs.put(name, config);
        }
        return config;
    }

    // ==================== 数据提供者 - 单值（子类可覆盖）====================

    /**
     * 共享缓存数据提供者（使用默认共享缓存名称）
     * <p>
     * 当缓存未命中时，getShareCache 方法会调用此方法获取数据。
     * 子类可覆盖此方法，从数据库、远程API或其他数据源获取共享数据。
     * <p>
     * 对应关系（类比 BaseCacheService）：
     * - BaseCacheService: getCache(key) → getEntity(key)
     * - ShareCacheService: getShareCache(key) → getShareEntity(key)
     *
     * @param key 缓存key
     * @param <V> 值类型
     * @return 数据值，默认返回null（子类覆盖提供具体实现）
     */
    public <V> V getShareEntity(Object key) {
        return null;
    }

    /**
     * 指定缓存名称的共享缓存数据提供者
     * <p>
     * 子类可覆盖此方法，根据不同的 shareName 提供不同的数据源。
     * 默认委托给 getShareEntity(key)。
     * <p>
     * 使用场景：一个Service需要管理多个不同的共享缓存时
     * <pre>
     * &#64;Override
     * public &lt;V&gt; V getShareEntity(String shareName, Object key) {
     *     if ("user_status".equals(shareName)) {
     *         return (V) queryUserStatus(key);
     *     } else if ("user_profile".equals(shareName)) {
     *         return (V) queryUserProfile(key);
     *     }
     *     return super.getShareEntity(shareName, key);
     * }
     * </pre>
     *
     * @param shareName 共享缓存名称
     * @param key       缓存key
     * @param <V>       值类型
     * @return 数据值，默认委托给 getShareEntity(key)
     */
    public <V> V getShareEntity(String shareName, Object key) {
        return getShareEntity(key);
    }

    /**
     * 使用默认缓存名称、指定Value类型的共享缓存数据提供者
     * 默认委托给 getShareEntity(getShareCacheName(), valueType, key)。
     *
     * @param valueType 值类型
     * @param key       缓存key
     * @param <V>       值类型泛型
     * @return 数据值
     */
    public <V> V getShareEntity(Class<V> valueType, Object key) {
        return getShareEntity(getShareCacheName(valueType), valueType, key);
    }

    /**
     * 指定缓存名称和Value类型的共享缓存数据提供者
     * <p>
     * 子类可覆盖此方法，根据不同的类型提供类型安全的数据。
     * 默认委托给 getShareEntity(shareName, key)。
     * <p>
     * 使用场景：同一缓存名称下存储不同类型的数据
     * <pre>
     * &#64;Override
     * public &lt;V&gt; V getShareEntity(String shareName, Class&lt;V&gt; valueType, Object key) {
     *     Object raw = queryFromDb(shareName, key);
     *     return convertTo(raw, valueType);
     * }
     * </pre>
     *
     * @param shareName 共享缓存名称
     * @param valueType 值类型
     * @param key       缓存key
     * @param <V>       值类型泛型
     * @return 数据值，默认委托给 getShareEntity(shareName, key)
     */
    public <V> V getShareEntity(String shareName, Class<V> valueType, Object key) {
        return getShareEntity(shareName, key);
    }

    /**
     * 共享缓存数据提供者（可变参数版本，使用默认共享缓存名称）
     * <p>
     * 多个 keys 会被组合为复合Key。子类可覆盖此方法提供多维度查询的数据。
     * 如果只有一个参数，委托给单参数版本 getShareEntity(Object key)。
     * <p>
     * 对应关系（类比 BaseCacheService）：
     * - BaseCacheService: getCache(keys...) → getEntity(keys...)
     * - ShareCacheService: getShareCache(keys...) → getShareEntity(keys...)
     *
     * @param keys 可变参数key
     * @param <V>  值类型
     * @return 数据值，默认返回null（子类覆盖提供具体实现）
     */
    public <V> V getShareEntity(Object... keys) {
        if (keys == null || keys.length == 0) {
            return null;
        }
        if (keys.length == 1) {
            return getShareEntity(keys[0]);
        }
        return null;
    }

    /**
     * 指定缓存名称的共享缓存数据提供者（可变参数版本）
     * <p>
     * 如果只有一个参数，委托给单参数版本 getShareEntity(String, Object)。
     * 默认委托给 getShareEntity(Object... keys)。
     *
     * @param shareName 共享缓存名称
     * @param keys      可变参数key
     * @param <V>       值类型
     * @return 数据值
     */
    public <V> V getShareEntity(String shareName, Object... keys) {
        if (keys == null || keys.length == 0) {
            return null;
        }
        if (keys.length == 1) {
            return getShareEntity(shareName, keys[0]);
        }
        return getShareEntity(keys);
    }

    /**
     * 指定Value类型的共享缓存数据提供者（可变参数版本）
     * <p>
     * 如果只有一个参数，委托给单参数版本 getShareEntity(Class, Object)。
     * 默认委托给 getShareEntity(String, Object... keys)。
     *
     * @param valueType 值类型
     * @param keys      可变参数key
     * @param <V>       值类型泛型
     * @return 数据值
     */
    public <V> V getShareEntity(Class<V> valueType, Object... keys) {
        if (keys == null || keys.length == 0) {
            return null;
        }
        if (keys.length == 1) {
            return getShareEntity(valueType, keys[0]);
        }
        return getShareEntity(getShareCacheName(valueType), valueType, keys);
    }

    /**
     * 指定缓存名称和Value类型的共享缓存数据提供者（可变参数版本）
     * <p>
     * 如果只有一个参数，委托给单参数版本 getShareEntity(String, Class, Object)。
     * 默认委托给 getShareEntity(String, Object... keys)。
     *
     * @param shareName 共享缓存名称
     * @param valueType 值类型
     * @param keys      可变参数key
     * @param <V>       值类型泛型
     * @return 数据值
     */
    public <V> V getShareEntity(String shareName, Class<V> valueType, Object... keys) {
        if (keys == null || keys.length == 0) {
            return null;
        }
        if (keys.length == 1) {
            return getShareEntity(shareName, valueType, keys[0]);
        }
        return getShareEntity(shareName, keys);
    }

    // ==================== 数据提供者 - List（子类可覆盖）====================

    /**
     * 共享List缓存数据提供者（使用默认共享缓存名称）
     * <p>
     * 当List缓存未命中时，getShareListCache 方法会调用此方法获取列表数据。
     * 子类可覆盖此方法，从数据库等数据源获取列表数据。
     * <p>
     * 对应关系（类比 BaseCacheService）：
     * - BaseCacheService: getListCache(key) → getListEntity(key)
     * - ShareCacheService: getShareListCache(key) → getShareListEntity(key)
     *
     * @param key 缓存key
     * @param <V> 列表元素类型
     * @return 列表数据，默认返回null（子类覆盖提供具体实现）
     */
    public <V> List<V> getShareListEntity(Object key) {
        return null;
    }

    /**
     * 指定缓存名称的共享List缓存数据提供者
     * <p>
     * 子类可覆盖此方法，根据不同的 shareName 提供不同的列表数据源。
     * 默认委托给 getShareListEntity(key)。
     *
     * @param shareName 共享缓存名称
     * @param key       缓存key
     * @param <V>       列表元素类型
     * @return 列表数据，默认委托给 getShareListEntity(key)
     */
    public <V> List<V> getShareListEntity(String shareName, Object key) {
        return getShareListEntity(key);
    }

    /**
     * 使用默认缓存名称、指定Value类型的共享List缓存数据提供者
     * 默认委托给 getShareListEntity(getShareCacheName(), valueType, key)。
     *
     * @param valueType 列表元素类型
     * @param key       缓存key
     * @param <V>       列表元素类型泛型
     * @return 列表数据
     */
    public <V> List<V> getShareListEntity(Class<V> valueType, Object key) {
        return getShareListEntity(getShareCacheName(valueType), valueType, key);
    }

    /**
     * 指定缓存名称和Value类型的共享List缓存数据提供者
     * <p>
     * 子类可覆盖此方法，根据不同的类型提供类型安全的列表数据。
     * 默认委托给 getShareListEntity(shareName, key)。
     *
     * @param shareName 共享缓存名称
     * @param valueType 列表元素类型
     * @param key       缓存key
     * @param <V>       列表元素类型泛型
     * @return 列表数据，默认委托给 getShareListEntity(shareName, key)
     */
    public <V> List<V> getShareListEntity(String shareName, Class<V> valueType, Object key) {
        return getShareListEntity(shareName, key);
    }

    /**
     * 共享List缓存数据提供者（可变参数版本，使用默认共享缓存名称）
     * <p>
     * 如果只有一个参数，委托给单参数版本 getShareListEntity(Object key)。
     *
     * @param keys 可变参数key
     * @param <V>  列表元素类型
     * @return 列表数据，默认返回null（子类覆盖提供具体实现）
     */
    public <V> List<V> getShareListEntity(Object... keys) {
        if (keys == null || keys.length == 0) {
            return null;
        }
        if (keys.length == 1) {
            return getShareListEntity(keys[0]);
        }
        return null;
    }

    /**
     * 指定缓存名称的共享List缓存数据提供者（可变参数版本）
     * <p>
     * 如果只有一个参数，委托给单参数版本 getShareListEntity(String, Object)。
     * 默认委托给 getShareListEntity(Object... keys)。
     *
     * @param shareName 共享缓存名称
     * @param keys      可变参数key
     * @param <V>       列表元素类型
     * @return 列表数据
     */
    public <V> List<V> getShareListEntity(String shareName, Object... keys) {
        if (keys == null || keys.length == 0) {
            return null;
        }
        if (keys.length == 1) {
            return getShareListEntity(shareName, keys[0]);
        }
        return getShareListEntity(keys);
    }

    /**
     * 指定Value类型的共享List缓存数据提供者（可变参数版本）
     * <p>
     * 如果只有一个参数，委托给单参数版本 getShareListEntity(Class, Object)。
     * 默认委托给 getShareListEntity(String, Object... keys)。
     *
     * @param valueType 列表元素类型
     * @param keys      可变参数key
     * @param <V>       列表元素类型泛型
     * @return 列表数据
     */
    public <V> List<V> getShareListEntity(Class<V> valueType, Object... keys) {
        if (keys == null || keys.length == 0) {
            return null;
        }
        if (keys.length == 1) {
            return getShareListEntity(valueType, keys[0]);
        }
        return getShareListEntity(getShareCacheName(valueType), valueType, keys);
    }

    /**
     * 指定缓存名称和Value类型的共享List缓存数据提供者（可变参数版本）
     * <p>
     * 如果只有一个参数，委托给单参数版本 getShareListEntity(String, Class, Object)。
     * 默认委托给 getShareListEntity(String, Object... keys)。
     *
     * @param shareName 共享缓存名称
     * @param valueType 列表元素类型
     * @param keys      可变参数key
     * @param <V>       列表元素类型泛型
     * @return 列表数据
     */
    public <V> List<V> getShareListEntity(String shareName, Class<V> valueType, Object... keys) {
        if (keys == null || keys.length == 0) {
            return null;
        }
        if (keys.length == 1) {
            return getShareListEntity(shareName, valueType, keys[0]);
        }
        return getShareListEntity(shareName, keys);
    }

    // ==================== 数据提供者 - Map（子类可覆盖）====================

    /**
     * 共享Map缓存数据提供者（使用默认共享缓存名称）
     * <p>
     * 当Map缓存未命中时，getShareMapCache 方法会调用此方法获取Map数据。
     * 适用于不依赖实体类、使用Map存储结构化数据的场景。
     * 默认委托给 getShareEntity(key)。
     *
     * @param key 缓存key
     * @return Map数据，默认委托给 getShareEntity(key)
     */
    public Map<String, Object> getShareMapEntity(Object key) {
        return getShareEntity(key);
    }

    /**
     * 指定缓存名称的共享Map缓存数据提供者
     * 默认委托给 getShareEntity(shareName, key)。
     *
     * @param shareName 共享缓存名称
     * @param key       缓存key
     * @return Map数据，默认委托给 getShareEntity(shareName, key)
     */
    public Map<String, Object> getShareMapEntity(String shareName, Object key) {
        return getShareEntity(shareName, key);
    }

    /**
     * 共享Map缓存数据提供者（可变参数版本，使用默认共享缓存名称）
     * <p>
     * 如果只有一个参数，委托给单参数版本 getShareMapEntity(Object key)。
     * 默认委托给 getShareEntity(Object... keys)。
     *
     * @param keys 可变参数key
     * @return Map数据
     */
    public Map<String, Object> getShareMapEntity(Object... keys) {
        if (keys == null || keys.length == 0) {
            return null;
        }
        if (keys.length == 1) {
            return getShareMapEntity(keys[0]);
        }
        return getShareEntity(keys);
    }

    /**
     * 指定缓存名称的共享Map缓存数据提供者（可变参数版本）
     * <p>
     * 如果只有一个参数，委托给单参数版本 getShareMapEntity(String, Object)。
     * 默认委托给 getShareEntity(String, Object... keys)。
     *
     * @param shareName 共享缓存名称
     * @param keys      可变参数key
     * @return Map数据
     */
    public Map<String, Object> getShareMapEntity(String shareName, Object... keys) {
        if (keys == null || keys.length == 0) {
            return null;
        }
        if (keys.length == 1) {
            return getShareMapEntity(shareName, keys[0]);
        }
        return getShareEntity(shareName, keys);
    }

    // ==================== 获取共享缓存 - 单值 ====================

    /**
     * 获取共享缓存值（使用默认共享缓存名称）
     * <p>
     * 如果缓存未命中，会调用 getShareEntity(key) 获取数据并缓存。
     * 子类可覆盖 getShareEntity 方法提供数据源。
     *
     * @param key 缓存key
     * @param <V> 值类型
     * @return 缓存值，不存在且 getShareEntity 返回null则返回null
     */
    public <V> V getShareCache(Object key) {
        if (key == null) {
            return null;
        }
        CacheConfig config = getShareConfig();
        if (config == null) {
            return null;
        }
        return cacheService.compute(key, () -> getShareEntity(key), config);
    }

    /**
     * 获取共享缓存值，如果不存在则通过supplier获取并缓存（使用默认共享缓存名称）
     * <p>
     * 显式传入的 supplier 优先级高于 getShareEntity 方法。
     *
     * @param key      缓存key
     * @param supplier 值提供者，缓存未命中时调用（优先于 getShareEntity）
     * @param <V>      值类型
     * @return 缓存值
     */
    public <V> V getShareCache(Object key, Supplier<V> supplier) {
        if (key == null) {
            return null;
        }
        CacheConfig config = getShareConfig();
        if (config == null) {
            return supplier != null ? supplier.get() : null;
        }
        return cacheService.compute(key, supplier, config);
    }

    /**
     * 获取指定名称的共享缓存值
     * <p>
     * 如果缓存未命中，会调用 getShareEntity(shareName, key) 获取数据并缓存。
     *
     * @param shareName 共享缓存名称
     * @param key       缓存key
     * @param <V>       值类型
     * @return 缓存值，不存在且 getShareEntity 返回null则返回null
     */
    public <V> V getShareCache(String shareName, Object key) {
        if (key == null || shareName == null) {
            return null;
        }
        CacheConfig config = getShareConfig(shareName);
        if (config == null) {
            return null;
        }
        return cacheService.compute(key, () -> getShareEntity(shareName, key), config);
    }

    /**
     * 获取指定名称的共享缓存值，如果不存在则通过supplier获取并缓存
     * <p>
     * 显式传入的 supplier 优先级高于 getShareEntity 方法。
     *
     * @param shareName 共享缓存名称
     * @param key       缓存key
     * @param supplier  值提供者，缓存未命中时调用（优先于 getShareEntity）
     * @param <V>       值类型
     * @return 缓存值
     */
    public <V> V getShareCache(String shareName, Object key, Supplier<V> supplier) {
        if (key == null || shareName == null) {
            return null;
        }
        CacheConfig config = getShareConfig(shareName);
        if (config == null) {
            return supplier != null ? supplier.get() : null;
        }
        return cacheService.compute(key, supplier, config);
    }

    /**
     * 获取使用默认缓存名称、指定Value类型的共享缓存值
     * <p>
     * 如果缓存未命中，会调用 getShareEntity(valueType, key) 获取数据并缓存。
     *
     * @param valueType 值类型
     * @param key       缓存key
     * @param <V>       值类型泛型
     * @return 缓存值
     */
    public <V> V getShareCache(Class<V> valueType, Object key) {
        if (key == null || valueType == null) {
            return null;
        }
        CacheConfig config = getShareConfig(getShareCacheName(valueType), valueType);
        if (config == null) {
            return null;
        }
        return cacheService.compute(key, () -> getShareEntity(valueType, key), config);
    }

    /**
     * 获取使用默认缓存名称、指定Value类型的共享缓存值，如果不存在则通过supplier获取并缓存
     *
     * @param valueType 值类型
     * @param key       缓存key
     * @param supplier  值提供者
     * @param <V>       值类型泛型
     * @return 缓存值
     */
    public <V> V getShareCache(Class<V> valueType, Object key, Supplier<V> supplier) {
        if (key == null || valueType == null) {
            return null;
        }
        CacheConfig config = getShareConfig(getShareCacheName(valueType), valueType);
        if (config == null) {
            return supplier != null ? supplier.get() : null;
        }
        return cacheService.compute(key, supplier, config);
    }

    /**
     * 获取指定名称和Value类型的共享缓存值
     * <p>
     * 如果缓存未命中，会调用 getShareEntity(shareName, valueType, key) 获取数据并缓存。
     * 使用独立的缓存配置，与其他类型的缓存隔离。
     *
     * @param shareName 共享缓存名称
     * @param valueType 值类型
     * @param key       缓存key
     * @param <V>       值类型泛型
     * @return 缓存值，不存在且 getShareEntity 返回null则返回null
     */
    public <V> V getShareCache(String shareName, Class<V> valueType, Object key) {
        if (key == null || shareName == null || valueType == null) {
            return null;
        }
        CacheConfig config = getShareConfig(shareName, valueType);
        if (config == null) {
            return null;
        }
        return cacheService.compute(key, () -> getShareEntity(shareName, valueType, key), config);
    }

    /**
     * 获取指定名称和Value类型的共享缓存值，如果不存在则通过supplier获取并缓存
     * <p>
     * 显式传入的 supplier 优先级高于 getShareEntity 方法。
     *
     * @param shareName 共享缓存名称
     * @param valueType 值类型
     * @param key       缓存key
     * @param supplier  值提供者，缓存未命中时调用（优先于 getShareEntity）
     * @param <V>       值类型泛型
     * @return 缓存值
     */
    public <V> V getShareCache(String shareName, Class<V> valueType, Object key, Supplier<V> supplier) {
        if (key == null || shareName == null || valueType == null) {
            return null;
        }
        CacheConfig config = getShareConfig(shareName, valueType);
        if (config == null) {
            return supplier != null ? supplier.get() : null;
        }
        return cacheService.compute(key, supplier, config);
    }


    // ==================== 获取共享缓存 - 单值可变参数 ====================

    /**
     * 获取共享缓存值（可变参数版本，使用默认共享缓存名称）
     * <p>
     * 多个 keys 会被组合为复合Key。如果只有一个参数，委托给单参数版本。
     * 缓存未命中时调用 getShareEntity(keys...) 获取数据。
     * <p>
     * 对应关系（类比 BaseCacheService）：
     * - BaseCacheService: getCache(keys...) → getEntity(keys...)
     * - ShareCacheService: getShareCache(keys...) → getShareEntity(keys...)
     *
     * @param keys 可变参数key
     * @param <V>  值类型
     * @return 缓存值
     */
    public <V> V getShareCache(Object... keys) {
        if (keys == null || keys.length == 0) {
            return null;
        }
        if (keys.length == 1) {
            return getShareCache(keys[0]);
        }
        CacheConfig config = getShareConfig();
        if (config == null) {
            return null;
        }
        String compositeKey = buildCompositeKey(keys);
        return cacheService.compute(compositeKey, () -> getShareEntity(keys), config);
    }

    /**
     * 获取指定名称的共享缓存值（可变参数版本）
     * <p>
     * 多个 keys 会被组合为复合Key。如果只有一个参数，委托给单参数版本。
     * 缓存未命中时调用 getShareEntity(shareName, keys...) 获取数据。
     *
     * @param shareName 共享缓存名称
     * @param keys      可变参数key
     * @param <V>       值类型
     * @return 缓存值
     */
    public <V> V getShareCache(String shareName, Object... keys) {
        if (keys == null || keys.length == 0 || shareName == null) {
            return null;
        }
        if (keys.length == 1) {
            return getShareCache(shareName, keys[0]);
        }
        CacheConfig config = getShareConfig(shareName);
        if (config == null) {
            return null;
        }
        String compositeKey = buildCompositeKey(keys);
        return cacheService.compute(compositeKey, () -> getShareEntity(shareName, keys), config);
    }

    /**
     * 获取使用默认缓存名称、指定Value类型的共享缓存值（可变参数版本）
     * <p>
     * 多个 keys 会被组合为复合Key。如果只有一个参数，委托给单参数版本。
     * 缓存未命中时调用 getShareEntity(valueType, keys...) 获取数据。
     *
     * @param valueType 值类型
     * @param keys      可变参数key
     * @param <V>       值类型泛型
     * @return 缓存值
     */
    public <V> V getShareCache(Class<V> valueType, Object... keys) {
        if (keys == null || keys.length == 0 || valueType == null) {
            return null;
        }
        if (keys.length == 1) {
            return getShareCache(valueType, keys[0]);
        }
        CacheConfig config = getShareConfig(getShareCacheName(valueType), valueType);
        if (config == null) {
            return null;
        }
        String compositeKey = buildCompositeKey(keys);
        return cacheService.compute(compositeKey, () -> getShareEntity(valueType, keys), config);
    }

    /**
     * 获取指定名称和Value类型的共享缓存值（可变参数版本）
     * <p>
     * 多个 keys 会被组合为复合Key。如果只有一个参数，委托给单参数版本。
     * 缓存未命中时调用 getShareEntity(shareName, valueType, keys...) 获取数据。
     *
     * @param shareName 共享缓存名称
     * @param valueType 值类型
     * @param keys      可变参数key
     * @param <V>       值类型泛型
     * @return 缓存值
     */
    public <V> V getShareCache(String shareName, Class<V> valueType, Object... keys) {
        if (keys == null || keys.length == 0 || shareName == null || valueType == null) {
            return null;
        }
        if (keys.length == 1) {
            return getShareCache(shareName, valueType, keys[0]);
        }
        CacheConfig config = getShareConfig(shareName, valueType);
        if (config == null) {
            return null;
        }
        String compositeKey = buildCompositeKey(keys);
        return cacheService.compute(compositeKey, () -> getShareEntity(shareName, valueType, keys), config);
    }

    // ==================== 获取共享缓存 - List ====================

    /**
     * 获取共享List缓存值（使用默认共享缓存名称）
     * <p>
     * 如果缓存未命中，会调用 getShareListEntity(key) 获取列表数据并缓存。
     *
     * @param key 缓存key
     * @param <V> 列表元素类型
     * @return 缓存的列表值，不存在且 getShareListEntity 返回null则返回null
     */
    public <V> List<V> getShareListCache(Object key) {
        if (key == null) {
            return null;
        }
        CacheConfig config = getShareListConfig();
        if (config == null) {
            return null;
        }
        return cacheService.compute(key, () -> getShareListEntity(key), config);
    }

    /**
     * 获取共享List缓存值，如果不存在则通过supplier获取并缓存（使用默认共享缓存名称）
     * <p>
     * 显式传入的 supplier 优先级高于 getShareListEntity 方法。
     *
     * @param key      缓存key
     * @param supplier 列表值提供者（优先于 getShareListEntity）
     * @param <V>      列表元素类型
     * @return 缓存的列表值
     */
    public <V> List<V> getShareListCache(Object key, Supplier<List<V>> supplier) {
        if (key == null) {
            return null;
        }
        CacheConfig config = getShareListConfig();
        if (config == null) {
            return supplier != null ? supplier.get() : null;
        }
        return cacheService.compute(key, supplier, config);
    }

    /**
     * 获取指定名称的共享List缓存值
     * <p>
     * 如果缓存未命中，会调用 getShareListEntity(shareName, key) 获取列表数据并缓存。
     *
     * @param shareName 共享缓存名称
     * @param key       缓存key
     * @param <V>       列表元素类型
     * @return 缓存的列表值，不存在且 getShareListEntity 返回null则返回null
     */
    public <V> List<V> getShareListCache(String shareName, Object key) {
        if (key == null || shareName == null) {
            return null;
        }
        CacheConfig config = getShareListConfig(shareName);
        if (config == null) {
            return null;
        }
        return cacheService.compute(key, () -> getShareListEntity(shareName, key), config);
    }

    /**
     * 获取指定名称的共享List缓存值，如果不存在则通过supplier获取并缓存
     * <p>
     * 显式传入的 supplier 优先级高于 getShareListEntity 方法。
     *
     * @param shareName 共享缓存名称
     * @param key       缓存key
     * @param supplier  列表值提供者（优先于 getShareListEntity）
     * @param <V>       列表元素类型
     * @return 缓存的列表值
     */
    public <V> List<V> getShareListCache(String shareName, Object key, Supplier<List<V>> supplier) {
        if (key == null || shareName == null) {
            return null;
        }
        CacheConfig config = getShareListConfig(shareName);
        if (config == null) {
            return supplier != null ? supplier.get() : null;
        }
        return cacheService.compute(key, supplier, config);
    }

    /**
     * 获取指定Value类型的共享List缓存值（使用默认名称）
     * <p>
     * 如果缓存未命中，会调用 getShareListEntity(valueType, key) 获取列表数据并缓存。
     * 使用独立的缓存配置，与其他类型的缓存隔离。
     *
     * @param valueType 列表元素类型
     * @param key       缓存key
     * @param <V>       列表元素类型泛型
     * @return 缓存的列表值
     */
    public <V> List<V> getShareListCache(Class<V> valueType, Object key) {
        if (key == null || valueType == null) {
            return null;
        }
        CacheConfig config = getShareListConfig(valueType);
        if (config == null) {
            return null;
        }
        return cacheService.compute(key, () -> getShareListEntity(valueType, key), config);
    }

    /**
     * 获取指定Value类型的共享List缓存值，如果不存在则通过supplier获取并缓存（使用默认名称）
     *
     * @param valueType 列表元素类型
     * @param key       缓存key
     * @param supplier  列表值提供者（优先于 getShareListEntity）
     * @param <V>       列表元素类型泛型
     * @return 缓存的列表值
     */
    public <V> List<V> getShareListCache(Class<V> valueType, Object key, Supplier<List<V>> supplier) {
        if (key == null || valueType == null) {
            return null;
        }
        CacheConfig config = getShareListConfig(valueType);
        if (config == null) {
            return supplier != null ? supplier.get() : null;
        }
        return cacheService.compute(key, supplier, config);
    }

    /**
     * 获取指定名称和Value类型的共享List缓存值
     * <p>
     * 如果缓存未命中，会调用 getShareListEntity(shareName, valueType, key) 获取列表数据并缓存。
     * 使用独立的缓存配置，与其他类型的缓存隔离。
     *
     * @param shareName 共享缓存名称
     * @param valueType 列表元素类型
     * @param key       缓存key
     * @param <V>       列表元素类型泛型
     * @return 缓存的列表值
     */
    public <V> List<V> getShareListCache(String shareName, Class<V> valueType, Object key) {
        if (key == null || shareName == null || valueType == null) {
            return null;
        }
        CacheConfig config = getShareListConfig(shareName, valueType);
        if (config == null) {
            return null;
        }
        return cacheService.compute(key, () -> getShareListEntity(shareName, valueType, key), config);
    }

    /**
     * 获取指定名称和Value类型的共享List缓存值，如果不存在则通过supplier获取并缓存
     *
     * @param shareName 共享缓存名称
     * @param valueType 列表元素类型
     * @param key       缓存key
     * @param supplier  列表值提供者（优先于 getShareListEntity）
     * @param <V>       列表元素类型泛型
     * @return 缓存的列表值
     */
    public <V> List<V> getShareListCache(String shareName, Class<V> valueType, Object key, Supplier<List<V>> supplier) {
        if (key == null || shareName == null || valueType == null) {
            return null;
        }
        CacheConfig config = getShareListConfig(shareName, valueType);
        if (config == null) {
            return supplier != null ? supplier.get() : null;
        }
        return cacheService.compute(key, supplier, config);
    }

    // ==================== 获取共享缓存 - List可变参数 ====================

    /**
     * 获取共享List缓存值（可变参数版本，使用默认共享缓存名称）
     * <p>
     * 多个 keys 会被组合为复合Key。如果只有一个参数，委托给单参数版本。
     * 缓存未命中时调用 getShareListEntity(keys...) 获取列表数据。
     *
     * @param keys 可变参数key
     * @param <V>  列表元素类型
     * @return 缓存的列表值
     */
    public <V> List<V> getShareListCache(Object... keys) {
        if (keys == null || keys.length == 0) {
            return null;
        }
        if (keys.length == 1) {
            return getShareListCache(keys[0]);
        }
        CacheConfig config = getShareListConfig();
        if (config == null) {
            return null;
        }
        String compositeKey = buildCompositeKey(keys);
        return cacheService.compute(compositeKey, () -> getShareListEntity(keys), config);
    }

    /**
     * 获取指定名称的共享List缓存值（可变参数版本）
     * <p>
     * 多个 keys 会被组合为复合Key。如果只有一个参数，委托给单参数版本。
     * 缓存未命中时调用 getShareListEntity(shareName, keys...) 获取列表数据。
     *
     * @param shareName 共享缓存名称
     * @param keys      可变参数key
     * @param <V>       列表元素类型
     * @return 缓存的列表值
     */
    public <V> List<V> getShareListCache(String shareName, Object... keys) {
        if (keys == null || keys.length == 0 || shareName == null) {
            return null;
        }
        if (keys.length == 1) {
            return getShareListCache(shareName, keys[0]);
        }
        CacheConfig config = getShareListConfig(shareName);
        if (config == null) {
            return null;
        }
        String compositeKey = buildCompositeKey(keys);
        return cacheService.compute(compositeKey, () -> getShareListEntity(shareName, keys), config);
    }

    /**
     * 获取指定Value类型的共享List缓存值（可变参数版本，使用默认名称）
     * <p>
     * 多个 keys 会被组合为复合Key。如果只有一个参数，委托给单参数版本。
     * 缓存未命中时调用 getShareListEntity(valueType, keys...) 获取列表数据。
     *
     * @param valueType 列表元素类型
     * @param keys      可变参数key
     * @param <V>       列表元素类型泛型
     * @return 缓存的列表值
     */
    public <V> List<V> getShareListCache(Class<V> valueType, Object... keys) {
        if (keys == null || keys.length == 0 || valueType == null) {
            return null;
        }
        if (keys.length == 1) {
            return getShareListCache(valueType, keys[0]);
        }
        CacheConfig config = getShareListConfig(valueType);
        if (config == null) {
            return null;
        }
        String compositeKey = buildCompositeKey(keys);
        return cacheService.compute(compositeKey, () -> getShareListEntity(valueType, keys), config);
    }

    /**
     * 获取指定名称和Value类型的共享List缓存值（可变参数版本）
     * <p>
     * 多个 keys 会被组合为复合Key。如果只有一个参数，委托给单参数版本。
     * 缓存未命中时调用 getShareListEntity(shareName, valueType, keys...) 获取列表数据。
     *
     * @param shareName 共享缓存名称
     * @param valueType 列表元素类型
     * @param keys      可变参数key
     * @param <V>       列表元素类型泛型
     * @return 缓存的列表值
     */
    public <V> List<V> getShareListCache(String shareName, Class<V> valueType, Object... keys) {
        if (keys == null || keys.length == 0 || shareName == null || valueType == null) {
            return null;
        }
        if (keys.length == 1) {
            return getShareListCache(shareName, valueType, keys[0]);
        }
        CacheConfig config = getShareListConfig(shareName, valueType);
        if (config == null) {
            return null;
        }
        String compositeKey = buildCompositeKey(keys);
        return cacheService.compute(compositeKey, () -> getShareListEntity(shareName, valueType, keys), config);
    }

    // ==================== 获取共享缓存 - Map便捷方法 ====================

    /**
     * 获取Map类型的共享缓存值（使用默认共享缓存名称）
     * <p>
     * 如果缓存未命中，会调用 getShareMapEntity(key) 获取Map数据并缓存。
     * 适用于不依赖实体类、使用Map存储结构化数据的场景。
     *
     * @param key 缓存key
     * @return Map类型的缓存值
     */
    public Map<String, Object> getShareMapCache(Object key) {
        if (key == null) {
            return null;
        }
        CacheConfig config = getShareConfig();
        if (config == null) {
            return null;
        }
        return cacheService.compute(key, () -> getShareMapEntity(key), config);
    }

    /**
     * 获取Map类型的共享缓存值，如果不存在则通过supplier获取并缓存
     *
     * @param key      缓存key
     * @param supplier Map值提供者
     * @return Map类型的缓存值
     */
    public Map<String, Object> getShareMapCache(Object key, Supplier<Map<String, Object>> supplier) {
        return getShareCache(key, supplier);
    }

    /**
     * 获取指定名称的Map类型共享缓存值
     * <p>
     * 如果缓存未命中，会调用 getShareMapEntity(shareName, key) 获取Map数据并缓存。
     *
     * @param shareName 共享缓存名称
     * @param key       缓存key
     * @return Map类型的缓存值
     */
    public Map<String, Object> getShareMapCache(String shareName, Object key) {
        if (key == null || shareName == null) {
            return null;
        }
        CacheConfig config = getShareConfig(shareName);
        if (config == null) {
            return null;
        }
        return cacheService.compute(key, () -> getShareMapEntity(shareName, key), config);
    }

    /**
     * 获取指定名称的Map类型共享缓存值，如果不存在则通过supplier获取并缓存
     *
     * @param shareName 共享缓存名称
     * @param key       缓存key
     * @param supplier  Map值提供者
     * @return Map类型的缓存值
     */
    public Map<String, Object> getShareMapCache(String shareName, Object key, Supplier<Map<String, Object>> supplier) {
        return getShareCache(shareName, key, supplier);
    }

    /**
     * 获取Map类型的共享缓存值（可变参数版本，使用默认共享缓存名称）
     * <p>
     * 多个 keys 会被组合为复合Key。如果只有一个参数，委托给单参数版本。
     * 缓存未命中时调用 getShareMapEntity(keys...) 获取Map数据。
     *
     * @param keys 可变参数key
     * @return Map类型的缓存值
     */
    public Map<String, Object> getShareMapCache(Object... keys) {
        if (keys == null || keys.length == 0) {
            return null;
        }
        if (keys.length == 1) {
            return getShareMapCache(keys[0]);
        }
        CacheConfig config = getShareConfig();
        if (config == null) {
            return null;
        }
        String compositeKey = buildCompositeKey(keys);
        return cacheService.compute(compositeKey, () -> getShareMapEntity(keys), config);
    }

    /**
     * 获取指定名称的Map类型共享缓存值（可变参数版本）
     * <p>
     * 多个 keys 会被组合为复合Key。如果只有一个参数，委托给单参数版本。
     * 缓存未命中时调用 getShareMapEntity(shareName, keys...) 获取Map数据。
     *
     * @param shareName 共享缓存名称
     * @param keys      可变参数key
     * @return Map类型的缓存值
     */
    public Map<String, Object> getShareMapCache(String shareName, Object... keys) {
        if (keys == null || keys.length == 0 || shareName == null) {
            return null;
        }
        if (keys.length == 1) {
            return getShareMapCache(shareName, keys[0]);
        }
        CacheConfig config = getShareConfig(shareName);
        if (config == null) {
            return null;
        }
        String compositeKey = buildCompositeKey(keys);
        return cacheService.compute(compositeKey, () -> getShareMapEntity(shareName, keys), config);
    }

    // ==================== 写入共享缓存 ====================

    /**
     * 写入共享缓存值（使用默认共享缓存名称）
     *
     * @param key   缓存key
     * @param value 缓存值
     * @param <V>   值类型
     */
    public <V> void putShareCache(Object key, V value) {
        if (key == null || value == null) {
            return;
        }
        CacheConfig config = getShareConfig();
        if (config == null) {
            return;
        }
        cacheService.put(config, key, value);
    }

    /**
     * 写入指定名称的共享缓存值
     *
     * @param shareName 共享缓存名称
     * @param key       缓存key
     * @param value     缓存值
     * @param <V>       值类型
     */
    public <V> void putShareCache(String shareName, Object key, V value) {
        if (key == null || value == null || shareName == null) {
            return;
        }
        CacheConfig config = getShareConfig(shareName);
        if (config == null) {
            return;
        }
        cacheService.put(config, key, value);
    }

    /**
     * 写入使用默认缓存名称、指定Value类型的共享缓存值
     * 使用独立的缓存配置，与其他类型的缓存隔离
     *
     * @param valueType 值类型
     * @param key       缓存key
     * @param value     缓存值
     * @param <V>       值类型泛型
     */
    public <V> void putShareCache(Class<V> valueType, Object key, V value) {
        if (key == null || value == null || valueType == null) {
            return;
        }
        CacheConfig config = getShareConfig(getShareCacheName(valueType), valueType);
        if (config == null) {
            return;
        }
        cacheService.put(config, key, value);
    }

    /**
     * 写入指定名称和Value类型的共享缓存值
     * 使用独立的缓存配置，与其他类型的缓存隔离
     *
     * @param shareName 共享缓存名称
     * @param valueType 值类型
     * @param key       缓存key
     * @param value     缓存值
     * @param <V>       值类型泛型
     */
    public <V> void putShareCache(String shareName, Class<V> valueType, Object key, V value) {
        if (key == null || value == null || shareName == null || valueType == null) {
            return;
        }
        CacheConfig config = getShareConfig(shareName, valueType);
        if (config == null) {
            return;
        }
        cacheService.put(config, key, value);
    }

    /**
     * 写入共享List缓存值（使用默认共享缓存名称）
     *
     * @param key   缓存key
     * @param value 列表值
     * @param <V>   列表元素类型
     */
    public <V> void putShareListCache(Object key, List<V> value) {
        if (key == null || value == null) {
            return;
        }
        CacheConfig config = getShareListConfig();
        if (config == null) {
            return;
        }
        cacheService.put(config, key, value);
    }

    /**
     * 写入指定名称的共享List缓存值
     *
     * @param shareName 共享缓存名称
     * @param key       缓存key
     * @param value     列表值
     * @param <V>       列表元素类型
     */
    public <V> void putShareListCache(String shareName, Object key, List<V> value) {
        if (key == null || value == null || shareName == null) {
            return;
        }
        CacheConfig config = getShareListConfig(shareName);
        if (config == null) {
            return;
        }
        cacheService.put(config, key, value);
    }

    /**
     * 写入指定Value类型的共享List缓存值（使用默认名称）
     * 使用独立的缓存配置，与其他类型的缓存隔离
     *
     * @param valueType 列表元素类型
     * @param key       缓存key
     * @param value     列表值
     * @param <V>       列表元素类型泛型
     */
    public <V> void putShareListCache(Class<V> valueType, Object key, List<V> value) {
        if (key == null || value == null || valueType == null) {
            return;
        }
        CacheConfig config = getShareListConfig(valueType);
        if (config == null) {
            return;
        }
        cacheService.put(config, key, value);
    }

    /**
     * 写入指定名称和Value类型的共享List缓存值
     * 使用独立的缓存配置，与其他类型的缓存隔离
     *
     * @param shareName 共享缓存名称
     * @param valueType 列表元素类型
     * @param key       缓存key
     * @param value     列表值
     * @param <V>       列表元素类型泛型
     */
    public <V> void putShareListCache(String shareName, Class<V> valueType, Object key, List<V> value) {
        if (key == null || value == null || shareName == null || valueType == null) {
            return;
        }
        CacheConfig config = getShareListConfig(shareName, valueType);
        if (config == null) {
            return;
        }
        cacheService.put(config, key, value);
    }

    /**
     * 写入Map类型的共享缓存值（使用默认共享缓存名称）
     *
     * @param key   缓存key
     * @param value Map值
     */
    public void putShareMapCache(Object key, Map<String, Object> value) {
        putShareCache(key, value);
    }

    /**
     * 写入指定名称的Map类型共享缓存值
     *
     * @param shareName 共享缓存名称
     * @param key       缓存key
     * @param value     Map值
     */
    public void putShareMapCache(String shareName, Object key, Map<String, Object> value) {
        putShareCache(shareName, key, value);
    }

    // ==================== 写入共享缓存 - 可变参数 ====================

    /**
     * 使用复合Key写入共享缓存值（使用默认共享缓存名称）
     * <p>
     * 多个 keys 会被组合为复合Key。如果只有一个key，委托给单参数版本。
     *
     * @param value 缓存值
     * @param keys  可变参数key
     * @param <V>   值类型
     */
    public <V> void putShareCache(V value, Object... keys) {
        if (keys == null || keys.length == 0 || value == null) {
            return;
        }
        if (keys.length == 1) {
            putShareCache(keys[0], value);
            return;
        }
        CacheConfig config = getShareConfig();
        if (config == null) {
            return;
        }
        String compositeKey = buildCompositeKey(keys);
        cacheService.put(config, compositeKey, value);
    }

    /**
     * 使用复合Key写入指定名称的共享缓存值
     * <p>
     * 多个 keys 会被组合为复合Key。如果只有一个key，委托给单参数版本。
     *
     * @param shareName 共享缓存名称
     * @param value     缓存值
     * @param keys      可变参数key
     * @param <V>       值类型
     */
    public <V> void putShareCache(String shareName, V value, Object... keys) {
        if (keys == null || keys.length == 0 || value == null || shareName == null) {
            return;
        }
        if (keys.length == 1) {
            putShareCache(shareName, keys[0], value);
            return;
        }
        CacheConfig config = getShareConfig(shareName);
        if (config == null) {
            return;
        }
        String compositeKey = buildCompositeKey(keys);
        cacheService.put(config, compositeKey, value);
    }

    /**
     * 使用复合Key写入默认名称、指定Value类型的共享缓存值
     * <p>
     * 多个 keys 会被组合为复合Key。如果只有一个key，委托给单参数版本。
     *
     * @param valueType 值类型
     * @param value     缓存值
     * @param keys      可变参数key
     * @param <V>       值类型泛型
     */
    public <V> void putShareCache(Class<V> valueType, V value, Object... keys) {
        if (keys == null || keys.length == 0 || value == null || valueType == null) {
            return;
        }
        if (keys.length == 1) {
            putShareCache(valueType, keys[0], value);
            return;
        }
        CacheConfig config = getShareConfig(getShareCacheName(valueType), valueType);
        if (config == null) {
            return;
        }
        String compositeKey = buildCompositeKey(keys);
        cacheService.put(config, compositeKey, value);
    }

    /**
     * 使用复合Key写入指定名称和Value类型的共享缓存值
     * <p>
     * 多个 keys 会被组合为复合Key。如果只有一个key，委托给单参数版本。
     *
     * @param shareName 共享缓存名称
     * @param valueType 值类型
     * @param value     缓存值
     * @param keys      可变参数key
     * @param <V>       值类型泛型
     */
    public <V> void putShareCache(String shareName, Class<V> valueType, V value, Object... keys) {
        if (keys == null || keys.length == 0 || value == null || shareName == null || valueType == null) {
            return;
        }
        if (keys.length == 1) {
            putShareCache(shareName, valueType, keys[0], value);
            return;
        }
        CacheConfig config = getShareConfig(shareName, valueType);
        if (config == null) {
            return;
        }
        String compositeKey = buildCompositeKey(keys);
        cacheService.put(config, compositeKey, value);
    }

    /**
     * 使用复合Key写入共享List缓存值（使用默认共享缓存名称）
     *
     * @param value 列表值
     * @param keys  可变参数key
     * @param <V>   列表元素类型
     */
    public <V> void putShareListCache(List<V> value, Object... keys) {
        if (keys == null || keys.length == 0 || value == null) {
            return;
        }
        if (keys.length == 1) {
            putShareListCache(keys[0], value);
            return;
        }
        CacheConfig config = getShareListConfig();
        if (config == null) {
            return;
        }
        String compositeKey = buildCompositeKey(keys);
        cacheService.put(config, compositeKey, value);
    }

    /**
     * 使用复合Key写入指定名称的共享List缓存值
     *
     * @param shareName 共享缓存名称
     * @param value     列表值
     * @param keys      可变参数key
     * @param <V>       列表元素类型
     */
    public <V> void putShareListCache(String shareName, List<V> value, Object... keys) {
        if (keys == null || keys.length == 0 || value == null || shareName == null) {
            return;
        }
        if (keys.length == 1) {
            putShareListCache(shareName, keys[0], value);
            return;
        }
        CacheConfig config = getShareListConfig(shareName);
        if (config == null) {
            return;
        }
        String compositeKey = buildCompositeKey(keys);
        cacheService.put(config, compositeKey, value);
    }

    /**
     * 使用复合Key写入默认名称、指定Value类型的共享List缓存值
     * <p>
     * 多个 keys 会被组合为复合Key。如果只有一个key，委托给单参数版本。
     *
     * @param valueType 列表元素类型
     * @param value     列表值
     * @param keys      可变参数key
     * @param <V>       列表元素类型泛型
     */
    public <V> void putShareListCache(Class<V> valueType, List<V> value, Object... keys) {
        if (keys == null || keys.length == 0 || value == null || valueType == null) {
            return;
        }
        if (keys.length == 1) {
            putShareListCache(valueType, keys[0], value);
            return;
        }
        CacheConfig config = getShareListConfig(valueType);
        if (config == null) {
            return;
        }
        String compositeKey = buildCompositeKey(keys);
        cacheService.put(config, compositeKey, value);
    }

    /**
     * 使用复合Key写入指定名称和Value类型的共享List缓存值
     * <p>
     * 多个 keys 会被组合为复合Key。如果只有一个key，委托给单参数版本。
     *
     * @param shareName 共享缓存名称
     * @param valueType 列表元素类型
     * @param value     列表值
     * @param keys      可变参数key
     * @param <V>       列表元素类型泛型
     */
    public <V> void putShareListCache(String shareName, Class<V> valueType, List<V> value, Object... keys) {
        if (keys == null || keys.length == 0 || value == null || shareName == null || valueType == null) {
            return;
        }
        if (keys.length == 1) {
            putShareListCache(shareName, valueType, keys[0], value);
            return;
        }
        CacheConfig config = getShareListConfig(shareName, valueType);
        if (config == null) {
            return;
        }
        String compositeKey = buildCompositeKey(keys);
        cacheService.put(config, compositeKey, value);
    }

    // ==================== 删除共享缓存 ====================

    /**
     * 删除共享缓存值（使用默认共享缓存名称）
     *
     * @param key 缓存key
     */
    public void removeShareCache(Object key) {
        if (key == null) {
            return;
        }
        CacheConfig config = getShareConfig();
        if (config == null) {
            return;
        }
        cacheService.remove(config.getName(), key);
    }

    /**
     * 删除指定名称的共享缓存值
     *
     * @param shareName 共享缓存名称
     * @param key       缓存key
     */
    public void removeShareCache(String shareName, Object key) {
        if (key == null || shareName == null) {
            return;
        }
        CacheConfig config = getShareConfig(shareName);
        if (config == null) {
            return;
        }
        cacheService.remove(config.getName(), key);
    }

    /**
     * 删除使用默认缓存名称、指定Value类型的共享缓存值
     *
     * @param valueType 值类型
     * @param key       缓存key
     * @param <V>       值类型泛型
     */
    public <V> void removeShareCache(Class<V> valueType, Object key) {
        if (key == null || valueType == null) {
            return;
        }
        CacheConfig config = getShareConfig(getShareCacheName(valueType), valueType);
        if (config == null) {
            return;
        }
        cacheService.remove(config.getName(), key);
    }

    /**
     * 删除指定名称和Value类型的共享缓存值
     *
     * @param shareName 共享缓存名称
     * @param valueType 值类型
     * @param key       缓存key
     * @param <V>       值类型泛型
     */
    public <V> void removeShareCache(String shareName, Class<V> valueType, Object key) {
        if (key == null || shareName == null || valueType == null) {
            return;
        }
        CacheConfig config = getShareConfig(shareName, valueType);
        if (config == null) {
            return;
        }
        cacheService.remove(config.getName(), key);
    }

    /**
     * 删除共享List缓存值（使用默认共享缓存名称）
     *
     * @param key 缓存key
     */
    public void removeShareListCache(Object key) {
        if (key == null) {
            return;
        }
        CacheConfig config = getShareListConfig();
        if (config == null) {
            return;
        }
        cacheService.remove(config.getName(), key);
    }

    /**
     * 删除指定名称的共享List缓存值
     *
     * @param shareName 共享缓存名称
     * @param key       缓存key
     */
    public void removeShareListCache(String shareName, Object key) {
        if (key == null || shareName == null) {
            return;
        }
        CacheConfig config = getShareListConfig(shareName);
        if (config == null) {
            return;
        }
        cacheService.remove(config.getName(), key);
    }

    /**
     * 删除指定Value类型的共享List缓存值（使用默认名称）
     *
     * @param valueType 列表元素类型
     * @param key       缓存key
     * @param <V>       列表元素类型泛型
     */
    public <V> void removeShareListCache(Class<V> valueType, Object key) {
        if (key == null || valueType == null) {
            return;
        }
        CacheConfig config = getShareListConfig(valueType);
        if (config == null) {
            return;
        }
        cacheService.remove(config.getName(), key);
    }

    /**
     * 删除指定名称和Value类型的共享List缓存值
     *
     * @param shareName 共享缓存名称
     * @param valueType 列表元素类型
     * @param key       缓存key
     * @param <V>       列表元素类型泛型
     */
    public <V> void removeShareListCache(String shareName, Class<V> valueType, Object key) {
        if (key == null || shareName == null || valueType == null) {
            return;
        }
        CacheConfig config = getShareListConfig(shareName, valueType);
        if (config == null) {
            return;
        }
        cacheService.remove(config.getName(), key);
    }

    /**
     * 同时删除共享缓存中的单值缓存和List缓存（使用默认共享缓存名称）
     * 确保数据一致性
     *
     * @param key 缓存key
     */
    public void removeShareCacheAll(Object key) {
        removeShareCache(key);
        removeShareListCache(key);
    }

    /**
     * 同时删除指定名称的共享缓存中的单值缓存和List缓存
     * 确保数据一致性
     *
     * @param shareName 共享缓存名称
     * @param key       缓存key
     */
    public void removeShareCacheAll(String shareName, Object key) {
        removeShareCache(shareName, key);
        removeShareListCache(shareName, key);
    }

    /**
     * 同时删除指定Value类型的共享缓存中的单值缓存和List缓存（使用默认名称）
     * 确保数据一致性
     *
     * @param valueType 值类型
     * @param key       缓存key
     * @param <V>       值类型泛型
     */
    public <V> void removeShareCacheAll(Class<V> valueType, Object key) {
        removeShareCache(valueType, key);
        removeShareListCache(valueType, key);
    }

    /**
     * 同时删除指定名称和Value类型的共享缓存中的单值缓存和List缓存
     * 确保数据一致性
     *
     * @param shareName 共享缓存名称
     * @param valueType 值类型
     * @param key       缓存key
     * @param <V>       值类型泛型
     */
    public <V> void removeShareCacheAll(String shareName, Class<V> valueType, Object key) {
        removeShareCache(shareName, valueType, key);
        removeShareListCache(shareName, valueType, key);
    }

    // ==================== 删除共享缓存 - 可变参数 ====================

    /**
     * 使用复合Key删除共享缓存值（使用默认共享缓存名称）
     * <p>
     * 多个 keys 会被组合为复合Key。如果只有一个key，委托给单参数版本。
     *
     * @param keys 可变参数key
     */
    public void removeShareCache(Object... keys) {
        if (keys == null || keys.length == 0) {
            return;
        }
        if (keys.length == 1) {
            removeShareCache(keys[0]);
            return;
        }
        CacheConfig config = getShareConfig();
        if (config == null) {
            return;
        }
        String compositeKey = buildCompositeKey(keys);
        cacheService.remove(config.getName(), compositeKey);
    }

    /**
     * 使用复合Key删除指定名称的共享缓存值
     *
     * @param shareName 共享缓存名称
     * @param keys      可变参数key
     */
    public void removeShareCache(String shareName, Object... keys) {
        if (keys == null || keys.length == 0 || shareName == null) {
            return;
        }
        if (keys.length == 1) {
            removeShareCache(shareName, keys[0]);
            return;
        }
        CacheConfig config = getShareConfig(shareName);
        if (config == null) {
            return;
        }
        String compositeKey = buildCompositeKey(keys);
        cacheService.remove(config.getName(), compositeKey);
    }

    /**
     * 使用复合Key删除默认名称、指定Value类型的共享缓存值
     * <p>
     * 多个 keys 会被组合为复合Key。如果只有一个key，委托给单参数版本。
     *
     * @param valueType 值类型
     * @param keys      可变参数key
     * @param <V>       值类型泛型
     */
    public <V> void removeShareCache(Class<V> valueType, Object... keys) {
        if (keys == null || keys.length == 0 || valueType == null) {
            return;
        }
        if (keys.length == 1) {
            removeShareCache(valueType, keys[0]);
            return;
        }
        CacheConfig config = getShareConfig(getShareCacheName(valueType), valueType);
        if (config == null) {
            return;
        }
        String compositeKey = buildCompositeKey(keys);
        cacheService.remove(config.getName(), compositeKey);
    }

    /**
     * 使用复合Key删除指定名称和Value类型的共享缓存值
     * <p>
     * 多个 keys 会被组合为复合Key。如果只有一个key，委托给单参数版本。
     *
     * @param shareName 共享缓存名称
     * @param valueType 值类型
     * @param keys      可变参数key
     * @param <V>       值类型泛型
     */
    public <V> void removeShareCache(String shareName, Class<V> valueType, Object... keys) {
        if (keys == null || keys.length == 0 || shareName == null || valueType == null) {
            return;
        }
        if (keys.length == 1) {
            removeShareCache(shareName, valueType, keys[0]);
            return;
        }
        CacheConfig config = getShareConfig(shareName, valueType);
        if (config == null) {
            return;
        }
        String compositeKey = buildCompositeKey(keys);
        cacheService.remove(config.getName(), compositeKey);
    }

    /**
     * 使用复合Key删除共享List缓存值（使用默认共享缓存名称）
     *
     * @param keys 可变参数key
     */
    public void removeShareListCache(Object... keys) {
        if (keys == null || keys.length == 0) {
            return;
        }
        if (keys.length == 1) {
            removeShareListCache(keys[0]);
            return;
        }
        CacheConfig config = getShareListConfig();
        if (config == null) {
            return;
        }
        String compositeKey = buildCompositeKey(keys);
        cacheService.remove(config.getName(), compositeKey);
    }

    /**
     * 使用复合Key删除指定名称的共享List缓存值
     *
     * @param shareName 共享缓存名称
     * @param keys      可变参数key
     */
    public void removeShareListCache(String shareName, Object... keys) {
        if (keys == null || keys.length == 0 || shareName == null) {
            return;
        }
        if (keys.length == 1) {
            removeShareListCache(shareName, keys[0]);
            return;
        }
        CacheConfig config = getShareListConfig(shareName);
        if (config == null) {
            return;
        }
        String compositeKey = buildCompositeKey(keys);
        cacheService.remove(config.getName(), compositeKey);
    }

    /**
     * 使用复合Key删除指定Value类型的共享List缓存值（使用默认名称）
     * <p>
     * 多个 keys 会被组合为复合Key。如果只有一个key，委托给单参数版本。
     *
     * @param valueType 列表元素类型
     * @param keys      可变参数key
     * @param <V>       列表元素类型泛型
     */
    public <V> void removeShareListCache(Class<V> valueType, Object... keys) {
        if (keys == null || keys.length == 0 || valueType == null) {
            return;
        }
        if (keys.length == 1) {
            removeShareListCache(valueType, keys[0]);
            return;
        }
        CacheConfig config = getShareListConfig(valueType);
        if (config == null) {
            return;
        }
        String compositeKey = buildCompositeKey(keys);
        cacheService.remove(config.getName(), compositeKey);
    }

    /**
     * 使用复合Key删除指定名称和Value类型的共享List缓存值
     * <p>
     * 多个 keys 会被组合为复合Key。如果只有一个key，委托给单参数版本。
     *
     * @param shareName 共享缓存名称
     * @param valueType 列表元素类型
     * @param keys      可变参数key
     * @param <V>       列表元素类型泛型
     */
    public <V> void removeShareListCache(String shareName, Class<V> valueType, Object... keys) {
        if (keys == null || keys.length == 0 || shareName == null || valueType == null) {
            return;
        }
        if (keys.length == 1) {
            removeShareListCache(shareName, valueType, keys[0]);
            return;
        }
        CacheConfig config = getShareListConfig(shareName, valueType);
        if (config == null) {
            return;
        }
        String compositeKey = buildCompositeKey(keys);
        cacheService.remove(config.getName(), compositeKey);
    }

    /**
     * 使用复合Key同时删除共享缓存中的单值缓存和List缓存（使用默认共享缓存名称）
     *
     * @param keys 可变参数key
     */
    public void removeShareCacheAll(Object... keys) {
        removeShareCache(keys);
        removeShareListCache(keys);
    }

    /**
     * 使用复合Key同时删除指定名称的共享缓存中的单值缓存和List缓存
     *
     * @param shareName 共享缓存名称
     * @param keys      可变参数key
     */
    public void removeShareCacheAll(String shareName, Object... keys) {
        removeShareCache(shareName, keys);
        removeShareListCache(shareName, keys);
    }

    /**
     * 使用复合Key同时删除指定Value类型的共享缓存中的单值缓存和List缓存（使用默认名称）
     *
     * @param valueType 值类型
     * @param keys      可变参数key
     * @param <V>       值类型泛型
     */
    public <V> void removeShareCacheAll(Class<V> valueType, Object... keys) {
        removeShareCache(valueType, keys);
        removeShareListCache(valueType, keys);
    }

    /**
     * 使用复合Key同时删除指定名称和Value类型的共享缓存中的单值缓存和List缓存
     *
     * @param shareName 共享缓存名称
     * @param valueType 值类型
     * @param keys      可变参数key
     * @param <V>       值类型泛型
     */
    public <V> void removeShareCacheAll(String shareName, Class<V> valueType, Object... keys) {
        removeShareCache(shareName, valueType, keys);
        removeShareListCache(shareName, valueType, keys);
    }

    // ==================== 清空共享缓存 ====================

    /**
     * 清空共享缓存（使用默认共享缓存名称）
     */
    public void clearShareCache() {
        CacheConfig config = getShareConfig();
        if (config == null) {
            return;
        }
        cacheService.clear(config.getName());
    }

    /**
     * 清空指定名称的共享缓存
     *
     * @param shareName 共享缓存名称
     */
    public void clearShareCache(String shareName) {
        if (shareName == null) {
            return;
        }
        CacheConfig config = getShareConfig(shareName);
        if (config == null) {
            return;
        }
        cacheService.clear(config.getName());
    }

    /**
     * 清空指定Value类型的共享缓存（使用默认名称）
     *
     * @param valueType 值类型
     * @param <V>       值类型泛型
     */
    public <V> void clearShareCache(Class<V> valueType) {
        if (valueType == null) {
            return;
        }
        CacheConfig config = getShareConfig(getShareCacheName(valueType), valueType);
        if (config == null) {
            return;
        }
        cacheService.clear(config.getName());
    }

    /**
     * 清空指定名称和Value类型的共享缓存
     *
     * @param shareName 共享缓存名称
     * @param valueType 值类型
     * @param <V>       值类型泛型
     */
    public <V> void clearShareCache(String shareName, Class<V> valueType) {
        if (shareName == null || valueType == null) {
            return;
        }
        CacheConfig config = getShareConfig(shareName, valueType);
        if (config == null) {
            return;
        }
        cacheService.clear(config.getName());
    }

    /**
     * 清空共享List缓存（使用默认共享缓存名称）
     */
    public void clearShareListCache() {
        CacheConfig config = getShareListConfig();
        if (config == null) {
            return;
        }
        cacheService.clear(config.getName());
    }

    /**
     * 清空指定名称的共享List缓存
     *
     * @param shareName 共享缓存名称
     */
    public void clearShareListCache(String shareName) {
        if (shareName == null) {
            return;
        }
        CacheConfig config = getShareListConfig(shareName);
        if (config == null) {
            return;
        }
        cacheService.clear(config.getName());
    }

    /**
     * 清空指定Value类型的共享List缓存（使用默认名称）
     *
     * @param valueType 列表元素类型
     * @param <V>       列表元素类型泛型
     */
    public <V> void clearShareListCache(Class<V> valueType) {
        if (valueType == null) {
            return;
        }
        CacheConfig config = getShareListConfig(valueType);
        if (config == null) {
            return;
        }
        cacheService.clear(config.getName());
    }

    /**
     * 清空指定名称和Value类型的共享List缓存
     *
     * @param shareName 共享缓存名称
     * @param valueType 列表元素类型
     * @param <V>       列表元素类型泛型
     */
    public <V> void clearShareListCache(String shareName, Class<V> valueType) {
        if (shareName == null || valueType == null) {
            return;
        }
        CacheConfig config = getShareListConfig(shareName, valueType);
        if (config == null) {
            return;
        }
        cacheService.clear(config.getName());
    }

    /**
     * 清空所有共享缓存（包括单值缓存和List缓存，使用默认共享缓存名称）
     */
    public void clearShareCacheAll() {
        clearShareCache();
        clearShareListCache();
    }

    /**
     * 清空指定名称的所有共享缓存（包括单值缓存和List缓存）
     *
     * @param shareName 共享缓存名称
     */
    public void clearShareCacheAll(String shareName) {
        clearShareCache(shareName);
        clearShareListCache(shareName);
    }

    /**
     * 清空指定Value类型的所有共享缓存（包括单值缓存和List缓存，使用默认名称）
     *
     * @param valueType 值类型
     * @param <V>       值类型泛型
     */
    public <V> void clearShareCacheAll(Class<V> valueType) {
        clearShareCache(valueType);
        clearShareListCache(valueType);
    }

    /**
     * 清空指定名称和Value类型的所有共享缓存（包括单值缓存和List缓存）
     *
     * @param shareName 共享缓存名称
     * @param valueType 值类型
     * @param <V>       值类型泛型
     */
    public <V> void clearShareCacheAll(String shareName, Class<V> valueType) {
        clearShareCache(shareName, valueType);
        clearShareListCache(shareName, valueType);
    }
}
