package cn.org.autumn.modules.sys.dao;

import cn.org.autumn.modules.sys.entity.SysRoleMenuEntity;
import cn.org.autumn.mybatis.SelectInLangDriver;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface SysRoleMenuDao extends BaseMapper<SysRoleMenuEntity> {

    @Select("select menu_id from sys_role_menu where role_id = #{value}")
    List<Long> queryMenuIdList(@Param("value") Long roleId);

    @Delete("DELETE FROM sys_role_menu WHERE role_id IN (#{roleIds})")
    @Lang(SelectInLangDriver.class)
    int deleteBatch(@Param("roleIds") Long[] roleIds);

    @Select("select menu_key from sys_role_menu where role_key = #{roleKey}")
    List<String> getMenuKeys(@Param("roleKey") String roleKey);

    @Delete("DELETE FROM sys_role_menu WHERE role_key IN (#{roleKeys})")
    int deleteMenus(@Param("roleKeys") String[] roleKeys);
}
