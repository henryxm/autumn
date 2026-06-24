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

/**
 * 带 {@code @FieldEncrypt} 实体的模块 Service：写前 {@link FieldEncryptService#onWrite}、读后 {@link FieldEncryptService#onRead}。
 * <p>
 * 不需要字段加解密的 Service 继续继承 {@link ModuleService}（CRUD 与 {@code @Cache} 零加密开销）。
 * {@code baseMapper} 自定义查询返回实体时，必须 {@link #afterRead(Object)} / {@link #afterRead(List)}。
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
        encrypt.onWrite(entity);
        return super.insert(entity);
    }

    @Override
    public boolean insertAllColumn(T entity) {
        encrypt.onWrite(entity);
        return super.insertAllColumn(entity);
    }

    @Override
    public boolean insertBatch(List<T> entities) {
        encrypt.onWrite(entities);
        return super.insertBatch(entities);
    }

    @Override
    public boolean insertBatch(List<T> entities, int batchSize) {
        encrypt.onWrite(entities);
        return super.insertBatch(entities, batchSize);
    }

    @Override
    public boolean insertOrUpdate(T entity) {
        encrypt.onWrite(entity);
        return super.insertOrUpdate(entity);
    }

    @Override
    public boolean insertOrUpdateAllColumn(T entity) {
        encrypt.onWrite(entity);
        return super.insertOrUpdateAllColumn(entity);
    }

    @Override
    public boolean insertOrUpdateBatch(List<T> entities) {
        encrypt.onWrite(entities);
        return super.insertOrUpdateBatch(entities);
    }

    @Override
    public boolean insertOrUpdateBatch(List<T> entities, int batchSize) {
        encrypt.onWrite(entities);
        return super.insertOrUpdateBatch(entities, batchSize);
    }

    @Override
    public boolean insertOrUpdateAllColumnBatch(List<T> entities) {
        encrypt.onWrite(entities);
        return super.insertOrUpdateAllColumnBatch(entities);
    }

    @Override
    public boolean insertOrUpdateAllColumnBatch(List<T> entities, int batchSize) {
        encrypt.onWrite(entities);
        return super.insertOrUpdateAllColumnBatch(entities, batchSize);
    }

    @Override
    public boolean updateById(T entity) {
        encrypt.onWrite(entity);
        return super.updateById(entity);
    }

    @Override
    public boolean updateAllColumnById(T entity) {
        encrypt.onWrite(entity);
        return super.updateAllColumnById(entity);
    }

    @Override
    public boolean update(T entity, Wrapper<T> wrapper) {
        encrypt.onWrite(entity);
        return super.update(entity, wrapper);
    }

    @Override
    public boolean updateBatchById(List<T> entities) {
        encrypt.onWrite(entities);
        return super.updateBatchById(entities);
    }

    @Override
    public boolean updateBatchById(List<T> entities, int batchSize) {
        encrypt.onWrite(entities);
        return super.updateBatchById(entities, batchSize);
    }

    @Override
    public boolean updateAllColumnBatchById(List<T> entities) {
        encrypt.onWrite(entities);
        return super.updateAllColumnBatchById(entities);
    }

    @Override
    public boolean updateAllColumnBatchById(List<T> entities, int batchSize) {
        encrypt.onWrite(entities);
        return super.updateAllColumnBatchById(entities, batchSize);
    }

    @Override
    public T selectById(Serializable id) {
        return encrypt.onRead(super.selectById(id));
    }

    @Override
    public List<T> selectBatchIds(Collection<? extends Serializable> idList) {
        return encrypt.onRead(super.selectBatchIds(idList));
    }

    @Override
    public List<T> selectList(Wrapper<T> wrapper) {
        return encrypt.onRead(super.selectList(wrapper));
    }

    @Override
    public T selectOne(Wrapper<T> wrapper) {
        return encrypt.onRead(super.selectOne(wrapper));
    }

    @Override
    public Page<T> selectPage(Page<T> page, Wrapper<T> wrapper) {
        Page<T> result = super.selectPage(page, wrapper);
        if (result != null) {
            encrypt.onRead(result.getRecords());
        }
        return result;
    }
}
