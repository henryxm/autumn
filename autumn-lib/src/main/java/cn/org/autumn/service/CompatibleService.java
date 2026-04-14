package cn.org.autumn.service;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class CompatibleService<M extends BaseMapper<T>, T> extends DialectService<M, T> {

    public T selectById(Serializable id) {
        return baseMapper.selectById(id);
    }

    public int insert(T entity) {
        return baseMapper.insert(entity);
    }

    public int updateAllColumnById(T entity) {
        return baseMapper.updateById(entity);
    }

    public boolean updateAllColumn(T entity, Wrapper<T> updateWrapper) {
        return update(entity, updateWrapper);
    }

    public List<T> selectBatchIds(Collection<? extends Serializable> idList) {
        return baseMapper.selectByIds(idList);
    }

    public List<T> selectByMap(Map<String, Object> columnMap) {
        return baseMapper.selectByMap(columnMap);
    }

    public T selectOne(Wrapper<T> queryWrapper) {
        return baseMapper.selectOne(queryWrapper);
    }

    public Integer selectCount(Wrapper<T> queryWrapper) {
        Long count = baseMapper.selectCount(queryWrapper);
        return count == null ? 0 : count.intValue();
    }

    public List<T> selectList(Wrapper<T> queryWrapper) {
        return baseMapper.selectList(queryWrapper);
    }

    public List<Map<String, Object>> selectMaps(Wrapper<T> queryWrapper) {
        return baseMapper.selectMaps(queryWrapper);
    }

    public <E> List<E> selectObjs(Wrapper<T> queryWrapper) {
        return baseMapper.selectObjs(queryWrapper);
    }

    public <E extends IPage<T>> E selectPage(E page, Wrapper<T> queryWrapper) {
        return page(page, queryWrapper);
    }

    public <E extends IPage<Map<String, Object>>> E selectMapsPage(E page, Wrapper<T> queryWrapper) {
        baseMapper.selectMaps(page, queryWrapper);
        return page;
    }

    public int deleteBatchIds(List<?> entity) {
        return baseMapper.deleteByIds(entity);
    }

    public boolean deleteByMap(Map<String, Object> columnMap) {
        return removeByMap(columnMap);
    }

    public boolean delete(Wrapper<T> wrapper) {
        return remove(wrapper);
    }

    public boolean deleteById(T entity) {
        return removeById(entity);
    }

    public boolean deleteById(Serializable id) {
        return removeById(id);
    }

    public boolean insertOrUpdate(T entity){
        return saveOrUpdate(entity);
    }

    public boolean insertBatch(Collection<T> entityList) {
        return saveBatch(entityList);
    }

    public boolean insertBatch(Collection<T> entityList, int batchSize) {
        return saveBatch(entityList, batchSize);
    }

    public boolean insertOrUpdateBatch(Collection<T> entityList) {
        return saveOrUpdateBatch(entityList);
    }

    public boolean insertOrUpdateBatch(Collection<T> entityList, int batchSize) {
        return saveOrUpdateBatch(entityList, batchSize);
    }
}