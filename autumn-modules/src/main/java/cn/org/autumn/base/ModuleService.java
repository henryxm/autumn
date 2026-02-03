package cn.org.autumn.base;

import cn.org.autumn.annotation.Cache;
import cn.org.autumn.annotation.Caches;
import cn.org.autumn.config.CacheConfig;
import cn.org.autumn.config.Config;
import cn.org.autumn.exception.AException;
import cn.org.autumn.menu.BaseMenu;
import cn.org.autumn.modules.lan.service.Language;
import cn.org.autumn.modules.lan.service.LanguageService;
import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.service.BaseService;
import cn.org.autumn.service.CacheService;
import cn.org.autumn.table.utils.HumpConvert;
import cn.org.autumn.table.utils.Escape;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模块示例基础服务类
 * 自动化处理相关语言及菜单更新
 *
 * @param <M>
 * @param <T>
 */
@Slf4j
public abstract class ModuleService<M extends BaseMapper<T>, T> extends BaseService<M, T> {

    @Autowired
    protected SysMenuService sysMenuService;

    @Autowired
    protected Language language;

    @Autowired
    protected LanguageService languageService;

    @Autowired
    protected SysConfigService sysConfigService;

    @Autowired
    protected CacheService cacheService;

    protected BaseMenu baseMenu;

    static final Map<String, CacheConfig> configs = new ConcurrentHashMap<>();

    public BaseMenu getBaseMenu() {
        if (null != baseMenu)
            return baseMenu;
        String bean = getPrefix() + "Menu";
        Object o = Config.getBean(bean);
        if (o instanceof BaseMenu)
            baseMenu = (BaseMenu) o;
        if (null == baseMenu)
            throw new AException("Module menu default class name is missing, You should implement getBaseMenu by yourself.");
        return baseMenu;
    }

    @Override
    public String parentMenu() {
        getBaseMenu().init();
        SysMenuEntity sysMenuEntity = sysMenuService.getByMenuKey(getBaseMenu().getMenu());
        if (null != sysMenuEntity)
            return sysMenuEntity.getMenuKey();
        return "";
    }

    @Override
    public String menu() {
        Class<?> clazz = getModelClass();
        String menuKey = clazz.getSimpleName();
        if (menuKey.toLowerCase().endsWith("entity")) {
            menuKey = menuKey.substring(0, menuKey.length() - 6);
        }
        return SysMenuService.getMenuKey(getBaseMenu().getNamespace(), menuKey);
    }

    @Override
    public void init() {
        sysMenuService.put(getMenuItemsInternal(), getMenuItems(), getMenuList());
        language.put(getLanguageItemsInternal(), getLanguageItems(), getLanguageList());
    }

    public long getCacheExpire() {
        return 10;
    }

