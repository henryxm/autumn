package cn.org.autumn.modules.sys.dao;

import cn.org.autumn.modules.sys.dao.sql.SysRoleMenuDaoSql;
import cn.org.autumn.modules.sys.entity.SysRoleMenuEntity;
import cn.org.autumn.mybatis.SelectInLangDriver;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface SysRoleMenuDao extends BaseMapper<SysRoleMenuEntity> {

    @DeleteProvider(type = SysRoleMenuDaoSql.class, method = "deleteByRoleKeys")
    @Lang(SelectInLangDriver.class)
    int deleteBatch(@Param("roleKeys") String[] roleKeys);

    @SelectProvider(type = SysRoleMenuDaoSql.class, method = "getMenuKeys")
    List<String> getMenuKeys(@Param("roleKey") String roleKey);

    @DeleteProvider(type = SysRoleMenuDaoSql.class, method = "deleteByRoleKeys")
    @Lang(SelectInLangDriver.class)
    int deleteMenus(@Param("roleKeys") String[] roleKeys);
}
