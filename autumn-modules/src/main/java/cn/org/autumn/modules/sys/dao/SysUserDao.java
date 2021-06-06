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

    @Select("select m.perms from sys_user_role ur " +
            "LEFT JOIN sys_role_menu rm on ur.role_key = rm.role_key " +
            "LEFT JOIN sys_menu m on rm.menu_key = m.menu_key " +
            "where ur.user_uuid = #{userUuid}")
    List<String> getPermsByUserUuid(@Param("userUuid") String userUuid);

    @Select("select distinct rm.menu_key from sys_user_role ur " +
            "LEFT JOIN sys_role_menu rm on ur.role_key = rm.role_key " +
            "where ur.user_uuid = #{userUuid}")
    List<String> getMenus(@Param("userUuid") String userUuid);

    @Select("select * from sys_user u where u.username = #{username}")
    SysUserEntity getByUsername(@Param("username") String username);

    @Select("SELECT * FROM sys_user WHERE email=#{email}")
    SysUserEntity getByEmail(@Param("email") String email);

    @Select("SELECT * FROM sys_user WHERE mobile=#{mobile}")
    SysUserEntity getByPhone(@Param("mobile") String mobile);

    @Select("SELECT * FROM sys_user WHERE uuid=#{uuid}")
    SysUserEntity getByUuid(@Param("uuid") String uuid);

}
