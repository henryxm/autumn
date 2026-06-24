package cn.org.autumn.base;

import cn.org.autumn.annotation.FieldEncrypt;
import cn.org.autumn.config.CacheConfig;
import cn.org.autumn.crypto.FieldEncryptCacheKey;
import cn.org.autumn.crypto.FieldEncryptCacheLookup;
import cn.org.autumn.crypto.FieldEncryptService;
import cn.org.autumn.table.utils.HumpConvert;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
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
    protected boolean tryEncryptCacheEq(QueryWrapper<T> wrapper, String fieldName, Object value) {
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

    private void encryptWrite(T entity) {
        encrypt.onWrite(entity);
    }

    private void encryptWrite(Collection<T> entities) {
        if (entities == null) {
            return;
        }
        for (T entity : entities) {
            encrypt.onWrite(entity);
        }
    }

    @Override
    public int insert(T entity) {
        encryptWrite(entity);
        return super.insert(entity);
    }

    @Override
    public boolean insertOrUpdate(T entity) {
        encryptWrite(entity);
        return super.insertOrUpdate(entity);
    }

    @Override
    public boolean insertBatch(Collection<T> entityList) {
        encryptWrite(entityList);
        return super.insertBatch(entityList);
    }

    @Override
    public boolean insertBatch(Collection<T> entityList, int batchSize) {
        encryptWrite(entityList);
        return super.insertBatch(entityList, batchSize);
    }

    @Override
    public boolean insertOrUpdateBatch(Collection<T> entityList) {
        encryptWrite(entityList);
        return super.insertOrUpdateBatch(entityList);
    }

    @Override
    public boolean insertOrUpdateBatch(Collection<T> entityList, int batchSize) {
        encryptWrite(entityList);
        return super.insertOrUpdateBatch(entityList, batchSize);
    }

    @Override
    public int updateAllColumnById(T entity) {
        encryptWrite(entity);
        return super.updateAllColumnById(entity);
    }

    @Override
    public boolean updateAllColumn(T entity, Wrapper<T> updateWrapper) {
        encryptWrite(entity);
        return super.updateAllColumn(entity, updateWrapper);
    }

    @Override
    public boolean save(T entity) {
        encryptWrite(entity);
        return super.save(entity);
    }

    @Override
    public boolean saveOrUpdate(T entity) {
        encryptWrite(entity);
        return super.saveOrUpdate(entity);
    }

    @Override
    public boolean saveBatch(Collection<T> entityList) {
        encryptWrite(entityList);
        return super.saveBatch(entityList);
    }

    @Override
    public boolean saveBatch(Collection<T> entityList, int batchSize) {
        encryptWrite(entityList);
        return super.saveBatch(entityList, batchSize);
    }

    @Override
    public boolean saveOrUpdateBatch(Collection<T> entityList) {
        encryptWrite(entityList);
        return super.saveOrUpdateBatch(entityList);
    }

    @Override
    public boolean saveOrUpdateBatch(Collection<T> entityList, int batchSize) {
        encryptWrite(entityList);
        return super.saveOrUpdateBatch(entityList, batchSize);
    }

    @Override
    public boolean updateById(T entity) {
        encryptWrite(entity);
        return super.updateById(entity);
    }

    @Override
    public boolean update(T entity, Wrapper<T> updateWrapper) {
        encryptWrite(entity);
        return super.update(entity, updateWrapper);
    }

    @Override
    public boolean updateBatchById(Collection<T> entityList) {
        encryptWrite(entityList);
        return super.updateBatchById(entityList);
    }

    @Override
    public boolean updateBatchById(Collection<T> entityList, int batchSize) {
        encryptWrite(entityList);
        return super.updateBatchById(entityList, batchSize);
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
    public List<T> selectList(Wrapper<T> queryWrapper) {
        return encrypt.onRead(super.selectList(queryWrapper));
    }

    @Override
    public T selectOne(Wrapper<T> queryWrapper) {
        return encrypt.onRead(super.selectOne(queryWrapper));
    }

    @Override
    public <E extends IPage<T>> E selectPage(E page, Wrapper<T> queryWrapper) {
        E result = super.selectPage(page, queryWrapper);
        if (result != null) {
            encrypt.onRead(result.getRecords());
        }
        return result;
    }
}
