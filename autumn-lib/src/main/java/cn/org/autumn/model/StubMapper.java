package cn.org.autumn.model;

import cn.org.autumn.service.DefaultMapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import org.apache.ibatis.session.RowBounds;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class StubMapper implements DefaultMapper {
    @Override
    public Integer insert(DefaultEntity entity) {
        return null;
    }

    @Override
    public Integer insertAllColumn(DefaultEntity entity) {
        return null;
    }

    @Override
    public Integer deleteById(Serializable id) {
        return null;
    }

    @Override
    public Integer deleteByMap(Map<String, Object> columnMap) {
        return null;
    }

    @Override
    public Integer delete(Wrapper<DefaultEntity> wrapper) {
        return null;
    }

    @Override
    public Integer deleteBatchIds(Collection<? extends Serializable> idList) {
        return null;
    }

    @Override
    public Integer updateById(DefaultEntity entity) {
        return null;
    }

    @Override
    public Integer updateAllColumnById(DefaultEntity entity) {
        return null;
    }

    @Override
    public Integer update(DefaultEntity entity, Wrapper<DefaultEntity> wrapper) {
        return null;
    }

    @Override
    public DefaultEntity selectById(Serializable id) {
        return null;
    }

    @Override
    public List<DefaultEntity> selectBatchIds(Collection<? extends Serializable> idList) {
        return null;
    }

    @Override
    public List<DefaultEntity> selectByMap(Map<String, Object> columnMap) {
        return null;
    }

    @Override
    public DefaultEntity selectOne(DefaultEntity entity) {
        return null;
    }

    @Override
    public Integer selectCount(Wrapper<DefaultEntity> wrapper) {
        return null;
    }

    @Override
    public List<DefaultEntity> selectList(Wrapper<DefaultEntity> wrapper) {
        return null;
    }

    @Override
    public List<Map<String, Object>> selectMaps(Wrapper<DefaultEntity> wrapper) {
        return null;
    }

    @Override
    public List<Object> selectObjs(Wrapper<DefaultEntity> wrapper) {
        return null;
    }

    @Override
    public List<DefaultEntity> selectPage(RowBounds rowBounds, Wrapper<DefaultEntity> wrapper) {
        return null;
    }

    @Override
    public List<Map<String, Object>> selectMapsPage(RowBounds rowBounds, Wrapper<DefaultEntity> wrapper) {
        return null;
    }
}
