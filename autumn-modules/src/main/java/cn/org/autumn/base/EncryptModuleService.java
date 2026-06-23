package cn.org.autumn.base;

import cn.org.autumn.crypto.FieldEncryptService;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.baomidou.mybatisplus.plugins.Page;
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
