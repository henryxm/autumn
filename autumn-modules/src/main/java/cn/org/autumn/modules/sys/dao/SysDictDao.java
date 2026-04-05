package cn.org.autumn.modules.sys.dao;

import cn.org.autumn.modules.sys.dao.sql.SysDictDaoSql;
import cn.org.autumn.modules.sys.entity.SysDictEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface SysDictDao extends BaseMapper<SysDictEntity> {

    @SelectProvider(type = SysDictDaoSql.class, method = "getByType")
    List<SysDictEntity> getByType(@Param("type") String type);
}