    public boolean isCacheNull() {
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
        Class<?> entityClass = getModelClass();
        if (entityClass == null) {
            return String.class;
        }
        // 检查类上是否有 @Cache 注解（复合字段缓存）
        Cache classCache = entityClass.getAnnotation(Cache.class);
        if (classCache != null && classCache.value().length > 0) {
            // 复合字段缓存，使用String类型作为key
            return String.class;
        }
        // 检查类上是否有 @Caches 注解（多个复合字段缓存）
        Caches caches = entityClass.getAnnotation(Caches.class);
        if (caches != null && caches.value().length > 0) {
            // 多个复合字段缓存，使用String类型作为key
            return String.class;
        }
        // 查找字段上的 @Cache 注解（单个字段缓存）
        Field cacheField = findCacheField(entityClass);
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
        Class<?> entityClass = getModelClass();
        if (entityClass == null) {
            return String.class;
        }
        // 检查类上是否有 @Caches 注解（多个复合字段缓存）
        Caches caches = entityClass.getAnnotation(Caches.class);
        if (caches != null && caches.value().length > 0) {
            // 查找指定name的复合字段缓存
            for (Cache cache : caches.value()) {
                if (cache.value().length > 0 && name != null && name.equals(cache.name())) {
                    // 复合字段缓存，使用String类型作为key
                    return String.class;
                }
            }
        }
        // 查找字段上的 @Cache 注解（单个字段缓存）
        Field cacheField = findCacheField(entityClass, name);
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

    public CacheConfig getConfig() {
        String name = getModelClass().getSimpleName().replace("Entity", "").toLowerCase();
        CacheConfig config = configs.get(name);
        if (null == config) {
            config = CacheConfig.builder().name(name).key(getCacheKeyType()).value(getModelClass()).expire(getCacheExpire()).Null(isCacheNull()).build();
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
        String name = getModelClass().getSimpleName().replace("Entity", "") + naming;
        name = name.toLowerCase();
        CacheConfig config = configs.get(name);
        if (null == config) {
            config = CacheConfig.builder().name(name).key(getCacheKeyType(naming)).value(getModelClass()).expire(getCacheExpire()).Null(isCacheNull()).build();
            configs.put(name, config);
        }
        return config;
    }

    /**
     * 查找类上的@Caches注解
     *
     * @param entityClass 实体类
     * @return Caches注解，如果不存在返回null
     */
    private Caches findCachesAnnotation(Class<?> entityClass) {
        if (entityClass == null) {
            return null;
        }
        return entityClass.getAnnotation(Caches.class);
    }

    /**
     * 查找@Caches注解中指定name的@Cache注解
     *
     * @param entityClass 实体类
     * @param name        name属性值
     * @return Cache注解，如果不存在返回null
     */
    private Cache findCacheInCaches(Class<?> entityClass, String name) {
        if (entityClass == null || name == null || name.isEmpty()) {
            return null;
        }
        Caches caches = findCachesAnnotation(entityClass);
        if (caches != null && caches.value().length > 0) {
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
     * @param entityClass 实体类
     * @return Cache注解，如果不存在返回null
     */
    private Cache findSingleCacheAnnotation(Class<?> entityClass) {
        if (entityClass == null) {
            return null;
        }
        Cache classCache = entityClass.getAnnotation(Cache.class);
        if (classCache != null && classCache.value().length > 0) {
            return classCache;
        }
        return null;
    }

    /**
     * 获取所有@Caches注解中定义的复合key缓存配置
     *
     * @param entityClass 实体类
     * @return Cache注解数组，key为name属性值，value为Cache注解
     */
    private Map<String, Cache> findAllCompositeCaches(Class<?> entityClass) {
        Map<String, Cache> compositeCaches = new HashMap<>();
        if (entityClass == null) {
            return compositeCaches;
        }
        // 检查@Caches注解
        Caches caches = findCachesAnnotation(entityClass);
        if (caches != null && caches.value().length > 0) {
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
        Cache singleCache = findSingleCacheAnnotation(entityClass);
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
            Class<?> entityClass = getModelClass();
            if (entityClass != null && entityClass.isInstance(id)) {
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
            Class<?> entityClass = getModelClass();
            for (Serializable id : idList) {
                // 检查 id 是否是实体类型 T 的实例
                if (entityClass != null && entityClass.isInstance(id)) {
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
     *
     * @param key 缓存 key（单个值）
     * @return 实体对象
     */
    public T getCache(Object key) {
        if (key == null) {
            return null;
        }
        // 构建缓存 key
        Object cacheKey = buildCacheKey(key);
        if (cacheKey == null) {
            return null;
        }
        // 调用单参数版本的 getEntity
        return cacheService.compute(cacheKey, () -> getEntity(key), getConfig());
    }

    /**
     * 根据name属性获取缓存，如果不存在则查询并缓存
     * 用于支持多个不同字段都有唯一值的情况，通过name来区分不同的缓存字段
     * 支持单个字段缓存和复合字段缓存（通过@Caches注解定义）
     *
     * @param name name属性值，用于区分不同的缓存字段或复合key
     * @param key  缓存key（字段值或Map）
     * @return 实体对象
     */
    public T getNameCache(String name, Object key) {
        if (key == null || name == null) {
            return null;
        }
        // 构建缓存 key（支持复合字段）
        Object cacheKey = buildCacheKey(name, key);
        if (cacheKey == null) {
            return null;
        }
        // 使用name对应的配置和getEntity方法
        return cacheService.compute(cacheKey, () -> getNameEntity(name, key), getConfig(name));
    }

    /**
     * 根据name属性获取复合key缓存，如果不存在则查询并缓存
     * 用于支持@Caches注解中定义的多个复合key缓存
     *
     * @param name name属性值，用于区分不同的复合key
     * @param keys 复合key的值数组，按@Cache注解中字段顺序对应
     * @return 实体对象
     */
    public T getNameCache(String name, Object... keys) {
        if (keys == null || keys.length == 0 || name == null) {
            return null;
        }
        // 如果只有一个参数，调用单参数版本
        if (keys.length == 1) {
            return getNameCache(name, keys[0]);
        }
        // 构建缓存 key（复合字段）
        Object cacheKey = buildCacheKey(name, keys);
        if (cacheKey == null) {
            return null;
        }
        // 使用name对应的配置和getEntity方法
        return cacheService.compute(cacheKey, () -> getNameEntity(name, keys), getConfig(name));
    }

    /**
     * 获取缓存，如果不存在则查询并缓存（可变参数版本，兼容性）
     * 获取一条缓存值，如果未能获取到该值，则调用getEntity函数，通过查询数据库获取，实现类可以实现getEntity来提高效率
     * 也可以通过@Cache来标注其字段，通过反射来获取对应字段的实例
     * <p>
     * 支持两种方式：
     * 1. 单个字段缓存：getCache(key) - key 为单个值（推荐使用单参数版本 getCache(Object key)）
     * 2. 复合字段缓存：getCache(value1, value2, ...) - 多个值按 @Cache 注解中字段顺序对应
     *
     * @param keys 缓存 key，可以是单个值或多个值（可变参数）
     * @return 实体对象
     */
    public T getCache(Object... keys) {
        if (keys == null || keys.length == 0) {
            return null;
        }
        // 如果只有一个参数，调用单参数版本（更高效）
        if (keys.length == 1) {
            return getCache(keys[0]);
        }
        // 构建缓存 key
        Object cacheKey = buildCacheKey(keys);
        if (cacheKey == null) {
            return null;
        }
        // 调用可变参数版本的 getEntity
        return cacheService.compute(cacheKey, () -> getEntity(keys), getConfig());
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
     *
     * @param name name属性值，如果为空则使用默认的@Cache注解
     * @param key  单个 key
     * @return 缓存 key
     */
    private Object buildCacheKey(String name, Object key) {
        if (key == null) {
            return null;
        }
        Class<?> entityClass = getModelClass();
        if (entityClass == null) {
            return key;
        }
        // 查找复合字段缓存配置
        Cache compositeCache = findCompositeCache(entityClass, name);
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
                return buildCompositeKeyString(values);
            }
            // 单个参数在复合字段模式下，参数数量不匹配，返回 null
            return null;
        }
        // 单个字段缓存，直接返回 key
        return key;
    }

    /**
     * 查找复合字段缓存配置（支持@Cache和@Caches注解）
     *
     * @param entityClass 实体类
     * @param name        name属性值，如果为空则查找默认的@Cache注解
     * @return Cache注解，如果不存在返回null
     */
    private Cache findCompositeCache(Class<?> entityClass, String name) {
        if (entityClass == null) {
            return null;
        }
        // 如果name为空，查找单个@Cache注解（向前兼容）
        if (name == null || name.isEmpty()) {
            return findSingleCacheAnnotation(entityClass);
        }
        // 如果name不为空，查找@Caches注解中指定name的@Cache注解
        return findCacheInCaches(entityClass, name);
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
     *
     * @param name name属性值，如果为空则使用默认的@Cache注解
     * @param keys 可变参数数组
     * @return 缓存 key（字符串或单个值）
     */
    private Object buildCacheKey(String name, Object... keys) {
        if (keys == null || keys.length == 0) {
            return null;
        }
        Class<?> entityClass = getModelClass();
        if (entityClass == null) {
            return keys.length == 1 ? keys[0] : buildCompositeKeyString(keys);
        }
        // 查找复合字段缓存配置
        Cache compositeCache = findCompositeCache(entityClass, name);
        if (compositeCache != null && compositeCache.value().length > 0) {
            String[] fieldNames = compositeCache.value();
            // 如果只有一个参数且是 Map，从 Map 中提取值构建字符串 key
            if (keys.length == 1 && keys[0] instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> keyMap = (Map<String, Object>) keys[0];
                Object[] values = new Object[fieldNames.length];
                for (int i = 0; i < fieldNames.length; i++) {
                    values[i] = keyMap.get(fieldNames[i]);
                }
                return buildCompositeKeyString(values);
            }
            // 将可变参数值与字段名对应，按字段顺序构建字符串 key
            Object[] values = new Object[fieldNames.length];
            int paramCount = Math.min(keys.length, fieldNames.length);
            for (int i = 0; i < paramCount; i++) {
                if (keys[i] != null) {
                    values[i] = keys[i];
                } else {
                    // 如果参数数量与字段数量不匹配，返回 null
                    return null;
                }
            }
            // 如果参数数量与字段数量不匹配，返回 null
            if (paramCount != fieldNames.length) {
                return null;
            }
            return buildCompositeKeyString(values);
        }
        // 单个字段缓存，返回第一个参数
        return keys[0];
    }

    /**
     * 构建复合 key 字符串
     * 使用 ":" 作为分隔符连接多个值
     *
     * @param values 值数组
     * @return 组合后的 key 字符串
     */
    private String buildCompositeKeyString(Object... values) {
        if (values == null || values.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(":");
            }
            sb.append(values[i] != null ? values[i].toString() : "null");
        }
        return sb.toString();
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
        return getEntity("", key);
    }

    /**
     * 通过反射获取对象实体（带name参数版本，支持Caches注解）
     *
     * @param name name属性值，如果为空则使用默认的@Cache注解
     * @param key  指定的实体key（单个值）
     * @return 返回实体类型的对象， 如果未使用@Cache指定字段，则返回null
     */
    private T getEntity(String name, Object key) {
        if (key == null) {
            return null;
        }
        Class<?> entityClass = getModelClass();
        if (entityClass == null) {
            return null;
        }
        // 查找复合字段缓存配置（支持@Cache和@Caches注解）
        Cache compositeCache = findCompositeCache(entityClass, name);
        if (compositeCache != null && compositeCache.value().length > 0) {
            String[] fieldNames = compositeCache.value();
            // 如果是 Map，使用 Map 方式
            if (key instanceof Map) {
                return getEntityByCompositeFields(key, entityClass, fieldNames);
            }
            // 单个参数在复合字段模式下，参数数量不匹配，返回 null
            return null;
        }
        // 查找 @Cache 标注的字段（单个字段缓存）
        Field cacheField = findCacheField(entityClass, name);
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

    /**
     * 根据name属性获取实体（支持单个字段和复合字段）
     * 支持@Caches注解中定义的复合key缓存
     *
     * @param name name属性值，用于区分不同的缓存字段或复合key
     * @param key  缓存key（字段值、Map或可变参数）
     * @return 实体对象
     */
    public T getNameEntity(String name, Object key) {
        if (key == null || name == null) {
            return null;
        }
        return getEntity(name, key);
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
        Class<?> entityClass = getModelClass();
        if (entityClass == null) {
            return null;
        }
        // 查找复合字段缓存配置（支持@Caches注解）
        Cache compositeCache = findCompositeCache(entityClass, name);
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
            return getEntityByCompositeFields(fieldValues, entityClass, fieldNames);
        }
        // 查找 @Cache 标注的字段（单个字段缓存）
        Field cacheField = findCacheField(entityClass, name);
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
        Class<?> entityClass = getModelClass();
        if (entityClass == null) {
            return null;
        }
        // 查找复合字段缓存配置（支持@Cache和@Caches注解，默认使用不带name的）
        Cache compositeCache = findCompositeCache(entityClass, "");
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
            return getEntityByCompositeFields(fieldValues, entityClass, fieldNames);
        }
        // 查找 @Cache 标注的字段（单个字段缓存）
        Field cacheField = findCacheField(entityClass);
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
     * 使用复合字段查询实体
     *
     * @param key         缓存 key，可以是 Map（key 为字段名）或对象（从中提取字段值）
     * @param entityClass 实体类
     * @param fieldNames  字段名数组
     * @return 实体对象
     */
    private T getEntityByCompositeFields(Object key, Class<?> entityClass, String[] fieldNames) {
        Map<String, Object> fieldValues = extractFieldValues(key, entityClass, fieldNames);
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
     * 从 key 中提取字段值
     *
     * @param key         缓存 key，可以是 Map 或对象
     * @param entityClass 实体类
     * @param fieldNames  字段名数组
     * @return 字段值 Map，key 为字段名，value 为字段值
     */
    private Map<String, Object> extractFieldValues(Object key, Class<?> entityClass, String[] fieldNames) {
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
        if (entityClass.isInstance(key)) {
            @SuppressWarnings("unchecked")
            T entity = (T) key;
            for (String fieldName : fieldNames) {
                Field field = findField(entityClass, fieldName);
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
     * 根据 key 删除缓存（使用默认配置）
     */
    private void removeCacheByKey(Object key) {
        removeCacheByKey("", key);
    }

    /**
     * 根据 name 和 key 删除缓存
     *
     * @param name name属性值，如果为空则使用默认配置
     * @param key  缓存key
     */
    private void removeCacheByKey(String name, Object key) {
        if (key == null) {
            return;
        }
        try {
            CacheConfig config = (name == null || name.isEmpty()) ? getConfig() : getConfig(name);
            String cacheName = config.getName();
            cacheService.remove(cacheName, key);
        } catch (Throwable e) {
            if (log.isWarnEnabled())
                log.warn("删除缓存失败: name={}, key={}, error={}", name, key, e.getMessage());
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
    private void removeCacheByEntity(T entity) {
        if (entity == null) {
            return;
        }
        try {
            Class<?> entityClass = getModelClass();
            if (entityClass == null) {
                return;
            }
            // 1. 删除所有复合字段缓存（包括@Cache和@Caches中定义的）
            removeAllCompositeCaches(entity, entityClass);
            // 2. 删除所有字段上带 @Cache 注解的缓存（包括带name和不带name的）
            removeFieldCaches(entity, entityClass);
        } catch (Throwable e) {
            if (log.isWarnEnabled())
                log.warn("清除实体缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 删除所有复合字段缓存（支持@Cache和@Caches注解）
     *
     * @param entity     实体对象
     * @param entityClass 实体类
     */
    private void removeAllCompositeCaches(T entity, Class<?> entityClass) {
        Map<String, Cache> compositeCaches = findAllCompositeCaches(entityClass);
        for (Map.Entry<String, Cache> entry : compositeCaches.entrySet()) {
            String name = entry.getKey();
            Cache cache = entry.getValue();
            if (cache != null && cache.value().length > 0) {
                // 删除复合字段缓存
                removeCompositeCache(entity, entityClass, cache.value(), name);
            }
        }
    }

    /**
     * 删除复合字段缓存（使用默认配置）
     *
     * @param entity      实体对象
     * @param entityClass 实体类
     * @param fieldNames  字段名数组
     */
    private void removeCompositeCache(T entity, Class<?> entityClass, String[] fieldNames) {
        removeCompositeCache(entity, entityClass, fieldNames, "");
    }

    /**
     * 删除复合字段缓存（支持指定name配置）
     *
     * @param entity      实体对象
     * @param entityClass 实体类
     * @param fieldNames  字段名数组
     * @param name        name属性值，如果为空则使用默认配置
     */
    private void removeCompositeCache(T entity, Class<?> entityClass, String[] fieldNames, String name) {
        Object[] values = new Object[fieldNames.length];
        boolean allValuesPresent = true;

        for (int i = 0; i < fieldNames.length; i++) {
            Field field = findField(entityClass, fieldNames[i]);
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
            String compositeKeyString = buildCompositeKeyString(values);
            if (!compositeKeyString.isEmpty()) {
                removeCacheByKey(name, compositeKeyString);
            }
        }
    }

    /**
     * 删除所有字段上带 @Cache 注解的缓存
     *
     * @param entity      实体对象
     * @param entityClass 实体类
     */
    private void removeFieldCaches(T entity, Class<?> entityClass) {
        Map<String, Field> cacheFields = findAllCacheFields(entityClass);
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

    /**
     * 获取实体字段的值
     */
    private Object getFieldValue(T entity, Field field) {
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
    private Object convert(Object value, Class<?> target) {
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