package cn.org.autumn.modules.sys.dao;

import cn.org.autumn.modules.sys.entity.SysUserEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface SysUserDao extends BaseMapper<SysUserEntity> {

    /**
     * 查询用户的所有权限
     *
     * @param userId 用户ID
     */
    @Deprecated
    @Select("select m.perms from sys_user_role ur " +
            "LEFT JOIN sys_role_menu rm on ur.role_id = rm.role_id " +
            "LEFT JOIN sys_menu m on rm.menu_id = m.menu_id " +
            "where ur.user_id = #{userId}")
    List<String> queryAllPerms(@Param("userId") Long userId);

    @Select("select m.perms from sys_user_role ur " +
            "LEFT JOIN sys_role_menu rm on ur.role_id = rm.role_id " +
            "LEFT JOIN sys_menu m on rm.menu_id = m.menu_id " +
            "where ur.user_id = #{userId}")
    List<String> getPermsByUserUuid(@Param("userId") String userUuid);

    /**
     * 查询用户的所有菜单ID
     */
    @Select("select distinct rm.menu_id from sys_user_role ur " +
            "LEFT JOIN sys_role_menu rm on ur.role_id = rm.role_id " +
            "where ur.user_id = #{userId}")
    List<Long> queryAllMenuId(@Param("userId") Long userId);

    @Select("select * from sys_user u where u.username = #{username}")
    SysUserEntity getByUsername(@Param("username") String username);

    @Select("SELECT * FROM sys_user WHERE email=#{email}")
    SysUserEntity getByEmail(@Param("email")String email);

    @Select("SELECT * FROM sys_user WHERE mobile=#{mobile}")
    SysUserEntity getByPhone(@Param("mobile")String mobile);

    @Select("SELECT * FROM sys_user WHERE uuid=#{uuid}")
    SysUserEntity getByUuid(@Param("uuid")String uuid);

}
