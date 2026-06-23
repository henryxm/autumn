package cn.org.autumn.base;

import cn.org.autumn.crypto.FieldEncryptService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * 带 {@code @FieldEncrypt} 实体的模块 Service：写前 {@link FieldEncryptService#onWrite}、读后 {@link FieldEncryptService#onRead}。
 * <p>
 * 不需要字段加解密的 Service 继续继承 {@link ModuleService}（CRUD 零开销）。
 * {@code baseMapper} 自定义查询返回实体时，必须 {@link #afterRead(Object)} / {@link #afterRead(List)}。
 * <p>
 * vector / searchable / hash 列 / 运行时开关区别见 {@code docs/AI_FIELD_ENCRYPT.md} §0。
 */
public abstract class EncryptModuleService<M extends BaseMapper<T>, T> extends ModuleService<M, T> {

    @Autowired
    protected FieldEncryptService encrypt;

    protected T afterRead(T entity) {
        return encrypt.onRead(entity);
    }

    protected List<T> afterRead(List<T> entities) {
        return encrypt.onRead(entities);
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
