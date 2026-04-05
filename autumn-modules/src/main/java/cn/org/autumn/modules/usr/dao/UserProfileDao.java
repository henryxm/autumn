package cn.org.autumn.modules.usr.dao;

import cn.org.autumn.modules.usr.dao.sql.UserProfileDaoSql;
import cn.org.autumn.modules.usr.entity.UserProfileEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;
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

    @SelectProvider(type = UserProfileDaoSql.class, method = "getByUuid")
    UserProfileEntity getByUuid(@Param("uuid") String uuid);

    @SelectProvider(type = UserProfileDaoSql.class, method = "getByOpenId")
    UserProfileEntity getByOpenId(@Param("openId") String openId);

    @SelectProvider(type = UserProfileDaoSql.class, method = "getByUnionId")
    UserProfileEntity getByUnionId(@Param("unionId") String unionId);

    @SelectProvider(type = UserProfileDaoSql.class, method = "getByUsername")
    UserProfileEntity getByUsername(@Param("username") String username);

    @UpdateProvider(type = UserProfileDaoSql.class, method = "setUuid")
    Integer setUuid(@Param("username") String username, @Param("uuid") String uuid);

    @UpdateProvider(type = UserProfileDaoSql.class, method = "touchLogin")
    Integer updateLoginIp(@Param("uuid") String uuid, @Param("ip") String ip, @Param("userAgent") String userAgent);

    @UpdateProvider(type = UserProfileDaoSql.class, method = "touchVisit")
    Integer updateVisitIp(@Param("uuid") String uuid, @Param("ip") String ip, @Param("userAgent") String userAgent);
}
