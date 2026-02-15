package cn.org.autumn.modules.sys.dao;

import cn.org.autumn.modules.sys.entity.SysUserRoleEntity;
import cn.org.autumn.mybatis.SelectInLangDriver;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface SysUserRoleDao extends BaseMapper<SysUserRoleEntity> {

    @Select("select role_key from sys_user_role where user_uuid = #{userUuid}")
    List<String> getRoleKeys(@Param("userUuid") String userUuid);

    @Select("select * from sys_user_role where username = #{username}")
    List<SysUserRoleEntity> getByUsername(@Param("username") String username);

    @Select("select count(*) from sys_user_role where user_uuid = #{userUuid} and role_key = #{roleKey}")
    Integer hasUserRole(@Param("userUuid") String userUuid, @Param("roleKey") String roleKey);

    @Delete("DELETE FROM sys_user_role WHERE role_key IN (#{roleKeys})")
    @Lang(SelectInLangDriver.class)
    int deleteByRoleKeys(@Param("roleKeys") String[] roleKeys);
}
