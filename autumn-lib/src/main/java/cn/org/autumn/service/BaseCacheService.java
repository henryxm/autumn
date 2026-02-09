package cn.org.autumn.service;

import cn.org.autumn.annotation.Cache;
import cn.org.autumn.annotation.Caches;
import cn.org.autumn.config.CacheConfig;
import cn.org.autumn.model.CacheParam;
import cn.org.autumn.table.utils.Escape;
import cn.org.autumn.table.utils.HumpConvert;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public abstract class BaseCacheService<M extends BaseMapper<T>, T> extends BaseQueueService<M, T> {

    @Autowired
    protected CacheService cacheService;

    static final Map<String, CacheConfig> configs = new ConcurrentHashMap<>();

    public long getCacheExpire() {
        return 10;
    }

    public long getCacheExpire(String naming) {
        return 10;
    }

    public <X> long getCacheExpire(Class<X> clazz, String naming) {
        return 10;
    }

    public <X> long getCacheExpire(Class<X> clazz) {
        return 10;
    }

    public boolean isCacheNull() {
        return false;
    }

    public boolean isCacheNull(String naming) {
        return false;
    }

    public <X> boolean isCacheNull(Class<X> clazz) {
        return false;
    }

    public <X> boolean isCacheNull(Class<X> clazz, String naming) {
        return false;
    }

    /**
     * 获取缓存Key的类型
     * 1. 如果类上有@Cache注解（复合字段缓存），返回String.class
     * 2. 如果类上有@Caches注解（多个复合字段缓存），返回String.class
     * 3. 如果字段上有@Cache注解，返回该字段的类型
     * 4. 如果字段类型是复合类型（数组、集合、Map等），返回String.class
     * 5. 否则返回字段的类型
     *
     * @return Key类型
     */
    public Class<?> getCacheKeyType() {
        Class<?> clazz = getModelClass();
        if (clazz == null) {
            return String.class;
        }
        // 检查类上是否有 @Cache 注解（复合字段缓存）
        Cache classCache = clazz.getAnnotation(Cache.class);
        if (classCache != null && classCache.value().length > 0) {
            // 复合字段缓存，使用String类型作为key
            return String.class;
        }
        // 检查类上是否有 @Caches 注解（多个复合字段缓存）
        Caches caches = clazz.getAnnotation(Caches.class);
        if (caches != null && caches.value().length > 0) {
            // 多个复合字段缓存，使用String类型作为key
            return String.class;
        }
        // 查找字段上的 @Cache 注解（单个字段缓存）
        Field cacheField = findCacheField(clazz);
        if (cacheField == null) {
            // 如果没有找到@Cache注解的字段，默认使用String类型
            return String.class;
        }
        Class<?> fieldType = cacheField.getType();
        // 判断是否是复合类型
        if (isCompositeType(fieldType)) {
            return String.class;
        }
        return fieldType;
    }

    /**
     * 根据name获取缓存Key的类型
     * 1. 如果类上有@Caches注解，查找指定name的复合字段缓存，返回String.class
     * 2. 如果字段上有@Cache注解，返回该字段的类型
     * 3. 如果字段类型是复合类型（数组、集合、Map等），返回String.class
     * 4. 否则返回字段的类型
     *
     * @param name name属性值
     * @return Key类型
     */
    public Class<?> getCacheKeyType(String name) {
        Class<?> clazz = getModelClass();
        if (clazz == null) {
            return String.class;
        }
        // 检查类上是否有 @Caches 注解（多个复合字段缓存）
        Caches caches = clazz.getAnnotation(Caches.class);
        if (caches != null) {
            // 查找指定name的复合字段缓存
            for (Cache cache : caches.value()) {
                if (cache.value().length > 0 && name != null && name.equals(cache.name())) {
                    // 复合字段缓存，使用String类型作为key
                    return String.class;
                }
            }
        }
        // 查找字段上的 @Cache 注解（单个字段缓存）
        Field cacheField = findCacheField(clazz, name);
        if (cacheField == null) {
            // 如果没有找到@Cache注解的字段，默认使用String类型
            return String.class;
        }
        Class<?> fieldType = cacheField.getType();
        // 判断是否是复合类型
        if (isCompositeType(fieldType)) {
            return String.class;
        }
        return fieldType;
    }

    /**
     * 判断是否是复合类型
     * 复合类型包括：数组、集合（Collection）、Map等
     *
     * @param type 类型
     * @return 如果是复合类型返回true，否则返回false
     */
    private boolean isCompositeType(Class<?> type) {
        if (type == null) {
            return false;
        }
        // 数组类型
        if (type.isArray()) {
            return true;
        }
        // 集合类型
        if (Collection.class.isAssignableFrom(type)) {
            return true;
        }
        // Map类型
        return Map.class.isAssignableFrom(type);
    }

    /**
     * 获取当前实体的缓存名称前缀
     * 所有与该实体相关的缓存配置名称都以此前缀开头
     *
     * @return 实体缓存名称前缀
     */
    protected String getEntityBaseName() {
        Class<?> clazz = getModelClass();
        if (clazz == null) {
            return null;
        }
        return clazz.getSimpleName().replace("Entity", "").toLowerCase();
    }

    public CacheConfig getConfig() {
        String name = getEntityBaseName();
        CacheConfig config = configs.get(name);
        if (null == config) {
            config = CacheConfig.builder().name(name).key(getCacheKeyType()).value(getModelClass()).expire(getCacheExpire()).Null(isCacheNull()).build();
            configs.put(name, config);
        }
        return config;
    }

    /**
     * 获取自定义类型的缓存配置
     * 缓存名称格式为：{entityName}{clazzName}
     *
     * @param clazz 自定义类型
     * @param <X>   类型泛型
     * @return 缓存配置
     */
    public <X> CacheConfig getConfig(Class<X> clazz) {
        String name = getEntityBaseName();
        name += clazz.getSimpleName().toLowerCase();
        CacheConfig config = configs.get(name);
        if (null == config) {
            config = CacheConfig.builder().name(name).key(getCacheKeyType()).value(clazz).expire(getCacheExpire(clazz)).Null(isCacheNull(clazz)).build();
            configs.put(name, config);
        }
        return config;
    }

    /**
     * 获取自定义类型和命名缓存的组合配置
     * 缓存名称格式为：{entityName}{naming}{clazzName}
     * 用于支持 getNameCache(Class<X> clazz, String name, Object key) 等方法
     *
     * @param clazz  自定义类型
     * @param naming name属性值，如果为空则使用默认配置
     * @param <X>    类型泛型
     * @return 缓存配置
     */
    public <X> CacheConfig getConfig(Class<X> clazz, String naming) {
        // 如果naming为空，使用默认的自定义类型配置
        if (naming == null || naming.isEmpty()) {
            return getConfig(clazz);
        }
        String name = getEntityBaseName() + naming;
        name = name.toLowerCase() + clazz.getSimpleName().toLowerCase();
        CacheConfig config = configs.get(name);
        if (null == config) {
            config = CacheConfig.builder().name(name).key(getCacheKeyType(naming)).value(clazz).expire(getCacheExpire(clazz, naming)).Null(isCacheNull(clazz, naming)).build();
            configs.put(name, config);
        }
        return config;
    }

    /**
     * 根据name属性获取缓存配置
     * 如果name为空或null，返回默认配置（等同于getConfig()）
     * 支持@Caches注解中定义的复合key缓存配置
     *
     * @param naming name属性值，如果为空则使用默认配置
     * @return 缓存配置
     */
    public CacheConfig getConfig(String naming) {
        // 如果name为空，使用默认配置
        if (naming == null || naming.isEmpty()) {
            return getConfig();
        }
        String name = getEntityBaseName() + naming;
        name = name.toLowerCase();
        CacheConfig config = configs.get(name);
        if (null == config) {
            config = CacheConfig.builder().name(name).key(getCacheKeyType(naming)).value(getModelClass()).expire(getCacheExpire(naming)).Null(isCacheNull(naming)).build();
            configs.put(name, config);
        }
        return config;
    }

    /**
     * 获取非唯一缓存的配置（用于List列表缓存）
     * 缓存名称后缀添加 "list" 以区分唯一和非唯一缓存
     *
     * @return 非唯一缓存配置
     */
    public CacheConfig getListConfig() {
        String name = getEntityBaseName() + "list";
        CacheConfig config = configs.get(name);
        if (null == config) {
            config = CacheConfig.builder().name(name).key(getCacheKeyType()).value(List.class).expire(getCacheExpire()).Null(isCacheNull()).build();
            configs.put(name, config);
        }
        return config;
    }

    /**
     * 获取自定义类型的非唯一缓存配置（用于List列表缓存）
     * 缓存名称格式为：{entityName}list{clazzName}
     *
     * @param clazz 自定义类型
     * @param <X>   类型泛型
     * @return 非唯一缓存配置
     */
    public <X> CacheConfig getListConfig(Class<X> clazz) {
        String name = getEntityBaseName() + "list";
        name += clazz.getSimpleName().toLowerCase();
        CacheConfig config = configs.get(name);
        if (null == config) {
            config = CacheConfig.builder().name(name).key(getCacheKeyType()).value(List.class).expire(getCacheExpire(clazz)).Null(isCacheNull(clazz)).build();
            configs.put(name, config);
        }
        return config;
    }

    /**
     * 获取自定义类型和命名缓存的组合 List 配置
     * 缓存名称格式为：{entityName}{naming}list{clazzName}
     *
     * @param clazz  自定义类型
     * @param naming name属性值，如果为空则使用默认配置
     * @param <X>    类型泛型
     * @return 非唯一缓存配置
     */
    public <X> CacheConfig getListConfig(Class<X> clazz, String naming) {
        // 如果naming为空，使用默认的自定义类型 List 配置
        if (naming == null || naming.isEmpty()) {
            return getListConfig(clazz);
        }
        String name = getEntityBaseName() + naming + "list";
        name = name.toLowerCase() + clazz.getSimpleName().toLowerCase();
        CacheConfig config = configs.get(name);
        if (null == config) {
            config = CacheConfig.builder().name(name).key(getCacheKeyType(naming)).value(List.class).expire(getCacheExpire(clazz, naming)).Null(isCacheNull(clazz, naming)).build();
            configs.put(name, config);
        }
        return config;
    }

    /**
     * 根据name属性获取非唯一缓存的配置（用于List列表缓存）
     * 缓存名称后缀添加 "list" 以区分唯一和非唯一缓存
     *
     * @param naming name属性值，如果为空则使用默认配置
     * @return 非唯一缓存配置
     */
    public CacheConfig getListConfig(String naming) {
        if (naming == null || naming.isEmpty()) {
            return getListConfig();
        }
        String name = getEntityBaseName() + naming + "list";
        name = name.toLowerCase();
        CacheConfig config = configs.get(name);
        if (null == config) {
            config = CacheConfig.builder().name(name).key(getCacheKeyType(naming)).value(List.class).expire(getCacheExpire(naming)).Null(isCacheNull(naming)).build();
            configs.put(name, config);
        }
        return config;
    }

    /**
     * 查找类上的@Caches注解
     *
     * @param clazz 实体类
     * @return Caches注解，如果不存在返回null
     */
    private Caches findCachesAnnotation(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }
        return clazz.getAnnotation(Caches.class);
    }

    /**
     * 查找@Caches注解中指定name的@Cache注解
     *
     * @param clazz 实体类
     * @param name  name属性值
     * @return Cache注解，如果不存在返回null
     */
    private Cache findCacheInCaches(Class<?> clazz, String name) {
        if (clazz == null || name == null || name.isEmpty()) {
            return null;
        }
        Caches caches = findCachesAnnotation(clazz);
        if (caches != null) {
            for (Cache cache : caches.value()) {
                if (cache.value().length > 0 && name.equals(cache.name())) {
                    return cache;
                }
            }
        }
        return null;
    }

    /**
     * 查找类上的@Cache注解（单个复合key，向前兼容）
     *
     * @param clazz 实体类
     * @return Cache注解，如果不存在返回null
     */
    private Cache findSingleCacheAnnotation(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }
        Cache classCache = clazz.getAnnotation(Cache.class);
        if (classCache != null && classCache.value().length > 0) {
            return classCache;
        }
        return null;
    }

    /**
     * 获取所有@Caches注解中定义的复合key缓存配置
     *
     * @param clazz 实体类
     * @return Cache注解数组，key为name属性值，value为Cache注解
     */
    private Map<String, Cache> findAllCompositeCaches(Class<?> clazz) {
        Map<String, Cache> compositeCaches = new HashMap<>();
        if (clazz == null) {
            return compositeCaches;
        }
        // 检查@Caches注解
        Caches caches = findCachesAnnotation(clazz);
        if (caches != null) {
            for (Cache cache : caches.value()) {
                if (cache.value().length > 0) {
                    String name = cache.name();
                    if (!name.isEmpty()) {
                        compositeCaches.put(name, cache);
                    }
                }
            }
        }
        // 检查单个@Cache注解（向前兼容，不带name）
        Cache singleCache = findSingleCacheAnnotation(clazz);
        if (singleCache != null) {
            // 单个@Cache注解不带name，使用空字符串作为key
            compositeCaches.put("", singleCache);
        }
        return compositeCaches;
    }

    @Override
    public boolean insertOrUpdate(T entity) {
        removeCacheByEntity(entity);
        return super.insertOrUpdate(entity);
    }

    @Override
    public boolean insertOrUpdateAllColumn(T entity) {
        removeCacheByEntity(entity);
        return super.insertOrUpdateAllColumn(entity);
    }

    @Override
    public boolean insertOrUpdateBatch(List<T> entityList) {
        for (T entity : entityList) {
            removeCacheByEntity(entity);
        }
        return super.insertOrUpdateBatch(entityList);
    }

    @Override
    public boolean insertOrUpdateBatch(List<T> entityList, int batchSize) {
        for (T entity : entityList) {
            removeCacheByEntity(entity);
        }
        return super.insertOrUpdateBatch(entityList, batchSize);
    }

    @Override
    public boolean insertOrUpdateAllColumnBatch(List<T> entityList) {
        for (T entity : entityList) {
            removeCacheByEntity(entity);
        }
        return super.insertOrUpdateAllColumnBatch(entityList);
    }

    @Override
    public boolean insertOrUpdateAllColumnBatch(List<T> entityList, int batchSize) {
        for (T entity : entityList) {
            removeCacheByEntity(entity);
        }
        return super.insertOrUpdateAllColumnBatch(entityList, batchSize);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean deleteById(Serializable id) {
        boolean result = super.deleteById(id);
        if (result) {
            // 检查 id 是否是实体类型 T 的实例
            Class<?> clazz = getModelClass();
            if (clazz != null && clazz.isInstance(id)) {
                // 如果是实体类型，调用 removeCacheByEntity
                removeCacheByEntity((T) id);
            } else {
                // 否则，使用 id 作为 key 删除缓存
                removeCacheByKey(id);
            }
        }
        return result;
    }

    @Override
    public boolean deleteByMap(Map<String, Object> columnMap) {
        // 先查询要删除的实体，以便删除缓存
        List<T> entities = selectByMap(columnMap);
        boolean result = super.deleteByMap(columnMap);
        if (result && entities != null) {
            for (T entity : entities) {
                removeCacheByEntity(entity);
            }
        }
        return result;
    }

    @Override
    public boolean delete(Wrapper<T> wrapper) {
        // 先查询要删除的实体，以便删除缓存
        List<T> entities = selectList(wrapper);
        boolean result = super.delete(wrapper);
        if (result && entities != null) {
            for (T entity : entities) {
                removeCacheByEntity(entity);
            }
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean deleteBatchIds(Collection<? extends Serializable> idList) {
        boolean result = super.deleteBatchIds(idList);
        if (result && idList != null) {
            Class<?> clazz = getModelClass();
            for (Serializable id : idList) {
                // 检查 id 是否是实体类型 T 的实例
                if (clazz != null && clazz.isInstance(id)) {
                    // 如果是实体类型，调用 removeCacheByEntity
                    removeCacheByEntity((T) id);
                } else {
                    // 否则，使用 id 作为 key 删除缓存
                    removeCacheByKey(id);
                }
            }
        }
        return result;
    }

    @Override
    public boolean updateById(T entity) {
        // 更新前先删除缓存
        removeCacheByEntity(entity);
        return super.updateById(entity);
    }

    @Override
    public boolean updateAllColumnById(T entity) {
        // 更新前先删除缓存
        removeCacheByEntity(entity);
        return super.updateAllColumnById(entity);
    }

    @Override
    public boolean update(T entity, Wrapper<T> wrapper) {
        // 更新前先删除缓存
        removeCacheByEntity(entity);
        return super.update(entity, wrapper);
    }

    @Override
    public boolean updateBatchById(List<T> entityList) {
        // 更新前先删除缓存
        if (entityList != null) {
            for (T entity : entityList) {
                removeCacheByEntity(entity);
            }
        }
        return super.updateBatchById(entityList);
    }

    @Override
    public boolean updateBatchById(List<T> entityList, int batchSize) {
        // 更新前先删除缓存
        if (entityList != null) {
            for (T entity : entityList) {
                removeCacheByEntity(entity);
            }
        }
        return super.updateBatchById(entityList, batchSize);
    }

    @Override
    public boolean updateAllColumnBatchById(List<T> entityList) {
        // 更新前先删除缓存
        if (entityList != null) {
            for (T entity : entityList) {
                removeCacheByEntity(entity);
            }
        }
        return super.updateAllColumnBatchById(entityList);
    }

    @Override
    public boolean updateAllColumnBatchById(List<T> entityList, int batchSize) {
        // 更新前先删除缓存
        if (entityList != null) {
            for (T entity : entityList) {
                removeCacheByEntity(entity);
            }
        }
        return super.updateAllColumnBatchById(entityList, batchSize);
    }

    /**
     * 获取缓存，如果不存在则查询并缓存（单个参数版本，高效）
     * 获取一条缓存值，如果未能获取到该值，则调用getEntity函数，通过查询数据库获取，实现类可以实现getEntity来提高效率
     * 也可以通过@Cache来标注其字段，通过反射来获取对应字段的实例
     * <p>
     * 注意：此方法要求 @Cache 注解的 unique 属性为 true（默认值）。
     * 如果需要获取非唯一字段的数据列表，请使用 getListCache 方法。
     *
     * @param key 缓存 key（单个值）
     * @return 实体对象
     * @throws IllegalStateException 如果 @Cache 注解的 unique=false
     */
    public T getCache(Object key) {
        if (key == null) {
            return null;
        }
        // 校验 unique 必须为 true
        checkCacheUnique(true);
        // 构建缓存 key
        Object cacheKey = buildCacheKey(key);
        if (cacheKey == null) {
            return null;
        }
        // 调用单参数版本的 getEntity
        return cacheService.compute(cacheKey, () -> getEntity(key), getConfig());
    }

    /**
     * 获取自定义类型的缓存，如果不存在则查询并缓存（单个参数版本）
     * 此方法支持将实体对象转换为指定的自定义类型，并使用独立的缓存配置
     * 缓存名称格式为：{entityName}{clazzName}，与原始实体缓存分开存储
     * <p>
     * 使用场景：当需要缓存DTO、VO等转换后的对象时使用
     * <p>
     * 注意：此方法要求 @Cache 注解的 unique 属性为 true（默认值）
     *
     * @param clazz 目标类型，不能为空
     * @param key   缓存 key（单个值）
     * @param <X>   目标类型泛型
     * @return 目标类型对象，如果 key 或 clazz 为空则返回 null
     * @throws IllegalStateException 如果 @Cache 注解的 unique=false
     */
    public <X> X getCache(Class<X> clazz, Object key) {
        if (key == null || clazz == null) {
            return null;
        }
        // 校验 unique 必须为 true
        checkCacheUnique(true);
        // 构建缓存 key
        Object cacheKey = buildCacheKey(key);
        if (cacheKey == null) {
            return null;
        }
        // 使用自定义类型的配置进行缓存，确保与原始实体缓存分离
        return cacheService.compute(cacheKey, () -> getEntity(clazz, key), getConfig(clazz));
    }

    /**
     * 根据name属性获取缓存，如果不存在则查询并缓存
     * 用于支持多个不同字段都有唯一值的情况，通过name来区分不同的缓存字段
     * 支持单个字段缓存和复合字段缓存（通过@Caches注解定义）
     * <p>
     * 注意：此方法要求 @Cache 注解的 unique 属性为 true（默认值）。
     * 如果需要获取非唯一字段的数据列表，请使用 getNameListCache 方法。
     *
     * @param name name属性值，用于区分不同的缓存字段或复合key
     * @param key  缓存key（字段值或Map）
     * @return 实体对象
     * @throws IllegalStateException 如果 @Cache 注解的 unique=false
     */
    public T getNameCache(String name, Object key) {
        if (key == null || name == null) {
            return null;
        }
        // 校验 unique 必须为 true
        checkCacheUnique(name, true);
        // 构建缓存 key（支持复合字段）
        Object cacheKey = buildCacheKey(name, key);
        if (cacheKey == null) {
            return null;
        }
        // 使用name对应的配置和getEntity方法
        return cacheService.compute(cacheKey, () -> getNameEntity(name, key), getConfig(name));
    }

    /**
     * 根据name属性获取自定义类型的缓存，如果不存在则查询并缓存
     * 用于支持多个不同字段都有唯一值的情况，通过name来区分不同的缓存字段
     * 支持单个字段缓存和复合字段缓存（通过@Caches注解定义）
     * 缓存名称格式为：{entityName}{naming}{clazzName}
     * <p>
     * 注意：此方法要求 @Cache 注解的 unique 属性为 true（默认值）
     *
     * @param clazz 目标类型，不能为空
     * @param name  name属性值，用于区分不同的缓存字段或复合key
     * @param key   缓存key（字段值或Map）
     * @param <X>   目标类型泛型
     * @return 目标类型对象
     * @throws IllegalStateException 如果 @Cache 注解的 unique=false
     */
    public <X> X getNameCache(Class<X> clazz, String name, Object key) {
        if (key == null || name == null || clazz == null) {
            return null;
        }
        // 校验 unique 必须为 true
        checkCacheUnique(name, true);
        // 构建缓存 key（支持复合字段）
        Object cacheKey = buildCacheKey(name, key);
        if (cacheKey == null) {
            return null;
        }
        // 使用自定义类型和name对应的配置
        return cacheService.compute(cacheKey, () -> getNameEntity(clazz, name, key), getConfig(clazz, name));
    }

    /**
     * 根据name属性获取复合key缓存，如果不存在则查询并缓存
     * 用于支持@Caches注解中定义的多个复合key缓存
     * <p>
     * 注意：此方法要求 @Cache 注解的 unique 属性为 true（默认值）。
     * 如果需要获取非唯一字段的数据列表，请使用 getNameListCache 方法。
     *
     * @param name name属性值，用于区分不同的复合key
     * @param keys 复合key的值数组，按@Cache注解中字段顺序对应
     * @return 实体对象
     * @throws IllegalStateException 如果 @Cache 注解的 unique=false
     */
    public T getNameCache(String name, Object... keys) {
        if (keys == null || keys.length == 0 || name == null) {
            return null;
        }
        // 如果只有一个参数，调用单参数版本（已包含校验）
        if (keys.length == 1) {
            return getNameCache(name, keys[0]);
        }
        // 校验 unique 必须为 true
        checkCacheUnique(name, true);
        // 构建缓存 key（复合字段）
        Object cacheKey = buildCacheKey(name, keys);
        if (cacheKey == null) {
            return null;
        }
        // 使用name对应的配置和getEntity方法
        return cacheService.compute(cacheKey, () -> getNameEntity(name, keys), getConfig(name));
    }

    /**
     * 根据name属性获取自定义类型的复合key缓存，如果不存在则查询并缓存
     * 用于支持@Caches注解中定义的多个复合key缓存
     * 缓存名称格式为：{entityName}{naming}{clazzName}
     * <p>
     * 注意：此方法要求 @Cache 注解的 unique 属性为 true（默认值）
     *
     * @param clazz 目标类型，不能为空
     * @param name  name属性值，用于区分不同的复合key
     * @param keys  复合key的值数组，按@Cache注解中字段顺序对应
     * @param <X>   目标类型泛型
     * @return 目标类型对象
     * @throws IllegalStateException 如果 @Cache 注解的 unique=false
     */
    public <X> X getNameCache(Class<X> clazz, String name, Object... keys) {
        if (keys == null || keys.length == 0 || name == null || clazz == null) {
            return null;
        }
        // 如果只有一个参数，调用单参数版本（已包含校验）
        if (keys.length == 1) {
            return getNameCache(clazz, name, keys[0]);
        }
        // 校验 unique 必须为 true
        checkCacheUnique(name, true);
        // 构建缓存 key（复合字段）
        Object cacheKey = buildCacheKey(name, keys);
        if (cacheKey == null) {
            return null;
        }
        // 使用自定义类型和name对应的配置
        return cacheService.compute(cacheKey, () -> getNameEntity(clazz, name, keys), getConfig(clazz, name));
    }

    /**
     * 获取缓存，如果不存在则查询并缓存（可变参数版本，兼容性）
     * 获取一条缓存值，如果未能获取到该值，则调用getEntity函数，通过查询数据库获取，实现类可以实现getEntity来提高效率
     * 也可以通过@Cache来标注其字段，通过反射来获取对应字段的实例
     * <p>
     * 支持两种方式：
     * 1. 单个字段缓存：getCache(key) - key 为单个值（推荐使用单参数版本 getCache(Object key)）
     * 2. 复合字段缓存：getCache(value1, value2, ...) - 多个值按 @Cache 注解中字段顺序对应
     * <p>
     * 注意：此方法要求 @Cache 注解的 unique 属性为 true（默认值）。
     * 如果需要获取非唯一字段的数据列表，请使用 getListCache 方法。
     *
     * @param keys 缓存 key，可以是单个值或多个值（可变参数）
     * @return 实体对象
     * @throws IllegalStateException 如果 @Cache 注解的 unique=false
     */
    public T getCache(Object... keys) {
        if (keys == null || keys.length == 0) {
            return null;
        }
        // 如果只有一个参数，调用单参数版本（更高效，已包含校验）
        if (keys.length == 1) {
            return getCache(keys[0]);
        }
        // 校验 unique 必须为 true
        checkCacheUnique(true);
        // 构建缓存 key
        Object cacheKey = buildCacheKey(keys);
        if (cacheKey == null) {
            return null;
        }
        // 调用可变参数版本的 getEntity
        return cacheService.compute(cacheKey, () -> getEntity(keys), getConfig());
    }

    /**
     * 获取自定义类型的缓存，如果不存在则查询并缓存（可变参数版本）
     * 此方法支持将实体对象转换为指定的自定义类型，并使用独立的缓存配置
     * <p>
     * 支持两种方式：
     * 1. 单个字段缓存：getCache(clazz, key) - 推荐使用单参数版本 getCache(Class<X>, Object)
     * 2. 复合字段缓存：getCache(clazz, value1, value2, ...) - 多个值按 @Cache 注解中字段顺序对应
     * <p>
     * 注意：此方法要求 @Cache 注解的 unique 属性为 true（默认值）
     *
     * @param clazz 目标类型，不能为空
     * @param keys  缓存 key，可以是单个值或多个值（可变参数）
     * @param <X>   目标类型泛型
     * @return 目标类型对象
     * @throws IllegalStateException 如果 @Cache 注解的 unique=false
     */
    public <X> X getCache(Class<X> clazz, Object... keys) {
        if (keys == null || keys.length == 0 || clazz == null) {
            return null;
        }
        // 如果只有一个参数，调用单参数版本（更高效，已包含校验）
        if (keys.length == 1) {
            return getCache(clazz, keys[0]);
        }
        // 校验 unique 必须为 true
        checkCacheUnique(true);
        // 构建缓存 key
        Object cacheKey = buildCacheKey(keys);
        if (cacheKey == null) {
            return null;
        }
        // 使用自定义类型的配置进行缓存
        return cacheService.compute(cacheKey, () -> getEntity(clazz, keys), getConfig(clazz));
    }

    /**
     * 获取List列表缓存，如果不存在则查询并缓存（单个参数版本）
     * 用于缓存非唯一字段（unique=false）的数据，返回符合条件的所有记录
     * <p>
     * 注意：此方法要求 @Cache 注解的 unique 属性为 false。
     * 如果需要获取唯一字段的单个数据，请使用 getCache 方法。
     *
     * @param key 缓存 key（单个值）
     * @return 实体对象列表
     * @throws IllegalStateException 如果 @Cache 注解的 unique=true
     */
    public List<T> getListCache(Object key) {
        if (key == null) {
            return null;
        }
        // 校验 unique 必须为 false
        checkCacheUnique(false);
        // 构建缓存 key
        Object cacheKey = buildCacheKey(key);
        if (cacheKey == null) {
            return null;
        }
        // 调用单参数版本的 getListEntity
        return cacheService.compute(cacheKey, () -> getListEntity(key), getListConfig());
    }

    /**
     * 获取自定义类型的List列表缓存，如果不存在则查询并缓存（单个参数版本）
     * 此方法将实体列表转换为自定义类型列表，并使用独立的缓存配置
     * 缓存名称格式为：{entityName}list{clazzName}
     * <p>
     * 注意：此方法要求 @Cache 注解的 unique 属性为 false
     *
     * @param clazz 目标类型，不能为空
     * @param key   缓存 key（单个值）
     * @param <X>   目标类型泛型
     * @return 目标类型对象列表
     * @throws IllegalStateException 如果 @Cache 注解的 unique=true
     */
    public <X> List<X> getListCache(Class<X> clazz, Object key) {
        if (key == null || clazz == null) {
            return null;
        }
        // 校验 unique 必须为 false
        checkCacheUnique(false);
        // 构建缓存 key
        Object cacheKey = buildCacheKey(key);
        if (cacheKey == null) {
            return null;
        }
        // 使用自定义类型的 List 配置进行缓存
        return cacheService.compute(cacheKey, () -> getListEntity(clazz, key), getListConfig(clazz));
    }

    /**
     * 获取自定义类型的List列表缓存，如果不存在则查询并缓存（可变参数版本）
     * 此方法将实体列表转换为自定义类型列表，并使用独立的缓存配置
     * <p>
     * 注意：此方法要求 @Cache 注解的 unique 属性为 false
     *
     * @param clazz 目标类型，不能为空
     * @param keys  缓存 key，可以是单个值或多个值（可变参数）
     * @param <X>   目标类型泛型
     * @return 目标类型对象列表
     * @throws IllegalStateException 如果 @Cache 注解的 unique=true
     */
    public <X> List<X> getListCache(Class<X> clazz, Object... keys) {
        if (keys == null || keys.length == 0 || clazz == null) {
            return null;
        }
        // 如果只有一个参数，调用单参数版本（更高效，已包含校验）
        if (keys.length == 1) {
            return getListCache(clazz, keys[0]);
        }
        // 校验 unique 必须为 false
        checkCacheUnique(false);
        // 构建缓存 key
        Object cacheKey = buildCacheKey(keys);
        if (cacheKey == null) {
            return null;
        }
        // 使用自定义类型的 List 配置进行缓存
        return cacheService.compute(cacheKey, () -> getListEntity(clazz, keys), getListConfig(clazz));
    }

    /**
     * 获取List列表缓存，如果不存在则查询并缓存（可变参数版本）
     * 用于缓存非唯一字段（unique=false）的数据，返回符合条件的所有记录
     * <p>
     * 注意：此方法要求 @Cache 注解的 unique 属性为 false。
     * 如果需要获取唯一字段的单个数据，请使用 getCache 方法。
     *
     * @param keys 缓存 key，可以是单个值或多个值（可变参数）
     * @return 实体对象列表
     * @throws IllegalStateException 如果 @Cache 注解的 unique=true
     */
    public List<T> getListCache(Object... keys) {
        if (keys == null || keys.length == 0) {
            return null;
        }
        // 如果只有一个参数，调用单参数版本（更高效，已包含校验）
        if (keys.length == 1) {
            return getListCache(keys[0]);
        }
        // 校验 unique 必须为 false
        checkCacheUnique(false);
        // 构建缓存 key
        Object cacheKey = buildCacheKey(keys);
        if (cacheKey == null) {
            return null;
        }
        // 调用可变参数版本的 getListEntity
        return cacheService.compute(cacheKey, () -> getListEntity(keys), getListConfig());
    }

    /**
     * 根据name属性获取List列表缓存，如果不存在则查询并缓存
     * 用于支持多个不同字段都有非唯一值的情况，通过name来区分不同的缓存字段
     * <p>
     * 注意：此方法要求 @Cache 注解的 unique 属性为 false。
     * 如果需要获取唯一字段的单个数据，请使用 getNameCache 方法。
     *
     * @param name name属性值，用于区分不同的缓存字段或复合key
     * @param key  缓存key（字段值或Map）
     * @return 实体对象列表
     * @throws IllegalStateException 如果 @Cache 注解的 unique=true
     */
    public List<T> getNameListCache(String name, Object key) {
        if (key == null || name == null) {
            return null;
        }
        // 校验 unique 必须为 false
        checkCacheUnique(name, false);
        // 构建缓存 key（支持复合字段）
        Object cacheKey = buildCacheKey(name, key);
        if (cacheKey == null) {
            return null;
        }
        // 使用name对应的配置和getNameListEntity方法
        return cacheService.compute(cacheKey, () -> getNameListEntity(name, key), getListConfig(name));
    }

    /**
     * 根据name属性获取自定义类型的List列表缓存，如果不存在则查询并缓存
     * 用于支持多个不同字段都有非唯一值的情况，通过name来区分不同的缓存字段
     * 缓存名称格式为：{entityName}{naming}list{clazzName}
     * <p>
     * 注意：此方法要求 @Cache 注解的 unique 属性为 false
     *
     * @param clazz 目标类型，不能为空
     * @param name  name属性值，用于区分不同的缓存字段或复合key
     * @param key   缓存key（字段值或Map）
     * @param <X>   目标类型泛型
     * @return 目标类型对象列表
     * @throws IllegalStateException 如果 @Cache 注解的 unique=true
     */
    public <X> List<X> getNameListCache(Class<X> clazz, String name, Object key) {
        if (key == null || name == null || clazz == null) {
            return null;
        }
        // 校验 unique 必须为 false
        checkCacheUnique(name, false);
        // 构建缓存 key（支持复合字段）
        Object cacheKey = buildCacheKey(name, key);
        if (cacheKey == null) {
            return null;
        }
        // 使用自定义类型和name对应的 List 配置
        return cacheService.compute(cacheKey, () -> getNameListEntity(clazz, name, key), getListConfig(clazz, name));
    }

    /**
     * 根据name属性获取自定义类型的复合key List列表缓存，如果不存在则查询并缓存
     * 用于支持@Caches注解中定义的多个复合key缓存（unique=false）
     * <p>
     * 注意：此方法要求 @Cache 注解的 unique 属性为 false
     *
     * @param clazz 目标类型，不能为空
     * @param name  name属性值，用于区分不同的复合key
     * @param keys  复合key的值数组，按@Cache注解中字段顺序对应
     * @param <X>   目标类型泛型
     * @return 目标类型对象列表
     * @throws IllegalStateException 如果 @Cache 注解的 unique=true
     */
    public <X> List<X> getNameListCache(Class<X> clazz, String name, Object... keys) {
        if (keys == null || keys.length == 0 || name == null || clazz == null) {
            return null;
        }
        // 如果只有一个参数，调用单参数版本（已包含校验）
        if (keys.length == 1) {
            return getNameListCache(clazz, name, keys[0]);
        }
        // 校验 unique 必须为 false
        checkCacheUnique(name, false);
        // 构建缓存 key（复合字段）
        Object cacheKey = buildCacheKey(name, keys);
        if (cacheKey == null) {
            return null;
        }
        // 使用自定义类型和name对应的 List 配置
        return cacheService.compute(cacheKey, () -> getNameListEntity(clazz, name, keys), getListConfig(clazz, name));
    }

    /**
     * 根据name属性获取复合key的List列表缓存，如果不存在则查询并缓存
     * 用于支持@Caches注解中定义的多个复合key缓存（unique=false）
     * <p>
     * 注意：此方法要求 @Cache 注解的 unique 属性为 false。
     * 如果需要获取唯一字段的单个数据，请使用 getNameCache 方法。
     *
     * @param name name属性值，用于区分不同的复合key
     * @param keys 复合key的值数组，按@Cache注解中字段顺序对应
     * @return 实体对象列表
     * @throws IllegalStateException 如果 @Cache 注解的 unique=true
     */
    public List<T> getNameListCache(String name, Object... keys) {
        if (keys == null || keys.length == 0 || name == null) {
            return null;
        }
        // 如果只有一个参数，调用单参数版本（已包含校验）
        if (keys.length == 1) {
            return getNameListCache(name, keys[0]);
        }
        // 校验 unique 必须为 false
        checkCacheUnique(name, false);
        // 构建缓存 key（复合字段）
        Object cacheKey = buildCacheKey(name, keys);
        if (cacheKey == null) {
            return null;
        }
        // 使用name对应的配置和getNameListEntity方法
        return cacheService.compute(cacheKey, () -> getNameListEntity(name, keys), getListConfig(name));
    }

    /**
     * 构建缓存 key（单个参数版本）
     *
     * @param key 单个 key
     * @return 缓存 key
     */
    private Object buildCacheKey(Object key) {
        return buildCacheKey("", key);
    }

    /**
     * 构建缓存 key（带name参数版本，支持Caches注解）
     * 支持 {@link CacheParam} 包装：透传参数返回 null，自定义 key 返回 toKey()
     *
     * @param name name属性值，如果为空则使用默认的@Cache注解
     * @param key  单个 key
     * @return 缓存 key
     */
    private Object buildCacheKey(String name, Object key) {
        if (key == null) {
            return null;
        }
        // CacheParam 透传参数，不参与缓存，返回 null
        if (key instanceof CacheParam && ((CacheParam<?>) key).isTransparent()) {
            return null;
        }
        Class<?> clazz = getModelClass();
        if (clazz == null) {
            return unwrapCacheParam(key);
        }
        // 查找复合字段缓存配置
        Cache compositeCache = findCompositeCache(clazz, name);
        if (compositeCache != null && compositeCache.value().length > 0) {
            // 单个参数在复合字段模式下，如果是 Map，从 Map 中提取值构建字符串 key
            if (key instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> keyMap = (Map<String, Object>) key;
                String[] fieldNames = compositeCache.value();
                Object[] values = new Object[fieldNames.length];
                for (int i = 0; i < fieldNames.length; i++) {
                    values[i] = keyMap.get(fieldNames[i]);
                }
                return buildCompositeKey(values);
            }
        }
        // 单个字段缓存，直接返回 key（解包 CacheParam）
        return unwrapCacheParam(key);
    }

    /**
     * 查找复合字段缓存配置（支持@Cache和@Caches注解）
     *
     * @param clazz 实体类
     * @param name  name属性值，如果为空则查找默认的@Cache注解
     * @return Cache注解，如果不存在返回null
     */
    private Cache findCompositeCache(Class<?> clazz, String name) {
        if (clazz == null) {
            return null;
        }
        // 如果name为空，查找单个@Cache注解（向前兼容）
        if (name == null || name.isEmpty()) {
            return findSingleCacheAnnotation(clazz);
        }
        // 如果name不为空，查找@Caches注解中指定name的@Cache注解
        return findCacheInCaches(clazz, name);
    }

    /**
     * 构建缓存 key（可变参数版本）
     * 对于复合字段，将可变参数转换为字符串 key（使用分隔符连接）
     *
     * @param keys 可变参数数组
     * @return 缓存 key（字符串或单个值）
     */
    private Object buildCacheKey(Object... keys) {
        return buildCacheKey("", keys);
    }

    /**
     * 构建缓存 key（带name参数的可变参数版本，支持Caches注解）
     * <p>
     * 支持 {@link CacheParam} 包装：透传参数不占用字段位置，不参与 key 计算
     *
     * @param name name属性值，如果为空则使用默认的@Cache注解
     * @param keys 可变参数数组
     * @return 缓存 key（字符串或单个值）
     */
    private Object buildCacheKey(String name, Object... keys) {
        if (keys == null || keys.length == 0) {
            return null;
        }
        // 过滤出有效 key 参数（排除透传参数）
        Object[] effectiveKeys = filterEffectiveKeys(keys);
        if (effectiveKeys.length == 0) {
            return null;
        }
        Class<?> clazz = getModelClass();
        if (clazz == null) {
            if (effectiveKeys.length == 1) {
                return unwrapCacheParam(effectiveKeys[0]);
            }
            return buildCompositeKey(effectiveKeys);
        }
        // 查找复合字段缓存配置
        Cache compositeCache = findCompositeCache(clazz, name);
        if (compositeCache != null && compositeCache.value().length > 0) {
            String[] fieldNames = compositeCache.value();
            // 如果只有一个有效参数且是 Map，从 Map 中提取值构建字符串 key
            if (effectiveKeys.length == 1 && effectiveKeys[0] instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> keyMap = (Map<String, Object>) effectiveKeys[0];
                Object[] values = new Object[fieldNames.length];
                for (int i = 0; i < fieldNames.length; i++) {
                    values[i] = keyMap.get(fieldNames[i]);
                }
                return buildCompositeKey(values);
            }
            // 将有效参数值与字段名对应，按字段顺序构建字符串 key
            Object[] values = new Object[fieldNames.length];
            int paramCount = Math.min(effectiveKeys.length, fieldNames.length);
            for (int i = 0; i < paramCount; i++) {
                if (effectiveKeys[i] != null) {
                    values[i] = effectiveKeys[i];
                } else {
                    // 参数值为 null，返回 null
                    return null;
                }
            }
            // 如果有效参数数量与字段数量不匹配，返回 null
            if (paramCount != fieldNames.length) {
                return null;
            }
            return buildCompositeKey(values);
        }
        // 单个字段缓存，返回第一个有效参数（解包 CacheParam）
        return unwrapCacheParam(effectiveKeys[0]);
    }

    /**
     * 构建复合 key 字符串
     * 使用 ":" 作为分隔符连接多个值
     * <p>
     * 支持 {@link CacheParam} 包装：
     * <ul>
     *   <li>CacheParam.pass(value) — 透传参数，跳过不参与 key 计算</li>
     *   <li>CacheParam.key(value, customKey) — 使用自定义 key 参与计算</li>
     * </ul>
     *
     * @param values 值数组
     * @return 组合后的 key 字符串
     */
    protected String buildCompositeKey(Object... values) {
        if (values == null || values.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Object value : values) {
            // CacheParam 透传参数，跳过不参与 key 计算
            if (value instanceof CacheParam && ((CacheParam<?>) value).isTransparent()) {
                continue;
            }
            if (!first) {
                sb.append(":");
            }
            first = false;
            if (value instanceof CacheParam) {
                // CacheParam 自定义 key
                sb.append(((CacheParam<?>) value).toKey());
            } else if (null == value) {
                sb.append("null");
            } else {
                sb.append(value);
            }
        }
        return sb.toString();
    }

    /**
     * 过滤出有效 key 参数（排除 {@link CacheParam} 透传参数）
     *
     * @param keys 原始参数数组
     * @return 仅包含参与 key 计算的参数数组
     */
    private Object[] filterEffectiveKeys(Object[] keys) {
        if (keys == null || keys.length == 0) {
            return new Object[0];
        }
        int count = 0;
        for (Object key : keys) {
            if (!(key instanceof CacheParam && ((CacheParam<?>) key).isTransparent())) {
                count++;
            }
        }
        if (count == keys.length) {
            return keys; // 无透传参数，直接返回原数组，避免拷贝
        }
        Object[] result = new Object[count];
        int idx = 0;
        for (Object key : keys) {
            if (!(key instanceof CacheParam && ((CacheParam<?>) key).isTransparent())) {
                result[idx++] = key;
            }
        }
        return result;
    }

    /**
     * 解包 CacheParam 获取缓存 key 值
     * <ul>
     *   <li>CacheParam 自定义 key：返回自定义 key 字符串</li>
     *   <li>CacheParam 透传：返回 null</li>
     *   <li>CacheKey（向后兼容）：返回 toKey()</li>
     *   <li>其他：返回原值</li>
     * </ul>
     *
     * @param value 原始值
     * @return 用于缓存的 key 值
     */
    private Object unwrapCacheParam(Object value) {
        if (value instanceof CacheParam) {
            CacheParam<?> param = (CacheParam<?>) value;
            if (param.isTransparent()) {
                return null;
            }
            return param.toKey();
        }
        return value;
    }

    /**
     * 解包参数获取原始业务值（供子类在业务逻辑中使用）
     * 如果参数是 {@link CacheParam} 包装，返回其内部的原始值；否则返回参数本身
     *
     * @param param 可能被 CacheParam 包装的参数
     * @return 原始业务值
     */
    protected Object unwrapValue(Object param) {
        if (param instanceof CacheParam) {
            return ((CacheParam<?>) param).get();
        }
        return param;
    }

    /**
     * 批量解包参数获取原始业务值（供子类在业务逻辑中使用）
     *
     * @param params 可能包含 CacheParam 包装的参数数组
     * @return 全部解包后的原始值数组
     */
    protected Object[] unwrapValues(Object... params) {
        if (params == null) {
            return null;
        }
        Object[] result = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            result[i] = unwrapValue(params[i]);
        }
        return result;
    }

    /**
     * 通过反射获取对象实体（单个参数版本，高效）
     * 具体实现类可以实现该方法，可提高消息，如果实例类为实现该方法，可以使用@Cache标注对应字段，系统将通过该字段，使用反射获取一条实例
     * 应保证使用@Cache标注的字段值的唯一性，因为默认实现只会去一条有效值
     * 支持@Cache和@Caches注解
     *
     * @param key 指定的实体key（单个值）
     * @return 返回实体类型的对象， 如果未使用@Cache指定字段，则返回null
     */
    public T getEntity(Object key) {
        return getNameEntity("", key);
    }

    public <X> X getEntity(Class<X> clazz, Object key) {
        return getNameEntity(clazz, "", key);
    }

    public <X> X getNameEntity(Class<X> clazz, String name, Object key) {
        T entity = getNameEntity(name, key);
        return convert(clazz, entity);
    }

    /**
     * 通过反射获取对象实体（带name参数版本，支持Caches注解）
     *
     * @param name name属性值，如果为空则使用默认的@Cache注解
     * @param key  指定的实体key（单个值）
     * @return 返回实体类型的对象， 如果未使用@Cache指定字段，则返回null
     */
    public T getNameEntity(String name, Object key) {
        if (key == null) {
            return null;
        }
        Class<?> clazz = getModelClass();
        if (clazz == null) {
            return null;
        }
        // 查找复合字段缓存配置（支持@Cache和@Caches注解）
        Cache compositeCache = findCompositeCache(clazz, name);
        if (compositeCache != null && compositeCache.value().length > 0) {
            String[] fieldNames = compositeCache.value();
            // 如果是 Map，使用 Map 方式
            if (key instanceof Map) {
                return getEntityByCompositeFields(key, clazz, fieldNames);
            }
            // 单个参数在复合字段模式下，参数数量不匹配，返回 null
            return null;
        }
        // 查找 @Cache 标注的字段（单个字段缓存）
        Field cacheField = findCacheField(clazz, name);
        if (cacheField == null) {
            return null;
        }
        // 将参数转换为字段类型
        Object convertedKey = convert(key, cacheField.getType());
        if (convertedKey == null) {
            return null;
        }
        // 使用 EntityWrapper 查询
        String fieldName = cacheField.getName();
        String columnName = HumpConvert.HumpToUnderline(fieldName);
        EntityWrapper<T> wrapper = new EntityWrapper<>();
        wrapper.eq(Escape.escape(columnName), convertedKey);
        return selectOne(wrapper);
    }

    public <X> X getNameEntity(Class<X> clazz, String name, Object... keys) {
        T entity = getNameEntity(name, keys);
        return convert(clazz, entity);
    }

    /**
     * 根据name属性获取实体（可变参数版本，支持复合key）
     *
     * @param name name属性值，用于区分不同的复合key
     * @param keys 复合key的值数组，按@Cache注解中字段顺序对应
     * @return 实体对象
     */
    public T getNameEntity(String name, Object... keys) {
        if (keys == null || keys.length == 0 || name == null) {
            return null;
        }
        // 如果只有一个参数，调用单参数版本
        if (keys.length == 1) {
            return getNameEntity(name, keys[0]);
        }
        Class<?> clazz = getModelClass();
        if (clazz == null) {
            return null;
        }
        // 查找复合字段缓存配置（支持@Caches注解）
        Cache compositeCache = findCompositeCache(clazz, name);
        if (compositeCache != null && compositeCache.value().length > 0) {
            String[] fieldNames = compositeCache.value();
            // 将可变参数值与字段名对应构建 Map
            Map<String, Object> fieldValues = new HashMap<>();
            int paramCount = Math.min(keys.length, fieldNames.length);
            for (int i = 0; i < paramCount; i++) {
                if (keys[i] != null) {
                    fieldValues.put(fieldNames[i], keys[i]);
                }
            }
            // 如果参数数量与字段数量不匹配，返回 null
            if (fieldValues.size() != fieldNames.length) {
                return null;
            }
            // 使用复合字段查询
            return getEntityByCompositeFields(fieldValues, clazz, fieldNames);
        }
        // 查找 @Cache 标注的字段（单个字段缓存）
        Field cacheField = findCacheField(clazz, name);
        if (cacheField == null) {
            return null;
        }
        // 将第一个参数转换为字段类型
        Object convertedKey = convert(keys[0], cacheField.getType());
        if (convertedKey == null) {
            return null;
        }
        // 使用 EntityWrapper 查询
        String fieldName = cacheField.getName();
        String columnName = HumpConvert.HumpToUnderline(fieldName);
        EntityWrapper<T> wrapper = new EntityWrapper<>();
        wrapper.eq(Escape.escape(columnName), convertedKey);
        return selectOne(wrapper);
    }

    public <X> X getEntity(Class<X> clazz, Object... keys) {
        T entity = getEntity(keys);
        return convert(clazz, entity);
    }

    public <X> X convert(Class<X> clazz, T entity) {
        try {
            X x = clazz.newInstance();
            BeanUtils.copyProperties(x, entity);
            return x;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 通过反射获取对象实体（可变参数版本，兼容性）
     * 具体实现类可以实现该方法，可提高消息，如果实例类为实现该方法，可以使用@Cache标注对应字段，系统将通过该字段，使用反射获取一条实例
     * 应保证使用@Cache标注的字段值的唯一性，因为默认实现只会去一条有效值
     * <p>
     * 支持两种方式：
     * 1. 在字段上标注 @Cache：使用单个字段作为缓存 key - getEntity(key)（推荐使用单参数版本 getEntity(Object key)）
     * 2. 在类上标注 @Cache(value = {"field1", "field2"})：使用多个字段组合作为复合缓存 key - getEntity(value1, value2, ...)
     * 3. 在类上标注 @Caches：使用多个复合key缓存，需要通过getNameEntity(name, keys)来获取
     *
     * @param keys 指定的实体key，可以是单个值（单个字段）或多个值（复合字段，按 @Cache 注解中字段顺序对应）
     * @return 返回实体类型的对象， 如果未使用@Cache指定字段，则返回null
     */
    public T getEntity(Object... keys) {
        if (keys == null || keys.length == 0) {
            return null;
        }
        // 如果只有一个参数，调用单参数版本（更高效）
        if (keys.length == 1) {
            return getEntity(keys[0]);
        }
        Class<?> clazz = getModelClass();
        if (clazz == null) {
            return null;
        }
        // 查找复合字段缓存配置（支持@Cache和@Caches注解，默认使用不带name的）
        Cache compositeCache = findCompositeCache(clazz, "");
        if (compositeCache != null && compositeCache.value().length > 0) {
            String[] fieldNames = compositeCache.value();
            // 将可变参数值与字段名对应构建 Map
            Map<String, Object> fieldValues = new HashMap<>();
            int paramCount = Math.min(keys.length, fieldNames.length);
            for (int i = 0; i < paramCount; i++) {
                if (keys[i] != null) {
                    fieldValues.put(fieldNames[i], keys[i]);
                }
            }
            // 如果参数数量与字段数量不匹配，返回 null
            if (fieldValues.size() != fieldNames.length) {
                return null;
            }
            // 使用复合字段查询
            return getEntityByCompositeFields(fieldValues, clazz, fieldNames);
        }
        // 查找 @Cache 标注的字段（单个字段缓存）
        Field cacheField = findCacheField(clazz);
        if (cacheField == null) {
            return null;
        }
        // 将第一个参数转换为字段类型
        Object convertedKey = convert(keys[0], cacheField.getType());
        if (convertedKey == null) {
            return null;
        }
        // 使用 EntityWrapper 查询
        String fieldName = cacheField.getName();
        String columnName = HumpConvert.HumpToUnderline(fieldName);
        EntityWrapper<T> wrapper = new EntityWrapper<>();
        wrapper.eq(Escape.escape(columnName), convertedKey);
        return selectOne(wrapper);
    }

    /**
     * 通过反射获取对象实体列表（单个参数版本，用于非唯一字段）
     * 当@Cache注解的unique=false时，使用此方法获取符合条件的所有记录
     *
     * @param key 指定的实体key（单个值）
     * @return 返回实体类型的对象列表，如果未使用@Cache指定字段，则返回null
     */
    public List<T> getListEntity(Object key) {
        return getNameListEntity("", key);
    }

    public <X> List<X> getListEntity(Class<X> clazz, Object key) {
        return getNameListEntity(clazz, "", key);
    }

    public <X> List<X> getNameListEntity(Class<X> clazz, String name, Object key) {
        if (key == null || name == null) {
            return null;
        }
        List<T> list = getNameListEntity(name, key);
        return convert(clazz, list);
    }

    /**
     * 通过反射获取对象实体列表（带name参数版本，用于非唯一字段）
     *
     * @param name name属性值，如果为空则使用默认的@Cache注解
     * @param key  指定的实体key（单个值）
     * @return 返回实体类型的对象列表，如果未使用@Cache指定字段，则返回null
     */
    public List<T> getNameListEntity(String name, Object key) {
        if (key == null) {
            return null;
        }
        Class<?> clazz = getModelClass();
        if (clazz == null) {
            return null;
        }
        // 查找复合字段缓存配置（支持@Cache和@Caches注解）
        Cache compositeCache = findCompositeCache(clazz, name);
        if (compositeCache != null && compositeCache.value().length > 0) {
            String[] fieldNames = compositeCache.value();
            // 如果是 Map，使用 Map 方式
            if (key instanceof Map) {
                return getListEntityByCompositeFields(key, clazz, fieldNames);
            }
            // 单个参数在复合字段模式下，参数数量不匹配，返回 null
            return null;
        }
        // 查找 @Cache 标注的字段（单个字段缓存）
        Field cacheField = findCacheField(clazz, name);
        if (cacheField == null) {
            return null;
        }
        // 将参数转换为字段类型
        Object convertedKey = convert(key, cacheField.getType());
        if (convertedKey == null) {
            return null;
        }
        // 使用 EntityWrapper 查询列表
        String fieldName = cacheField.getName();
        String columnName = HumpConvert.HumpToUnderline(fieldName);
        EntityWrapper<T> wrapper = new EntityWrapper<>();
        wrapper.eq(Escape.escape(columnName), convertedKey);
        return selectList(wrapper);
    }

    public <X> List<X> getNameListEntity(Class<X> clazz, String name, Object... keys) {
        if (keys == null || keys.length == 0 || name == null) {
            return null;
        }
        List<T> list = getNameListEntity(name, keys);
        return convert(clazz, list);
    }

    /**
     * 根据name属性获取实体列表（可变参数版本，支持复合key）
     *
     * @param name name属性值，用于区分不同的复合key
     * @param keys 复合key的值数组，按@Cache注解中字段顺序对应
     * @return 实体对象列表
     */
    public List<T> getNameListEntity(String name, Object... keys) {
        if (keys == null || keys.length == 0 || name == null) {
            return null;
        }
        // 如果只有一个参数，调用单参数版本
        if (keys.length == 1) {
            return getNameListEntity(name, keys[0]);
        }
        Class<?> clazz = getModelClass();
        if (clazz == null) {
            return null;
        }
        // 查找复合字段缓存配置（支持@Caches注解）
        Cache compositeCache = findCompositeCache(clazz, name);
        if (compositeCache != null && compositeCache.value().length > 0) {
            String[] fieldNames = compositeCache.value();
            // 将可变参数值与字段名对应构建 Map
            Map<String, Object> fieldValues = new HashMap<>();
            int paramCount = Math.min(keys.length, fieldNames.length);
            for (int i = 0; i < paramCount; i++) {
                if (keys[i] != null) {
                    fieldValues.put(fieldNames[i], keys[i]);
                }
            }
            // 如果参数数量与字段数量不匹配，返回 null
            if (fieldValues.size() != fieldNames.length) {
                return null;
            }
            // 使用复合字段查询列表
            return getListEntityByCompositeFields(fieldValues, clazz, fieldNames);
        }
        // 查找 @Cache 标注的字段（单个字段缓存）
        Field cacheField = findCacheField(clazz, name);
        if (cacheField == null) {
            return null;
        }
        // 将第一个参数转换为字段类型
        Object convertedKey = convert(keys[0], cacheField.getType());
        if (convertedKey == null) {
            return null;
        }
        // 使用 EntityWrapper 查询列表
        String fieldName = cacheField.getName();
        String columnName = HumpConvert.HumpToUnderline(fieldName);
        EntityWrapper<T> wrapper = new EntityWrapper<>();
        wrapper.eq(Escape.escape(columnName), convertedKey);
        return selectList(wrapper);
    }

    public <X> List<X> convert(Class<X> clazz, List<T> list) {
        try {
            if (null == list)
                return null;
            if (list.isEmpty())
                return new ArrayList<>();
            List<X> xList = new ArrayList<>();
            for (T t : list) {
                X x = clazz.newInstance();
                BeanUtils.copyProperties(x, t);
                xList.add(x);
            }
            return xList;
        } catch (Exception e) {
            return null;
        }
    }

    public <X> List<X> getListEntity(Class<X> clazz, Object... keys) {
        List<T> list = getListEntity(keys);
        return convert(clazz, list);
    }

    /**
     * 通过反射获取对象实体列表（可变参数版本，用于非唯一字段）
     * 当@Cache注解的unique=false时，使用此方法获取符合条件的所有记录
     *
     * @param keys 指定的实体key，可以是单个值（单个字段）或多个值（复合字段，按 @Cache 注解中字段顺序对应）
     * @return 返回实体类型的对象列表，如果未使用@Cache指定字段，则返回null
     */
    public List<T> getListEntity(Object... keys) {
        if (keys == null || keys.length == 0) {
            return null;
        }
        // 如果只有一个参数，调用单参数版本（更高效）
        if (keys.length == 1) {
            return getListEntity(keys[0]);
        }
        Class<?> clazz = getModelClass();
        if (clazz == null) {
            return null;
        }
        // 查找复合字段缓存配置（支持@Cache和@Caches注解，默认使用不带name的）
        Cache compositeCache = findCompositeCache(clazz, "");
        if (compositeCache != null && compositeCache.value().length > 0) {
            String[] fieldNames = compositeCache.value();
            // 将可变参数值与字段名对应构建 Map
            Map<String, Object> fieldValues = new HashMap<>();
            int paramCount = Math.min(keys.length, fieldNames.length);
            for (int i = 0; i < paramCount; i++) {
                if (keys[i] != null) {
                    fieldValues.put(fieldNames[i], keys[i]);
                }
            }
            // 如果参数数量与字段数量不匹配，返回 null
            if (fieldValues.size() != fieldNames.length) {
                return null;
            }
            // 使用复合字段查询列表
            return getListEntityByCompositeFields(fieldValues, clazz, fieldNames);
        }
        // 查找 @Cache 标注的字段（单个字段缓存）
        Field cacheField = findCacheField(clazz);
        if (cacheField == null) {
            return null;
        }
        // 将第一个参数转换为字段类型
        Object convertedKey = convert(keys[0], cacheField.getType());
        if (convertedKey == null) {
            return null;
        }
        // 使用 EntityWrapper 查询列表
        String fieldName = cacheField.getName();
        String columnName = HumpConvert.HumpToUnderline(fieldName);
        EntityWrapper<T> wrapper = new EntityWrapper<>();
        wrapper.eq(Escape.escape(columnName), convertedKey);
        return selectList(wrapper);
    }

    /**
     * 使用复合字段查询实体
     *
     * @param key        缓存 key，可以是 Map（key 为字段名）或对象（从中提取字段值）
     * @param clazz      实体类
     * @param fieldNames 字段名数组
     * @return 实体对象
     */
    private T getEntityByCompositeFields(Object key, Class<?> clazz, String[] fieldNames) {
        Map<String, Object> fieldValues = extractFieldValues(key, clazz, fieldNames);
        if (fieldValues.isEmpty()) {
            return null;
        }
        // 构建查询条件
        EntityWrapper<T> wrapper = new EntityWrapper<>();
        for (String fieldName : fieldNames) {
            Object value = fieldValues.get(fieldName);
            if (value == null) {
                return null; // 复合 key 的所有字段都必须有值
            }
            String columnName = HumpConvert.HumpToUnderline(fieldName);
            wrapper.eq(Escape.escape(columnName), value);
        }
        return selectOne(wrapper);
    }

    /**
     * 使用复合字段查询实体列表（用于非唯一字段）
     *
     * @param key        缓存 key，可以是 Map（key 为字段名）或对象（从中提取字段值）
     * @param clazz      实体类
     * @param fieldNames 字段名数组
     * @return 实体对象列表
     */
    private List<T> getListEntityByCompositeFields(Object key, Class<?> clazz, String[] fieldNames) {
        Map<String, Object> fieldValues = extractFieldValues(key, clazz, fieldNames);
        if (fieldValues.isEmpty()) {
            return null;
        }
        // 构建查询条件
        EntityWrapper<T> wrapper = new EntityWrapper<>();
        for (String fieldName : fieldNames) {
            Object value = fieldValues.get(fieldName);
            if (value == null) {
                return null; // 复合 key 的所有字段都必须有值
            }
            String columnName = HumpConvert.HumpToUnderline(fieldName);
            wrapper.eq(Escape.escape(columnName), value);
        }
        return selectList(wrapper);
    }

    /**
     * 从 key 中提取字段值
     *
     * @param key        缓存 key，可以是 Map 或对象
     * @param clazz      实体类
     * @param fieldNames 字段名数组
     * @return 字段值 Map，key 为字段名，value 为字段值
     */
    private Map<String, Object> extractFieldValues(Object key, Class<?> clazz, String[] fieldNames) {
        Map<String, Object> fieldValues = new java.util.HashMap<>();
        // 如果 key 是 Map，直接使用
        if (key instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> keyMap = (Map<String, Object>) key;
            for (String fieldName : fieldNames) {
                Object value = keyMap.get(fieldName);
                if (value != null) {
                    fieldValues.put(fieldName, value);
                }
            }
            return fieldValues;
        }
        // 如果 key 是实体类型，从实体中提取字段值
        if (clazz.isInstance(key)) {
            @SuppressWarnings("unchecked")
            T entity = (T) key;
            for (String fieldName : fieldNames) {
                Field field = findField(clazz, fieldName);
                if (field != null) {
                    Object value = getFieldValue(entity, field);
                    if (value != null) {
                        fieldValues.put(fieldName, value);
                    }
                }
            }
            return fieldValues;
        }
        // 尝试从普通对象中提取字段值
        Class<?> keyClass = key.getClass();
        for (String fieldName : fieldNames) {
            Field field = findField(keyClass, fieldName);
            if (field != null) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(key);
                    if (value != null) {
                        fieldValues.put(fieldName, value);
                    }
                } catch (IllegalAccessException e) {
                    // 忽略无法访问的字段
                }
            }
        }
        return fieldValues;
    }

    /**
     * 查找字段（包括父类）
     */
    private Field findField(Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            // 检查父类
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null && !superClass.equals(Object.class)) {
                return findField(superClass, fieldName);
            }
            return null;
        }
    }

    /**
     * 查找实体类中标注了 @Cache 的字段（不带name或name为空）
     */
    private Field findCacheField(Class<?> clazz) {
        return findCacheField(clazz, "");
    }

    /**
     * 查找实体类中标注了 @Cache 的字段（通过name或字段名匹配）
     *
     * @param clazz 实体类
     * @param name  name属性值或字段名，如果为空则查找不带name的字段
     * @return 匹配的字段，如果未找到返回null
     */
    private Field findCacheField(Class<?> clazz, String name) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            Cache cache = field.getAnnotation(Cache.class);
            if (cache != null) {
                String cacheName = cache.name();
                String fieldName = field.getName();
                boolean cacheNameEmpty = cacheName.isEmpty();
                // 如果name为空，只匹配不带name的字段
                if (name == null || name.isEmpty()) {
                    if (cacheNameEmpty) {
                        field.setAccessible(true);
                        return field;
                    }
                } else {
                    // 如果name不为空，匹配name属性或字段名
                    // 1. 如果字段有name属性，必须匹配name属性
                    // 2. 如果字段没有name属性，匹配字段名
                    if (!cacheNameEmpty && name.equalsIgnoreCase(cacheName)) {
                        field.setAccessible(true);
                        return field;
                    } else if (cacheNameEmpty && name.equalsIgnoreCase(fieldName)) {
                        field.setAccessible(true);
                        return field;
                    }
                }
            }
        }
        // 检查父类
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && !superClass.equals(Object.class)) {
            return findCacheField(superClass, name);
        }
        return null;
    }

    /**
     * 查找实体类中所有标注了 @Cache 的字段
     *
     * @param clazz 实体类
     * @return 所有带@Cache注解的字段列表，key为name属性值（如果为空则使用字段名），value为字段对象
     */
    private Map<String, Field> findAllCacheFields(Class<?> clazz) {
        Map<String, Field> cacheFields = new HashMap<>();
        findAllCacheFields(clazz, cacheFields);
        return cacheFields;
    }

    /**
     * 递归查找所有带@Cache注解的字段（包括父类）
     *
     * @param clazz       实体类
     * @param cacheFields 结果Map
     */
    private void findAllCacheFields(Class<?> clazz, Map<String, Field> cacheFields) {
        if (clazz == null || clazz.equals(Object.class)) {
            return;
        }
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            Cache cache = field.getAnnotation(Cache.class);
            if (cache != null) {
                field.setAccessible(true);
                String cacheName = cache.name();
                // 如果name为空，使用字段名作为key；否则使用name作为key
                String key = cacheName.isEmpty() ? field.getName() : cacheName;
                // 如果key已存在，跳过（子类的字段优先）
                if (!cacheFields.containsKey(key)) {
                    cacheFields.put(key, field);
                }
            }
        }
        // 检查父类
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && !superClass.equals(Object.class)) {
            findAllCacheFields(superClass, cacheFields);
        }
    }

    /**
     * 校验 @Cache 注解的 unique 属性是否符合预期
     * 用于在运行时检查缓存方法的调用是否正确
     *
     * @param name         name属性值，如果为空则使用默认的@Cache注解
     * @param expectUnique 期望的unique值，true表示期望唯一缓存，false表示期望非唯一缓存
     * @throws IllegalStateException 如果unique值不符合预期
     */
    private void checkCacheUnique(String name, boolean expectUnique) {
        Class<?> clazz = getModelClass();
        if (clazz == null) {
            return;
        }
        // 查找复合字段缓存配置
        Cache compositeCache = findCompositeCache(clazz, name);
        if (compositeCache != null && compositeCache.value().length > 0) {
            boolean actualUnique = compositeCache.unique();
            if (actualUnique != expectUnique) {
                String methodType = expectUnique ? "getCache/getNameCache" : "getListCache/getNameListCache";
                String expectedType = expectUnique ? "unique=true" : "unique=false";
                String actualType = actualUnique ? "unique=true" : "unique=false";
                throw new IllegalStateException(String.format("缓存方法调用错误: %s 方法要求 %s，但 @Cache 注解配置为 %s。" + "请使用 %s 方法代替。", methodType, expectedType, actualType, expectUnique ? "getListCache/getNameListCache" : "getCache/getNameCache"));
            }
            return;
        }
        // 查找字段上的 @Cache 注解
        Field cacheField = findCacheField(clazz, name);
        if (cacheField != null) {
            Cache cache = cacheField.getAnnotation(Cache.class);
            if (cache != null) {
                boolean actualUnique = cache.unique();
                if (actualUnique != expectUnique) {
                    String methodType = expectUnique ? "getCache/getNameCache" : "getListCache/getNameListCache";
                    String expectedType = expectUnique ? "unique=true" : "unique=false";
                    String actualType = actualUnique ? "unique=true" : "unique=false";
                    String fieldName = cacheField.getName();
                    throw new IllegalStateException(String.format("缓存方法调用错误: %s 方法要求 %s，但字段 '%s' 的 @Cache 注解配置为 %s。" + "请使用 %s 方法代替。", methodType, expectedType, fieldName, actualType, expectUnique ? "getListCache/getNameListCache" : "getCache/getNameCache"));
                }
            }
        }
    }

    /**
     * 校验默认 @Cache 注解的 unique 属性（无name参数）
     *
     * @param expectUnique 期望的unique值
     * @throws IllegalStateException 如果unique值不符合预期
     */
    protected void checkCacheUnique(boolean expectUnique) {
        checkCacheUnique("", expectUnique);
    }

    /**
     * 根据 key 删除缓存（使用默认配置，同时删除唯一和非唯一缓存）
     */
    protected void removeCacheByKey(Object key) {
        removeCacheByKey("", key);
    }

    /**
     * 根据 name 和 key 删除缓存（同时删除唯一和非唯一缓存）
     * 同时会扫描并删除所有与该 name 相关的类型特定缓存
     *
     * @param name name属性值，如果为空则使用默认配置
     * @param key  缓存key
     */
    protected void removeCacheByKey(String name, Object key) {
        if (key == null) {
            return;
        }
        try {
            // 删除唯一缓存
            CacheConfig config = (name == null || name.isEmpty()) ? getConfig() : getConfig(name);
            String cacheName = config.getName();
            cacheService.remove(cacheName, key);
            // 删除非唯一缓存（List缓存）
            CacheConfig listConfig = (name == null || name.isEmpty()) ? getListConfig() : getListConfig(name);
            String listCacheName = listConfig.getName();
            cacheService.remove(listCacheName, key);
            // 删除所有类型特定缓存（如 DTO、VO 等转换类型的缓存）
            removeTypedCacheByKey(cacheName, listCacheName, key);
        } catch (Throwable e) {
            if (log.isWarnEnabled())
                log.warn("删除缓存失败: name={}, key={}, error={}", name, key, e.getMessage());
        }
    }

    /**
     * 删除所有类型特定缓存
     * 扫描 configs 中所有以基础缓存名称为前缀的配置，删除其中的 key
     * <p>
     * 例如：基础缓存名称为 "user"，则会扫描并删除 "useruserdto"、"userlistuserdto" 等类型特定缓存
     *
     * @param baseCacheName     基础单值缓存名称（如 "user" 或 "userlocation"）
     * @param baseListCacheName 基础List缓存名称（如 "userlist" 或 "userlocationlist"）
     * @param key               缓存key
     */
    private void removeTypedCacheByKey(String baseCacheName, String baseListCacheName, Object key) {
        for (Map.Entry<String, CacheConfig> entry : configs.entrySet()) {
            String configName = entry.getKey();
            // 跳过已经处理过的基础配置
            if (configName.equals(baseCacheName) || configName.equals(baseListCacheName)) {
                continue;
            }
            // 匹配以基础缓存名称为前缀的类型特定配置（如 "useruserdto"、"userlistuserdto"）
            if (configName.startsWith(baseCacheName) || configName.startsWith(baseListCacheName)) {
                try {
                    cacheService.remove(configName, key);
                } catch (Throwable e) {
                    if (log.isWarnEnabled())
                        log.warn("删除类型特定缓存失败: config={}, key={}, error={}", configName, key, e.getMessage());
                }
            }
        }
    }

    /**
     * 根据类型和 key 删除缓存（同时删除类型特定的唯一和非唯一缓存）
     *
     * @param clazz 目标类型
     * @param key   缓存key
     * @param <X>   类型泛型
     */
    protected <X> void removeCacheByKey(Class<X> clazz, Object key) {
        if (key == null || clazz == null) {
            return;
        }
        try {
            CacheConfig config = getConfig(clazz);
            cacheService.remove(config.getName(), key);
            CacheConfig listConfig = getListConfig(clazz);
            cacheService.remove(listConfig.getName(), key);
        } catch (Throwable e) {
            if (log.isWarnEnabled())
                log.warn("删除类型缓存失败: clazz={}, key={}, error={}", clazz.getSimpleName(), key, e.getMessage());
        }
    }

    /**
     * 根据类型、name 和 key 删除缓存（同时删除类型+name特定的唯一和非唯一缓存）
     *
     * @param clazz 目标类型
     * @param name  name属性值，如果为空则使用默认配置
     * @param key   缓存key
     * @param <X>   类型泛型
     */
    protected <X> void removeCacheByKey(Class<X> clazz, String name, Object key) {
        if (key == null || clazz == null) {
            return;
        }
        try {
            CacheConfig config = getConfig(clazz, name);
            cacheService.remove(config.getName(), key);
            CacheConfig listConfig = getListConfig(clazz, name);
            cacheService.remove(listConfig.getName(), key);
        } catch (Throwable e) {
            if (log.isWarnEnabled())
                log.warn("删除类型命名缓存失败: clazz={}, name={}, key={}, error={}", clazz.getSimpleName(), name, key, e.getMessage());
        }
    }

    /**
     * 根据实体对象删除缓存
     * 从实体中提取 @Cache 标注的字段值作为缓存 key
     * 支持以下方式：
     * 1. 类上的单个复合字段缓存（@Cache(value = {"field1", "field2"})，向前兼容）
     * 2. 类上的多个复合字段缓存（@Caches，内部Cache需要指明name）
     * 3. 字段上的单个字段缓存（@Cache 或 @Cache(name = "xxx")）
     * 4. 多个不同name的字段缓存（会删除所有匹配的缓存）
     */
    protected void removeCacheByEntity(T entity) {
        if (entity == null) {
            return;
        }
        try {
            Class<?> clazz = getModelClass();
            if (clazz == null) {
                return;
            }
            // 1. 删除所有复合字段缓存（包括@Cache和@Caches中定义的）
            removeAllCompositeCaches(entity, clazz);
            // 2. 删除所有字段上带 @Cache 注解的缓存（包括带name和不带name的）
            removeFieldCaches(entity, clazz);
        } catch (Throwable e) {
            if (log.isWarnEnabled())
                log.warn("清除实体缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 删除所有复合字段缓存（支持@Cache和@Caches注解）
     *
     * @param entity 实体对象
     * @param clazz  实体类
     */
    protected void removeAllCompositeCaches(T entity, Class<?> clazz) {
        Map<String, Cache> compositeCaches = findAllCompositeCaches(clazz);
        for (Map.Entry<String, Cache> entry : compositeCaches.entrySet()) {
            String name = entry.getKey();
            Cache cache = entry.getValue();
            if (cache != null && cache.value().length > 0) {
                // 删除复合字段缓存
                removeCompositeCache(entity, clazz, cache.value(), name);
            }
        }
    }

    /**
     * 删除复合字段缓存（使用默认配置）
     *
     * @param entity     实体对象
     * @param clazz      实体类
     * @param fieldNames 字段名数组
     */
    protected void removeCompositeCache(T entity, Class<?> clazz, String[] fieldNames) {
        removeCompositeCache(entity, clazz, fieldNames, "");
    }

    /**
     * 删除复合字段缓存（支持指定name配置）
     *
     * @param entity     实体对象
     * @param clazz      实体类
     * @param fieldNames 字段名数组
     * @param name       name属性值，如果为空则使用默认配置
     */
    protected void removeCompositeCache(T entity, Class<?> clazz, String[] fieldNames, String name) {
        Object[] values = new Object[fieldNames.length];
        boolean allValuesPresent = true;

        for (int i = 0; i < fieldNames.length; i++) {
            Field field = findField(clazz, fieldNames[i]);
            if (field != null) {
                Object value = getFieldValue(entity, field);
                if (value != null) {
                    values[i] = value;
                } else {
                    allValuesPresent = false;
                    break;
                }
            } else {
                allValuesPresent = false;
                break;
            }
        }

        // 只有当所有字段值都存在时，才构建字符串 key 并删除缓存
        if (allValuesPresent) {
            String compositeKeyString = buildCompositeKey(values);
            if (!compositeKeyString.isEmpty()) {
                removeCacheByKey(name, compositeKeyString);
            }
        }
    }

    /**
     * 删除所有字段上带 @Cache 注解的缓存
     *
     * @param entity 实体对象
     * @param clazz  实体类
     */
    protected void removeFieldCaches(T entity, Class<?> clazz) {
        Map<String, Field> cacheFields = findAllCacheFields(clazz);
        for (Map.Entry<String, Field> entry : cacheFields.entrySet()) {
            Field field = entry.getValue();
            Cache cache = field.getAnnotation(Cache.class);
            if (cache != null) {
                Object key = getFieldValue(entity, field);
                if (key != null) {
                    // 获取name属性值，如果为空则使用空字符串（表示使用默认配置）
                    String cacheName = cache.name();
                    String nameForConfig = cacheName.isEmpty() ? "" : cacheName;
                    removeCacheByKey(nameForConfig, key);
                }
            }
        }
    }

    // ==================== 清空缓存 - 单值缓存 ====================

    /**
     * 清空默认单值缓存
     */
    public void clearCache() {
        try {
            CacheConfig config = getConfig();
            cacheService.clear(config.getName());
        } catch (Throwable e) {
            if (log.isWarnEnabled())
                log.warn("清空缓存失败: error={}", e.getMessage());
        }
    }

    /**
     * 清空指定命名的单值缓存
     *
     * @param naming name属性值，如果为空则清空默认缓存
     */
    public void clearCache(String naming) {
        try {
            CacheConfig config = getConfig(naming);
            cacheService.clear(config.getName());
        } catch (Throwable e) {
            if (log.isWarnEnabled())
                log.warn("清空命名缓存失败: naming={}, error={}", naming, e.getMessage());
        }
    }

    /**
     * 清空指定类型的单值缓存
     *
     * @param clazz 目标类型
     * @param <X>   类型泛型
     */
    public <X> void clearCache(Class<X> clazz) {
        if (clazz == null) {
            return;
        }
        try {
            CacheConfig config = getConfig(clazz);
            cacheService.clear(config.getName());
        } catch (Throwable e) {
            if (log.isWarnEnabled())
                log.warn("清空类型缓存失败: clazz={}, error={}", clazz.getSimpleName(), e.getMessage());
        }
    }

    /**
     * 清空指定类型和命名的单值缓存
     *
     * @param clazz  目标类型
     * @param naming name属性值，如果为空则清空默认类型缓存
     * @param <X>    类型泛型
     */
    public <X> void clearCache(Class<X> clazz, String naming) {
        if (clazz == null) {
            return;
        }
        try {
            CacheConfig config = getConfig(clazz, naming);
            cacheService.clear(config.getName());
        } catch (Throwable e) {
            if (log.isWarnEnabled())
                log.warn("清空类型命名缓存失败: clazz={}, naming={}, error={}", clazz.getSimpleName(), naming, e.getMessage());
        }
    }

    // ==================== 清空缓存 - List缓存 ====================

    /**
     * 清空默认List缓存
     */
    public void clearListCache() {
        try {
            CacheConfig config = getListConfig();
            cacheService.clear(config.getName());
        } catch (Throwable e) {
            if (log.isWarnEnabled())
                log.warn("清空List缓存失败: error={}", e.getMessage());
        }
    }

    /**
     * 清空指定命名的List缓存
     *
     * @param naming name属性值，如果为空则清空默认List缓存
     */
    public void clearListCache(String naming) {
        try {
            CacheConfig config = getListConfig(naming);
            cacheService.clear(config.getName());
        } catch (Throwable e) {
            if (log.isWarnEnabled())
                log.warn("清空命名List缓存失败: naming={}, error={}", naming, e.getMessage());
        }
    }

    /**
     * 清空指定类型的List缓存
     *
     * @param clazz 目标类型
     * @param <X>   类型泛型
     */
    public <X> void clearListCache(Class<X> clazz) {
        if (clazz == null) {
            return;
        }
        try {
            CacheConfig config = getListConfig(clazz);
            cacheService.clear(config.getName());
        } catch (Throwable e) {
            if (log.isWarnEnabled())
                log.warn("清空类型List缓存失败: clazz={}, error={}", clazz.getSimpleName(), e.getMessage());
        }
    }

    /**
     * 清空指定类型和命名的List缓存
     *
     * @param clazz  目标类型
     * @param naming name属性值，如果为空则清空默认类型List缓存
     * @param <X>    类型泛型
     */
    public <X> void clearListCache(Class<X> clazz, String naming) {
        if (clazz == null) {
            return;
        }
        try {
            CacheConfig config = getListConfig(clazz, naming);
            cacheService.clear(config.getName());
        } catch (Throwable e) {
            if (log.isWarnEnabled())
                log.warn("清空类型命名List缓存失败: clazz={}, naming={}, error={}", clazz.getSimpleName(), naming, e.getMessage());
        }
    }

    // ==================== 清空缓存 - 同时清空单值+List ====================

    /**
     * 清空默认单值缓存和List缓存
     */
    public void clearCacheAll() {
        clearCache();
        clearListCache();
    }

    /**
     * 清空指定命名的单值缓存和List缓存
     *
     * @param naming name属性值，如果为空则清空默认缓存
     */
    public void clearCacheAll(String naming) {
        clearCache(naming);
        clearListCache(naming);
    }

    /**
     * 清空指定类型的单值缓存和List缓存
     *
     * @param clazz 目标类型
     * @param <X>   类型泛型
     */
    public <X> void clearCacheAll(Class<X> clazz) {
        clearCache(clazz);
        clearListCache(clazz);
    }

    /**
     * 清空指定类型和命名的单值缓存和List缓存
     *
     * @param clazz  目标类型
     * @param naming name属性值
     * @param <X>    类型泛型
     */
    public <X> void clearCacheAll(Class<X> clazz, String naming) {
        clearCache(clazz, naming);
        clearListCache(clazz, naming);
    }

    /**
     * 清空当前实体相关的所有缓存（包括所有维度：默认/命名/类型/命名+类型，单值+List）
     * <p>
     * 通过扫描 configs 中所有以当前实体基础名称为前缀的配置，清空所有相关缓存。
     * 适用于需要完全重置某个实体所有缓存的场景。
     */
    public void clearCacheComplete() {
        String baseName = getEntityBaseName();
        if (baseName == null) {
            return;
        }
        for (Map.Entry<String, CacheConfig> entry : configs.entrySet()) {
            String configName = entry.getKey();
            if (configName.startsWith(baseName)) {
                try {
                    cacheService.clear(configName);
                } catch (Throwable e) {
                    if (log.isWarnEnabled())
                        log.warn("清空缓存失败: config={}, error={}", configName, e.getMessage());
                }
            }
        }
    }

    /**
     * 获取实体字段的值
     */
    protected Object getFieldValue(T entity, Field field) {
        try {
            field.setAccessible(true);
            return field.get(entity);
        } catch (Throwable e) {
            if (log.isWarnEnabled())
                log.warn("获取实体:{}", e.getMessage());
            return null;
        }
    }

    /**
     * 将值转换为目标类型
     */
    protected Object convert(Object value, Class<?> target) {
        if (value == null) {
            return null;
        }
        // 如果已经是目标类型，直接返回
        if (target.isAssignableFrom(value.getClass())) {
            return value;
        }
        String stringValue = value.toString().trim();
        try {
            // 处理基本类型和包装类型
            if (target == String.class) {
                return stringValue;
            } else if (target == Integer.class || target == int.class) {
                if (stringValue.isEmpty()) {
                    return target.isPrimitive() ? 0 : null;
                }
                return Integer.parseInt(stringValue);
            } else if (target == Long.class || target == long.class) {
                if (stringValue.isEmpty()) {
                    return target.isPrimitive() ? 0L : null;
                }
                return Long.parseLong(stringValue);
            } else if (target == Double.class || target == double.class) {
                if (stringValue.isEmpty()) {
                    return target.isPrimitive() ? 0.0 : null;
                }
                return Double.parseDouble(stringValue);
            } else if (target == Float.class || target == float.class) {
                if (stringValue.isEmpty()) {
                    return target.isPrimitive() ? 0.0f : null;
                }
                return Float.parseFloat(stringValue);
            } else if (target == Boolean.class || target == boolean.class) {
                if (stringValue.isEmpty()) {
                    return target.isPrimitive() ? Boolean.FALSE : null;
                }
                return Boolean.parseBoolean(stringValue);
            } else if (target == BigDecimal.class) {
                return stringValue.isEmpty() ? null : new BigDecimal(stringValue);
            } else if (target.isEnum()) {
                // 处理枚举类型
                Object[] enumConstants = target.getEnumConstants();
                if (enumConstants != null) {
                    for (Object enumConstant : enumConstants) {
                        if (enumConstant.toString().equals(stringValue)) {
                            return enumConstant;
                        }
                    }
                }
            }
        } catch (Throwable e) {
            if (log.isWarnEnabled())
                log.warn("转换失败:{}", e.getMessage());
            return null;
        }
        return null;
    }
}
