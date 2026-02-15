package cn.org.autumn.modules.usr.dao;

import cn.org.autumn.modules.usr.entity.UserOpenEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface UserOpenDao extends BaseMapper<UserOpenEntity> {

    /**
     * 根据openid、platform和appid精确查询（排除已删除）
     */
    @Select("SELECT * FROM usr_user_open WHERE openid = #{openid} AND platform = #{platform} AND appid = #{appid} AND deleted = 0 LIMIT 1")
    UserOpenEntity getByOpenidAndPlatformAndAppid(@Param("openid") String openid, @Param("platform") String platform, @Param("appid") String appid);

    /**
     * 根据openid和platform查询（排除已删除）
     */
    @Select("SELECT * FROM usr_user_open WHERE openid = #{openid} AND platform = #{platform} AND deleted = 0")
    List<UserOpenEntity> getByOpenidAndPlatform(@Param("openid") String openid, @Param("platform") String platform);

    /**
     * 根据unionid和platform查询（排除已删除）
     */
    @Select("SELECT * FROM usr_user_open WHERE unionid = #{unionid} AND platform = #{platform} AND deleted = 0")
    List<UserOpenEntity> getByUnionidAndPlatform(@Param("unionid") String unionid, @Param("platform") String platform);

    /**
     * 根据unionid、platform和appid查询（排除已删除）
     */
    @Select("SELECT * FROM usr_user_open WHERE unionid = #{unionid} AND platform = #{platform} AND appid = #{appid} AND deleted = 0 LIMIT 1")
    UserOpenEntity getByUnionidAndPlatformAndAppid(@Param("unionid") String unionid, @Param("platform") String platform, @Param("appid") String appid);

    /**
     * 根据uuid查询用户的所有平台绑定关系（排除已删除）
     */
    @Select("SELECT * FROM usr_user_open WHERE uuid = #{uuid} AND deleted = 0")
    List<UserOpenEntity> getByUuid(@Param("uuid") String uuid);

    /**
     * 根据uuid和platform查询（排除已删除）
     */
    @Select("SELECT * FROM usr_user_open WHERE uuid = #{uuid} AND platform = #{platform} AND deleted = 0")
    List<UserOpenEntity> getByUuidAndPlatform(@Param("uuid") String uuid, @Param("platform") String platform);

    /**
     * 根据openid查询（跨平台查询，排除已删除）
     */
    @Select("SELECT * FROM usr_user_open WHERE openid = #{openid} AND deleted = 0")
    List<UserOpenEntity> getByOpenid(@Param("openid") String openid);

    /**
     * 根据unionid查询（跨平台查询，排除已删除）
     */
    @Select("SELECT * FROM usr_user_open WHERE unionid = #{unionid} AND deleted = 0")
    List<UserOpenEntity> getByUnionid(@Param("unionid") String unionid);

    /**
     * 根据uuid查询所有绑定关系（包括已删除的，用于物理删除）
     */
    @Select("SELECT * FROM usr_user_open WHERE uuid = #{uuid}")
    List<UserOpenEntity> getAllByUuid(@Param("uuid") String uuid);

    /**
     * 根据openid、platform和appid删除绑定关系
     */
    @Delete("DELETE FROM usr_user_open WHERE openid = #{openid} AND platform = #{platform} AND appid = #{appid}")
    int deleteByOpenidAndPlatformAndAppid(@Param("openid") String openid, @Param("platform") String platform, @Param("appid") String appid);

    /**
     * 根据uuid删除用户的所有绑定关系
     */
    @Delete("DELETE FROM usr_user_open WHERE uuid = #{uuid}")
    int deleteByUuid(@Param("uuid") String uuid);

    /**
     * 根据uuid和platform删除用户在某个平台的所有绑定关系
     */
    @Delete("DELETE FROM usr_user_open WHERE uuid = #{uuid} AND platform = #{platform}")
    int deleteByUuidAndPlatform(@Param("uuid") String uuid, @Param("platform") String platform);

    /**
     * 根据unionid和platform删除同一平台下的所有绑定关系
     */
    @Delete("DELETE FROM usr_user_open WHERE unionid = #{unionid} AND platform = #{platform}")
    int deleteByUnionidAndPlatform(@Param("unionid") String unionid, @Param("platform") String platform);
}
