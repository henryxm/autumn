package cn.org.autumn.modules.sys.dao;

import cn.org.autumn.modules.sys.dao.sql.SysUserDaoSql;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface SysUserDao extends BaseMapper<SysUserEntity> {

    @SelectProvider(type = SysUserDaoSql.class, method = "getPermsByUserUuid")
    List<String> getPermsByUserUuid(@Param("userUuid") String userUuid);

    @SelectProvider(type = SysUserDaoSql.class, method = "getMenus")
    List<String> getMenus(@Param("userUuid") String userUuid);

    @UpdateProvider(type = SysUserDaoSql.class, method = "verify")
    void verify(@Param("uuid") String uuid, @Param("verify") int verify);

    @SelectProvider(type = SysUserDaoSql.class, method = "getByUsername")
    SysUserEntity getByUsername(@Param("username") String username);

    @SelectProvider(type = SysUserDaoSql.class, method = "getByUsernameLike")
    SysUserEntity getByUsernameLike(@Param("username") String username);

    @SelectProvider(type = SysUserDaoSql.class, method = "getByEmail")
    SysUserEntity getByEmail(@Param("email") String email);

    @SelectProvider(type = SysUserDaoSql.class, method = "getByEmailLike")
    SysUserEntity getByEmailLike(@Param("email") String email);

    @SelectProvider(type = SysUserDaoSql.class, method = "getByPhone")
    SysUserEntity getByPhone(@Param("mobile") String mobile);

    @SelectProvider(type = SysUserDaoSql.class, method = "getByPhoneLike")
    SysUserEntity getByPhoneLike(@Param("mobile") String mobile);

    @SelectProvider(type = SysUserDaoSql.class, method = "getByUuid")
    SysUserEntity getByUuid(@Param("uuid") String uuid);

    @SelectProvider(type = SysUserDaoSql.class, method = "getForDelete")
    SysUserEntity getForDelete(@Param("uuid") String uuid);

    @SelectProvider(type = SysUserDaoSql.class, method = "getByUuidLike")
    SysUserEntity getByUuidLike(@Param("uuid") String uuid);

    @SelectProvider(type = SysUserDaoSql.class, method = "getByQq")
    SysUserEntity getByQq(@Param("qq") String qq);

    @SelectProvider(type = SysUserDaoSql.class, method = "getByQqLike")
    SysUserEntity getByQqLike(@Param("qq") String qq);

    @SelectProvider(type = SysUserDaoSql.class, method = "getByWeixing")
    SysUserEntity getByWeixing(@Param("weixin") String weixin);

    @SelectProvider(type = SysUserDaoSql.class, method = "getByWeixingLike")
    SysUserEntity getByWeixingLike(@Param("weixin") String weixin);

    @SelectProvider(type = SysUserDaoSql.class, method = "getByAlipay")
    SysUserEntity getByAlipay(@Param("alipay") String alipay);

    @SelectProvider(type = SysUserDaoSql.class, method = "getByAlipayLike")
    SysUserEntity getByAlipayLike(@Param("alipay") String alipay);

    @SelectProvider(type = SysUserDaoSql.class, method = "getByIdCard")
    SysUserEntity getByIdCard(@Param("idCard") String idCard);

    @SelectProvider(type = SysUserDaoSql.class, method = "getByIdCardLike")
    SysUserEntity getByIdCardLike(@Param("idCard") String idCard);
}
