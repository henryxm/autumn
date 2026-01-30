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
import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Collection;
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

    public long expire() {
        return 10;
    }

    public CacheConfig getCacheConfig() {
        String name = getModelClass().getSimpleName().replace("Entity", "").toLowerCase();
        CacheConfig config = configs.get(name);
        if (null == config) {
            config = CacheConfig.builder().name(name).key(String.class).value(getModelClass()).expire(expire()).build();
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
     * 获取一条缓存值，如果未能获取到该值，则调用getEntity函数，通过查询数据库获取，实现类可以实现getEntity来提高效率
     * 也可以通过@Cache来标注其字段，通过反射来获取对应字段的实例
     *
     * @param key 缓存值的Key
     * @return 缓存的数据对象
     */
    public T getCache(Object key) {
        return cacheService.compute(key, () -> getEntity(key), getCacheConfig());
    }

    /**
     * 通过反射获取对象实体，具体实现类可以实现该方法，可提高消息，如果实例类为实现该方法，可以使用@Cache标注对应字段，系统将通过该字段，使用反射获取一条实例
     * 应保证使用@Cache标注的字段值的唯一性，因为默认实现只会去一条有效值
     *
     * @param key 指定的实体key
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
        // 查找 @Cache 标注的字段
        Field cacheField = findCacheField(entityClass);
        if (cacheField == null) {
            return null;
        }
        // 将 key 转换为字段类型
        Object convertedKey = convert(key, cacheField.getType());
        if (convertedKey == null) {
            return null;
        }
        // 使用 EntityWrapper 查询
        String fieldName = cacheField.getName();
        String columnName = HumpConvert.HumpToUnderline(fieldName);
        EntityWrapper<T> wrapper = new EntityWrapper<>();
        wrapper.eq(columnName, convertedKey);
        return selectOne(wrapper);
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
            CacheConfig config = getCacheConfig();
            String cacheName = config.getName();
            cacheService.remove(cacheName, key);
        } catch (Throwable e) {
            if (log.isWarnEnabled())
                log.warn("删除缓存:{}", e.getMessage());
        }
    }

    /**
     * 根据实体对象删除缓存
     * 从实体中提取 @Unique 或 @TableId 字段的值作为缓存 key
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
            // 查找 @Unique 标注的字段
            Field uniqueField = findCacheField(entityClass);
            if (uniqueField == null) {
                return;
            }
            // 获取字段值作为缓存 key
            Object key = getFieldValue(entity, uniqueField);
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