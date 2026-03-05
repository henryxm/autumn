package cn.org.autumn.service;

import cn.org.autumn.model.Parameterized;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.io.Serializable;
import java.util.List;

public class CompatibleService<M extends BaseMapper<T>, T> extends ServiceImpl<M, T> implements Parameterized {

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