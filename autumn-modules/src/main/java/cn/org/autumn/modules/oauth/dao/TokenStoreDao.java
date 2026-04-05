package cn.org.autumn.modules.oauth.dao;

import cn.org.autumn.modules.oauth.entity.TokenStoreEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import cn.org.autumn.modules.oauth.dao.sql.TokenStoreDaoSql;
import org.apache.ibatis.annotations.SelectProvider;
import org.springframework.stereotype.Repository;

/**
 * 授权令牌
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
@Mapper
@Repository
public interface TokenStoreDao extends BaseMapper<TokenStoreEntity> {

    @SelectProvider(type = TokenStoreDaoSql.class, method = "findByAuthCode")
    TokenStoreEntity findByAuthCode(@Param("authCode") String authCode);

    @SelectProvider(type = TokenStoreDaoSql.class, method = "findByUserUuid")
    TokenStoreEntity findByUserUuid(@Param("userUuid") String userUuid);

    @SelectProvider(type = TokenStoreDaoSql.class, method = "findByAccessToken")
    TokenStoreEntity findByAccessToken(@Param("accessToken") String accessToken);

    @SelectProvider(type = TokenStoreDaoSql.class, method = "findByRefreshToken")
    TokenStoreEntity findByRefreshToken(@Param("refreshToken") String refreshToken);
}
