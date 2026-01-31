package cn.org.autumn.base;

import cn.org.autumn.annotation.Cache;
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

    public long cacheExpire() {
        return 10;
    }

    public boolean cacheNull() {
        return false;
    }

    public CacheConfig getConfig() {
        String name = getModelClass().getSimpleName().replace("Entity", "").toLowerCase();
        CacheConfig config = configs.get(name);
        if (null == config) {
            config = CacheConfig.builder().name(name).key(String.class).value(getModelClass()).expire(cacheExpire()).Null(cacheNull()).build();
        }
        return config;
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
        if (key == null) {
            return null;
        }
        Class<?> entityClass = getModelClass();
        if (entityClass == null) {
            return key;
        }
        // 检查类上是否有 @Cache 注解（复合字段缓存）
        Cache classCache = entityClass.getAnnotation(Cache.class);
        if (classCache != null && classCache.value().length > 0) {
            // 单个参数在复合字段模式下，如果是 Map，从 Map 中提取值构建字符串 key
            if (key instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> keyMap = (Map<String, Object>) key;
                String[] fieldNames = classCache.value();
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
     * 构建缓存 key（可变参数版本）
     * 对于复合字段，将可变参数转换为字符串 key（使用分隔符连接）
     *
     * @param keys 可变参数数组
     * @return 缓存 key（字符串或单个值）
     */
    private Object buildCacheKey(Object... keys) {
        if (keys == null || keys.length == 0) {
            return null;
        }
        Class<?> entityClass = getModelClass();
        if (entityClass == null) {
            return keys.length == 1 ? keys[0] : buildCompositeKeyString(keys);
        }
        // 检查类上是否有 @Cache 注解（复合字段缓存）
        Cache classCache = entityClass.getAnnotation(Cache.class);
        if (classCache != null && classCache.value().length > 0) {
            String[] fieldNames = classCache.value();
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
     *
     * @param key 指定的实体key（单个值）
     * @return 返回实体类型的对象， 如果未使用@Cache指定字段，则返回null
     */
    public T getEntity(Object key) {
        if (key == null) {
            return null;
        }
        Class<?> entityClass = getModelClass();
        if (entityClass == null) {
            return null;
        }
        // 检查类上是否有 @Cache 注解（复合字段缓存）
        Cache classCache = entityClass.getAnnotation(Cache.class);
        if (classCache != null && classCache.value().length > 0) {
            String[] fieldNames = classCache.value();
            // 如果是 Map，使用 Map 方式
            if (key instanceof Map) {
                return getEntityByCompositeFields(key, entityClass, fieldNames);
            }
            // 单个参数在复合字段模式下，参数数量不匹配，返回 null
            return null;
        }
        // 查找 @Cache 标注的字段（单个字段缓存）
        Field cacheField = findCacheField(entityClass);
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
     * 通过反射获取对象实体（可变参数版本，兼容性）
     * 具体实现类可以实现该方法，可提高消息，如果实例类为实现该方法，可以使用@Cache标注对应字段，系统将通过该字段，使用反射获取一条实例
     * 应保证使用@Cache标注的字段值的唯一性，因为默认实现只会去一条有效值
     * <p>
     * 支持两种方式：
     * 1. 在字段上标注 @Cache：使用单个字段作为缓存 key - getEntity(key)（推荐使用单参数版本 getEntity(Object key)）
     * 2. 在类上标注 @Cache(value = {"field1", "field2"})：使用多个字段组合作为复合缓存 key - getEntity(value1, value2, ...)
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
        // 检查类上是否有 @Cache 注解（复合字段缓存）
        Cache classCache = entityClass.getAnnotation(Cache.class);
        if (classCache != null && classCache.value().length > 0) {
            String[] fieldNames = classCache.value();
            // 如果只有一个参数且是 Map，使用 Map 方式
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
     * 查找实体类中标注了 @Unique 的字段
     */
    private Field findCacheField(Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.getAnnotation(Cache.class) != null) {
                field.setAccessible(true);
                return field;
            }
        }
        // 检查父类
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && !superClass.equals(Object.class)) {
            return findCacheField(superClass);
        }
        return null;
    }

    /**
     * 根据 key 删除缓存
     */
    private void removeCacheByKey(Object key) {
        if (key == null) {
            return;
        }
        try {
            CacheConfig config = getConfig();
            String cacheName = config.getName();
            cacheService.remove(cacheName, key);
        } catch (Throwable e) {
            if (log.isWarnEnabled())
                log.warn("删除缓存:{}", e.getMessage());
        }
    }

    /**
     * 根据实体对象删除缓存
     * 从实体中提取 @Cache 标注的字段值作为缓存 key
     * 支持单个字段和复合字段两种方式
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
            // 检查类上是否有 @Cache 注解（复合字段缓存）
            Cache classCache = entityClass.getAnnotation(Cache.class);
            if (classCache != null && classCache.value().length > 0) {
                // 使用复合字段构建字符串缓存 key（与 buildCacheKey 保持一致）
                String[] fieldNames = classCache.value();
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
                        removeCacheByKey(compositeKeyString);
                    }
                }
                return;
            }
            // 查找 @Cache 标注的字段（单个字段缓存）
            Field cacheField = findCacheField(entityClass);
            if (cacheField == null) {
                return;
            }
            // 获取字段值作为缓存 key
            Object key = getFieldValue(entity, cacheField);
            if (key != null) {
                removeCacheByKey(key);
            }
        } catch (Throwable e) {
            if (log.isWarnEnabled())
                log.warn("清除实体:{}", e.getMessage());
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