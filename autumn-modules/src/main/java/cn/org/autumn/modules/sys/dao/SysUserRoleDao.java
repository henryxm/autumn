package cn.org.autumn.modules.sys.dao;

import cn.org.autumn.modules.sys.entity.SysUserRoleEntity;
import cn.org.autumn.mybatis.SelectInLangDriver;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface SysUserRoleDao extends BaseMapper<SysUserRoleEntity> {

    /**
     * 根据用户ID，获取角色ID列表
     */
    @Select("select role_id from sys_user_role where user_id = #{value}")
    List<Long> queryRoleIdList(@Param("value") Long userId);

    @Select("select role_key from sys_user_role where user_uuid = #{userUuid}")
    List<String> getRoleKeys(@Param("userUuid") String userUuid);

    @Select("select count(*) from sys_user_role where user_uuid = #{userUuid} and role_key = #{roleKey}")
    Integer hasUserRole(@Param("userUuid") String userUuid, @Param("roleKey") String roleKey);

    /**
     * 根据角色ID数组，批量删除
     */
    @Delete("DELETE FROM sys_user_role WHERE role_id IN (#{roleIds})")
    @Lang(SelectInLangDriver.class)
    int deleteBatch(@Param("roleIds") Long[] roleIds);

    @Delete("DELETE FROM sys_user_role WHERE role_key IN (#{roleKeys})")
    int deleteByRoleKeys(@Param("roleIds") String[] roleKeys);
}
