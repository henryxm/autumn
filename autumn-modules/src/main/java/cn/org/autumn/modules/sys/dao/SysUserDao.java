package cn.org.autumn.modules.sys.dao;

import cn.org.autumn.modules.sys.entity.SysUserEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
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

    @Update("update sys_user set `verify` = #{verify} where `uuid` = #{uuid}")
    void verify(@Param("uuid") String uuid, @Param("verify") int verify);

    @Select("select * from sys_user where `username` = #{username} and `status` >= 0")
    SysUserEntity getByUsername(@Param("username") String username);

    @Select("select * from sys_user where `username` like concat('%', #{username}, '%') and `status` >= 0 limit 1")
    SysUserEntity getByUsernameLike(@Param("username") String username);

    @Select("SELECT * FROM sys_user WHERE `email` = #{email} and `status` >= 0")
    SysUserEntity getByEmail(@Param("email") String email);

    @Select("SELECT * FROM sys_user WHERE `email` like concat('%', #{email}, '%') and `status` >= 0 limit 1")
    SysUserEntity getByEmailLike(@Param("email") String email);

    @Select("SELECT * FROM sys_user WHERE `mobile` = #{mobile} and `status` >= 0")
    SysUserEntity getByPhone(@Param("mobile") String mobile);

    @Select("SELECT * FROM sys_user WHERE `mobile` like concat('%', #{mobile}, '%') and `status` >= 0 limit 1")
    SysUserEntity getByPhoneLike(@Param("mobile") String mobile);

    @Select("SELECT * FROM sys_user WHERE `uuid` = #{uuid} and `status` >= 0")
    SysUserEntity getByUuid(@Param("uuid") String uuid);

    @Select("SELECT * FROM sys_user WHERE `uuid` = #{uuid}")
    SysUserEntity getForDelete(@Param("uuid") String uuid);

    @Select("SELECT * FROM sys_user WHERE `uuid` like concat('%', #{uuid}, '%')  and `status` >= 0 limit 1")
    SysUserEntity getByUuidLike(@Param("uuid") String uuid);

    @Select("SELECT * FROM sys_user WHERE `qq` = #{qq} and `status` >= 0")
    SysUserEntity getByQq(@Param("qq") String qq);

    @Select("SELECT * FROM sys_user WHERE `qq` like concat('%', #{qq}, '%') and `status` >= 0 limit 1")
    SysUserEntity getByQqLike(@Param("qq") String qq);

    @Select("SELECT * FROM sys_user WHERE `weixin` = #{weixin} and `status` >= 0")
    SysUserEntity getByWeixing(@Param("weixin") String weixin);

    @Select("SELECT * FROM sys_user WHERE `weixin` like concat('%', #{weixin}, '%') and `status` >= 0 limit 1")
    SysUserEntity getByWeixingLike(@Param("weixin") String weixin);

    @Select("SELECT * FROM sys_user WHERE `alipay` = #{alipay} and `status` >= 0")
    SysUserEntity getByAlipay(@Param("alipay") String alipay);

    @Select("SELECT * FROM sys_user WHERE `alipay` like concat('%', #{alipay}, '%') and `status` >= 0 limit 1")
    SysUserEntity getByAlipayLike(@Param("alipay") String alipay);

    @Select("SELECT * FROM sys_user WHERE `id_card` = #{idCard} and `status` >= 0")
    SysUserEntity getByIdCard(@Param("idCard") String idCard);

    @Select("SELECT * FROM sys_user WHERE `id_card` like concat('%', #{idCard}, '%') and `status` >= 0 limit 1")
    SysUserEntity getByIdCardLike(@Param("idCard") String idCard);
}
