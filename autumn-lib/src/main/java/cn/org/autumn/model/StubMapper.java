package cn.org.autumn.model;

import cn.org.autumn.service.DefaultMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.session.ResultHandler;

import java.io.Serializable;
import java.util.*;

/**
 * BaseMapper 的空实现（stub），用于不需要数据库操作的场景。
 * 已适配 MyBatis-Plus 3.5.x API。
 */
public class StubMapper implements DefaultMapper {
    @Override
    public int insert(DefaultEntity entity) { return 0; }

    @Override
    public int deleteById(DefaultEntity entity) { return 0; }

    @Override
    public int delete(Wrapper<DefaultEntity> wrapper) { return 0; }

    @Override
    public int updateById(DefaultEntity entity) { return 0; }

    @Override
    public int update(DefaultEntity entity, Wrapper<DefaultEntity> wrapper) { return 0; }

    @Override
    public DefaultEntity selectById(Serializable id) { return null; }

    @Override
    public List<DefaultEntity> selectByIds(Collection<? extends Serializable> idList) { return Collections.emptyList(); }

    @Override
    public void selectByIds(Collection<? extends Serializable> idList, ResultHandler<DefaultEntity> handler) {}

    @Override
    public Long selectCount(Wrapper<DefaultEntity> wrapper) { return 0L; }

    @Override
    public List<DefaultEntity> selectList(Wrapper<DefaultEntity> wrapper) { return Collections.emptyList(); }

    @Override
    public void selectList(Wrapper<DefaultEntity> wrapper, ResultHandler<DefaultEntity> handler) {}

    @Override
    public List<DefaultEntity> selectList(IPage<DefaultEntity> page, Wrapper<DefaultEntity> wrapper) { return Collections.emptyList(); }

    @Override
    public void selectList(IPage<DefaultEntity> page, Wrapper<DefaultEntity> wrapper, ResultHandler<DefaultEntity> handler) {}

    @Override
    public List<Map<String, Object>> selectMaps(Wrapper<DefaultEntity> wrapper) { return Collections.emptyList(); }

    @Override
    public void selectMaps(Wrapper<DefaultEntity> wrapper, ResultHandler<Map<String, Object>> handler) {}

    @Override
    public List<Map<String, Object>> selectMaps(IPage<? extends Map<String, Object>> page, Wrapper<DefaultEntity> wrapper) { return Collections.emptyList(); }

    @Override
    public void selectMaps(IPage<? extends Map<String, Object>> page, Wrapper<DefaultEntity> wrapper, ResultHandler<Map<String, Object>> handler) {}

    @Override
    public <E> List<E> selectObjs(Wrapper<DefaultEntity> wrapper) { return Collections.emptyList(); }

    @Override
    public <E> void selectObjs(Wrapper<DefaultEntity> wrapper, ResultHandler<E> handler) {}
}
