package cn.org.autumn.modules.usr.dao;

import cn.org.autumn.modules.usr.entity.UserProfileEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

/**
 * 用户信息
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
@Mapper
@Repository
public interface UserProfileDao extends BaseMapper<UserProfileEntity> {

    @Select("select * from usr_user_profile where uuid = #{uuid} limit 1")
    UserProfileEntity getByUuid(@Param("uuid") String uuid);

    @Select("select * from usr_user_profile where open_id = #{openId} limit 1")
    UserProfileEntity getByOpenId(@Param("openId") String openId);

    @Select("select * from usr_user_profile where union_id = #{unionId} limit 1")
    UserProfileEntity getByUnionId(@Param("unionId") String unionId);

    @Select("select * from usr_user_profile where username = #{username} limit 1")
    UserProfileEntity getByUsername(@Param("username") String username);

    @Update("update usr_user_profile set uuid = #{uuid} where username = #{username}")
    Integer setUuid(@Param("username") String username, @Param("uuid") String uuid);

    @Update("update usr_user_profile set login_ip = #{ip}, visit_ip = #{ip}, login_time = now(), visit_time = now() where uuid = #{uuid}")
    Integer updateLoginIp(@Param("uuid") String uuid, @Param("ip") String ip);

    @Update("update usr_user_profile set visit_ip = #{ip}, visit_time = now() where uuid = #{uuid}")
    Integer updateVisitIp(@Param("uuid") String uuid, @Param("ip") String ip);
}
