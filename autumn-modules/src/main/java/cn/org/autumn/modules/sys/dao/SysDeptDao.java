package cn.org.autumn.modules.sys.dao;

import cn.org.autumn.modules.sys.dao.sql.SysDeptDaoSql;
import cn.org.autumn.modules.sys.entity.SysDeptEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface SysDeptDao extends BaseMapper<SysDeptEntity> {

    @SelectProvider(type = SysDeptDaoSql.class, method = "getByParentKey")
    List<String> getByParentKey(@Param("value") String parentKey);

    @SelectProvider(type = SysDeptDaoSql.class, method = "getByDeptKey")
    SysDeptEntity getByDeptKey(@Param("deptKey") String deptKey);
}
