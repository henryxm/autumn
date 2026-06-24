package cn.org.autumn.base;

import cn.org.autumn.annotation.FieldEncrypt;
import cn.org.autumn.config.CacheConfig;
import cn.org.autumn.crypto.FieldEncryptCacheKey;
import cn.org.autumn.crypto.FieldEncryptCacheLookup;
import cn.org.autumn.crypto.FieldEncryptService;
import cn.org.autumn.table.utils.HumpConvert;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.baomidou.mybatisplus.plugins.Page;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 带 {@code @FieldEncrypt} 实体的模块 Service：写前 {@link FieldEncryptService#onWrite}、写后 {@link FieldEncryptService#restoreAfterWrite}、读路径 {@link FieldEncryptService#onRead}。
 * <p>
 * 不需要字段加解密的 Service 继续继承 {@link ModuleService}（CRUD 与 {@code @Cache} 零加密开销）。
 * {@code ServiceImpl} 全部读方法已自动解密；{@code baseMapper} / Dao 手写 SQL 须 {@link #afterRead(Object)}、{@link #afterReadMap(java.util.Map)} 等（见 {@code docs/AI_FIELD_ENCRYPT.md} §0.5）。
 * <p>
 * 与 {@code @Cache} 钩子见 {@code docs/AI_FIELD_ENCRYPT.md} §7（{@link #isEncryptCacheField}、{@link #isEncryptCacheEntity} 等）。
 */
public abstract class EncryptModuleService<M extends BaseMapper<T>, T> extends ModuleService<M, T> {

    @Autowired
    protected FieldEncryptService encrypt;

    @Override
    protected boolean isEncryptCacheField(String fieldName) {
        Class<?> clazz = getModelClass();
        return clazz != null && fieldName != null && encrypt.isEncryptCacheField(clazz, fieldName);
    }

    @Override
    protected boolean isEncryptCacheNaming(String naming) {
        Class<?> clazz = getModelClass();
        if (clazz == null) {
            return false;
        }
        Field cacheField = findCacheField(clazz, naming == null ? "" : naming);
        return cacheField != null && isEncryptCacheField(cacheField.getName());
    }

    @Override
    protected boolean isEncryptCacheEntity() {
        Class<?> clazz = getModelClass();
        return clazz != null && encrypt.hasEncryptedFields(clazz);
    }

    protected T afterRead(T entity) {
        return encrypt.onRead(entity);
    }

    protected List<T> afterRead(List<T> entities) {
        return encrypt.onRead(entities);
    }

    protected Map<String, Object> afterReadMap(Map<String, Object> row) {
        return encrypt.onReadMap(getModelClass(), row);
    }

    protected List<Map<String, Object>> afterReadMaps(List<Map<String, Object>> rows) {
        return encrypt.onReadMaps(getModelClass(), rows);
    }

    protected Object afterReadScalar(Object value) {
        return encrypt.onReadScalar(value);
    }

    protected List<Object> afterReadScalars(List<Object> values) {
        return encrypt.onReadScalars(values);
    }

    /**
     * 写库后将实体还原为业务明文态（委托 {@link FieldEncryptService#restoreAfterWrite}，幂等）。
     */
    protected T restoreAfterWrite(T entity) {
        return encrypt.restoreAfterWrite(entity);
    }

    protected List<T> restoreAfterWrite(List<T> entities) {
        return encrypt.restoreAfterWrite(entities);
    }

    private void beforeWrite(T entity) {
        encrypt.onWrite(entity);
    }

    private void beforeWrite(List<T> entities) {
        encrypt.onWrite(entities);
    }

    /** 写前加密、委托持久化、{@code finally} 内幂等还原，保持业务侧实体为明文态。 */
    private boolean writeEntity(Supplier<Boolean> persist, T entity) {
        beforeWrite(entity);
        try {
            return persist.get();
        } finally {
            restoreAfterWrite(entity);
        }
    }

    private boolean writeEntities(Supplier<Boolean> persist, List<T> entities) {
        beforeWrite(entities);
        try {
            return persist.get();
        } finally {
            restoreAfterWrite(entities);
        }
    }

    @Override
    protected T prepareCacheValue(T entity) {
        return afterRead(entity);
    }

    @Override
    protected List<T> prepareCacheValueList(List<T> entities) {
        return afterRead(entities);
    }

    @Override
    protected void mirrorEncryptCache(String naming, Object cacheKey, Object loaded) {
        Class<?> clazz = getModelClass();
        if (clazz == null || cacheKey == null || loaded == null) {
            return;
        }
        Field cacheField = findCacheField(clazz, naming == null ? "" : naming);
        if (cacheField == null) {
            return;
        }
        boolean listValue = loaded instanceof List;
        Object sampleEntity = listValue ? null : prepareCacheValue((T) loaded);
        List<FieldEncryptCacheKey> mirrors = encrypt.resolveCacheMirrorKeys(clazz, cacheField.getName(), naming, cacheKey, sampleEntity);
        for (FieldEncryptCacheKey mirror : mirrors) {
            CacheConfig target = listValue ? getListConfig(mirror.getNaming()) : getConfig(mirror.getNaming());
            if (target != null) {
                cacheService.put(target, mirror.getKey(), loaded);
            }
        }
    }

    @Override
    protected boolean tryEncryptCacheEq(EntityWrapper<T> wrapper, String fieldName, Object value) {
        Class<?> clazz = getModelClass();
        if (clazz == null) {
            return false;
        }
        FieldEncryptCacheLookup lookup = encrypt.resolveCacheDbLookup(clazz, fieldName, value);
        if (lookup != null) {
            wrapper.eq(columnInWrapper(lookup.getColumnName()), lookup.getQueryValue());
            return true;
        }
        return false;
    }

    @Override
    protected List<FieldEncryptCacheKey> encryptCacheEvictionKeys(T entity, Field field, String naming) {
        Class<?> clazz = getModelClass();
        if (clazz == null || entity == null || field == null) {
            return Collections.emptyList();
        }
        return encrypt.resolveCacheEvictionKeys(clazz, field.getName(), naming, entity);
    }

    @Override
    protected Object encryptCacheEvictionValue(T entity, String fieldName, Object raw) {
        Class<?> clazz = getModelClass();
        if (clazz == null) {
            return raw;
        }
        return encrypt.normalizeCacheEvictionKey(clazz, fieldName, raw);
    }

    @Override
    protected boolean tryHashQueryCondition(Map<String, Object> condition, Class<?> entityClass, Field field, Object value) {
        FieldEncrypt fieldEncrypt = field.getAnnotation(FieldEncrypt.class);
        if (fieldEncrypt == null || !fieldEncrypt.searchable() || !encrypt.useHashForQuery()) {
            return false;
        }
        String fieldName = field.getName();
        String hashField = fieldEncrypt.hashField().isEmpty() ? fieldName + "Hash" : fieldEncrypt.hashField();
        String hashColumn = HumpConvert.HumpToUnderline(hashField);
        if (condition.containsKey(hashColumn)) {
            return true;
        }
        condition.put(hashColumn, encrypt.hashQueryValue(entityClass, fieldName, value.toString()));
        return true;
    }

    @Override
    public boolean insert(T entity) {
        return writeEntity(() -> super.insert(entity), entity);
    }

    @Override
    public boolean insertAllColumn(T entity) {
        return writeEntity(() -> super.insertAllColumn(entity), entity);
    }

    @Override
    public boolean insertBatch(List<T> entities) {
        return writeEntities(() -> super.insertBatch(entities), entities);
    }

    @Override
    public boolean insertBatch(List<T> entities, int batchSize) {
        return writeEntities(() -> super.insertBatch(entities, batchSize), entities);
    }

    @Override
    public boolean insertOrUpdate(T entity) {
        return writeEntity(() -> super.insertOrUpdate(entity), entity);
    }

    @Override
    public boolean insertOrUpdateAllColumn(T entity) {
        return writeEntity(() -> super.insertOrUpdateAllColumn(entity), entity);
    }

    @Override
    public boolean insertOrUpdateBatch(List<T> entities) {
        return writeEntities(() -> super.insertOrUpdateBatch(entities), entities);
    }

    @Override
    public boolean insertOrUpdateBatch(List<T> entities, int batchSize) {
        return writeEntities(() -> super.insertOrUpdateBatch(entities, batchSize), entities);
    }

    @Override
    public boolean insertOrUpdateAllColumnBatch(List<T> entities) {
        return writeEntities(() -> super.insertOrUpdateAllColumnBatch(entities), entities);
    }

    @Override
    public boolean insertOrUpdateAllColumnBatch(List<T> entities, int batchSize) {
        return writeEntities(() -> super.insertOrUpdateAllColumnBatch(entities, batchSize), entities);
    }

    @Override
    public boolean updateById(T entity) {
        return writeEntity(() -> super.updateById(entity), entity);
    }

    @Override
    public boolean updateAllColumnById(T entity) {
        return writeEntity(() -> super.updateAllColumnById(entity), entity);
    }

    @Override
    public boolean update(T entity, Wrapper<T> wrapper) {
        return writeEntity(() -> super.update(entity, wrapper), entity);
    }

    @Override
    public boolean updateBatchById(List<T> entities) {
        return writeEntities(() -> super.updateBatchById(entities), entities);
    }

    @Override
    public boolean updateBatchById(List<T> entities, int batchSize) {
        return writeEntities(() -> super.updateBatchById(entities, batchSize), entities);
    }

    @Override
    public boolean updateAllColumnBatchById(List<T> entities) {
        return writeEntities(() -> super.updateAllColumnBatchById(entities), entities);
    }

    @Override
    public boolean updateAllColumnBatchById(List<T> entities, int batchSize) {
        return writeEntities(() -> super.updateAllColumnBatchById(entities, batchSize), entities);
    }

    @Override
    public T selectById(Serializable id) {
        return afterRead(super.selectById(id));
    }

    @Override
    public List<T> selectBatchIds(Collection<? extends Serializable> idList) {
        return afterRead(super.selectBatchIds(idList));
    }

    @Override
    public List<T> selectByMap(Map<String, Object> columnMap) {
        return afterRead(super.selectByMap(columnMap));
    }

    @Override
    public T selectOne(Wrapper<T> wrapper) {
        return afterRead(super.selectOne(wrapper));
    }

    @Override
    public Map<String, Object> selectMap(Wrapper<T> wrapper) {
        return afterReadMap(super.selectMap(wrapper));
    }

    @Override
    public Object selectObj(Wrapper<T> wrapper) {
        return afterReadScalar(super.selectObj(wrapper));
    }

    @Override
    public List<T> selectList(Wrapper<T> wrapper) {
        return afterRead(super.selectList(wrapper));
    }

    @Override
    public Page<T> selectPage(Page<T> page) {
        Page<T> result = super.selectPage(page);
        if (result != null) {
            afterRead(result.getRecords());
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> selectMaps(Wrapper<T> wrapper) {
        return afterReadMaps(super.selectMaps(wrapper));
    }

    @Override
    public List<Object> selectObjs(Wrapper<T> wrapper) {
        return afterReadScalars(super.selectObjs(wrapper));
    }

    @Override
    public Page<Map<String, Object>> selectMapsPage(Page page, Wrapper<T> wrapper) {
        Page<Map<String, Object>> result = super.selectMapsPage(page, wrapper);
        if (result != null) {
            afterReadMaps(result.getRecords());
        }
        return result;
    }

    @Override
    public Page<T> selectPage(Page<T> page, Wrapper<T> wrapper) {
        Page<T> result = super.selectPage(page, wrapper);
        if (result != null) {
            afterRead(result.getRecords());
        }
        return result;
    }
}
