package cn.org.autumn.modules.sys.dao;

import cn.org.autumn.modules.sys.dao.sql.SysRoleDeptDaoSql;
import cn.org.autumn.modules.sys.entity.SysRoleDeptEntity;
import cn.org.autumn.mybatis.SelectInLangDriver;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface SysRoleDeptDao extends BaseMapper<SysRoleDeptEntity> {

    /**
     * 根据角色key，获取部门Key列表
     */
    @SelectProvider(type = SysRoleDeptDaoSql.class, method = "getDeptKeys")
    @Lang(SelectInLangDriver.class)
    List<String> getDeptKeys(@Param("roleKeys") String[] roleKeys);

    /**
     * 根据角色Key数组，批量删除
     */
    @DeleteProvider(type = SysRoleDeptDaoSql.class, method = "deleteByRoleKey")
    @Lang(SelectInLangDriver.class)
    int deleteByRoleKey(@Param("roleKeys") String[] roleKeys);
}
