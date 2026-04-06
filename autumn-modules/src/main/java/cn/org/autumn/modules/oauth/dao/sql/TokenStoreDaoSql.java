package cn.org.autumn.modules.oauth.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;

/**
 * {@link cn.org.autumn.modules.oauth.dao.TokenStoreDao} 可移植 SQL。
 */
public class TokenStoreDaoSql extends RuntimeSql {

    public String findByAuthCode() {
        return "select * from oauth_token_store where auth_code = #{authCode}" + limitOne();
    }

    public String findByUserUuid() {
        return "select * from oauth_token_store where user_uuid = #{userUuid}" + limitOne();
    }

    public String findByAccessToken() {
        return "select * from oauth_token_store where access_token = #{accessToken}" + limitOne();
    }

    public String findByRefreshToken() {
        return "select * from oauth_token_store where refresh_token = #{refreshToken}" + limitOne();
    }
}
