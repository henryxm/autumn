package cn.org.autumn.modules.usr.dao;

import cn.org.autumn.modules.usr.dao.sql.UserTokenDaoSql;
import cn.org.autumn.modules.usr.entity.UserTokenEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
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

    @SelectProvider(type = UserTokenDaoSql.class, method = "getToken")
    UserTokenEntity getToken(@Param("token") String token);

    @SelectProvider(type = UserTokenDaoSql.class, method = "getUuid")
    UserTokenEntity getUuid(@Param("uuid") String uuid);

    @SelectProvider(type = UserTokenDaoSql.class, method = "getUser")
    List<UserTokenEntity> getUser(@Param("userUuid") String userUuid);

    @DeleteProvider(type = UserTokenDaoSql.class, method = "deleteUser")
    void deleteUser(@Param("userUuid") String userUuid);

    @DeleteProvider(type = UserTokenDaoSql.class, method = "deleteUuid")
    void deleteUuid(@Param("uuid") String uuid);
}
