package cn.org.autumn.modules.usr.dao;

import cn.org.autumn.modules.usr.entity.UserTokenEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户Token
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
@Mapper
@Repository
public interface UserTokenDao extends BaseMapper<UserTokenEntity> {

    @Select("select * from usr_user_token where `token` = #{token}")
    UserTokenEntity getToken(@Param("token") String token);

    @Select("select * from usr_user_token where `uuid` = #{uuid} LIMIT 1")
    UserTokenEntity getUuid(@Param("uuid") String uuid);

    @Select("select * from usr_user_token where `user_uuid` = #{userUuid}")
    List<UserTokenEntity> getUser(@Param("userUuid") String userUuid);

    @Delete("delete from usr_user_token where `user_uuid` = #{userUuid}")
    void deleteUser(@Param("userUuid") String userUuid);

    @Delete("delete from usr_user_token where `uuid` = #{uuid}")
    void deleteUuid(@Param("uuid") String uuid);
}
