package cn.org.autumn.service;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.io.Serializable;
import java.util.List;

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

    public int deleteBatchIds(List<?> entity) {
        return baseMapper.deleteByIds(entity);
    }
}