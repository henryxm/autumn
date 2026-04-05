package cn.org.autumn.modules.usr.dao;

import cn.org.autumn.modules.usr.dao.sql.UserOpenDaoSql;
import cn.org.autumn.modules.usr.entity.UserOpenEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface UserOpenDao extends BaseMapper<UserOpenEntity> {

    /**
     * 根据openid、platform和appid精确查询（排除已删除）
     */
    @SelectProvider(type = UserOpenDaoSql.class, method = "getByOpenidAndPlatformAndAppid")
    UserOpenEntity getByOpenidAndPlatformAndAppid(@Param("openid") String openid, @Param("platform") String platform, @Param("appid") String appid);

    /**
     * 根据openid和platform查询（排除已删除）
     */
    @SelectProvider(type = UserOpenDaoSql.class, method = "getByOpenidAndPlatform")
    List<UserOpenEntity> getByOpenidAndPlatform(@Param("openid") String openid, @Param("platform") String platform);

    /**
     * 根据unionid和platform查询（排除已删除）
     */
    @SelectProvider(type = UserOpenDaoSql.class, method = "getByUnionidAndPlatform")
    List<UserOpenEntity> getByUnionidAndPlatform(@Param("unionid") String unionid, @Param("platform") String platform);

    /**
     * 根据unionid、platform和appid查询（排除已删除）
     */
    @SelectProvider(type = UserOpenDaoSql.class, method = "getByUnionidAndPlatformAndAppid")
    UserOpenEntity getByUnionidAndPlatformAndAppid(@Param("unionid") String unionid, @Param("platform") String platform, @Param("appid") String appid);

    /**
     * 根据uuid查询用户的所有平台绑定关系（排除已删除）
     */
    @SelectProvider(type = UserOpenDaoSql.class, method = "getByUuid")
    List<UserOpenEntity> getByUuid(@Param("uuid") String uuid);

    /**
     * 根据uuid和platform查询（排除已删除）
     */
    @SelectProvider(type = UserOpenDaoSql.class, method = "getByUuidAndPlatform")
    List<UserOpenEntity> getByUuidAndPlatform(@Param("uuid") String uuid, @Param("platform") String platform);

    /**
     * 根据openid查询（跨平台查询，排除已删除）
     */
    @SelectProvider(type = UserOpenDaoSql.class, method = "getByOpenid")
    List<UserOpenEntity> getByOpenid(@Param("openid") String openid);

    /**
     * 根据unionid查询（跨平台查询，排除已删除）
     */
    @SelectProvider(type = UserOpenDaoSql.class, method = "getByUnionid")
    List<UserOpenEntity> getByUnionid(@Param("unionid") String unionid);

    /**
     * 根据uuid查询所有绑定关系（包括已删除的，用于物理删除）
     */
    @SelectProvider(type = UserOpenDaoSql.class, method = "getAllByUuid")
    List<UserOpenEntity> getAllByUuid(@Param("uuid") String uuid);

    /**
     * 根据openid、platform和appid删除绑定关系
     */
    @DeleteProvider(type = UserOpenDaoSql.class, method = "deleteByOpenidAndPlatformAndAppid")
    int deleteByOpenidAndPlatformAndAppid(@Param("openid") String openid, @Param("platform") String platform, @Param("appid") String appid);

    /**
     * 根据uuid删除用户的所有绑定关系
     */
    @DeleteProvider(type = UserOpenDaoSql.class, method = "deleteByUuid")
    int deleteByUuid(@Param("uuid") String uuid);

    /**
     * 根据uuid和platform删除用户在某个平台的所有绑定关系
     */
    @DeleteProvider(type = UserOpenDaoSql.class, method = "deleteByUuidAndPlatform")
    int deleteByUuidAndPlatform(@Param("uuid") String uuid, @Param("platform") String platform);

    /**
     * 根据unionid和platform删除同一平台下的所有绑定关系
     */
    @DeleteProvider(type = UserOpenDaoSql.class, method = "deleteByUnionidAndPlatform")
    int deleteByUnionidAndPlatform(@Param("unionid") String unionid, @Param("platform") String platform);
}
