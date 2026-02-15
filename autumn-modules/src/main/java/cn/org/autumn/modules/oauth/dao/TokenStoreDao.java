package cn.org.autumn.modules.oauth.dao;

import cn.org.autumn.modules.oauth.entity.TokenStoreEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
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

    @Select("select * from oauth_token_store where auth_code = #{authCode} limit 1")
    TokenStoreEntity findByAuthCode(@Param("authCode") String authCode);

    @Select("select * from oauth_token_store where user_uuid = #{userUuid} limit 1")
    TokenStoreEntity findByUserUuid(@Param("userUuid") String userUuid);

    @Select("select * from oauth_token_store where access_token = #{accessToken} limit 1")
    TokenStoreEntity findByAccessToken(@Param("accessToken") String accessToken);

    @Select("select * from oauth_token_store where refresh_token = #{refreshToken} limit 1")
    TokenStoreEntity findByRefreshToken(@Param("refreshToken") String refreshToken);
}
