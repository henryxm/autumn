package cn.org.autumn.modules.sys.dao;

import cn.org.autumn.modules.sys.entity.SysRoleMenuEntity;
import cn.org.autumn.mybatis.SelectInLangDriver;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface SysRoleMenuDao extends BaseMapper<SysRoleMenuEntity> {

    @Delete("DELETE FROM sys_role_menu WHERE role_key IN (#{roleKeys})")
    @Lang(SelectInLangDriver.class)
    int deleteBatch(@Param("roleKeys") String[] roleKeys);

    @Select("select menu_key from sys_role_menu where role_key = #{roleKey}")
    List<String> getMenuKeys(@Param("roleKey") String roleKey);

    @Delete("DELETE FROM sys_role_menu WHERE role_key IN (#{roleKeys})")
    @Lang(SelectInLangDriver.class)
    int deleteMenus(@Param("roleKeys") String[] roleKeys);
}
