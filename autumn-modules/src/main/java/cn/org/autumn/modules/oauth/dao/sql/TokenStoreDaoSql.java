package cn.org.autumn.modules.oauth.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;

/**
 * {@link cn.org.autumn.modules.oauth.dao.TokenStoreDao} 可移植 SQL。
 */
public class TokenStoreDaoSql extends RuntimeSql {

    private String tbl() {
        return quote("oauth_token_store");
    }

    public String findByAuthCode() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("auth_code") + " = #{authCode}" + limitOne();
    }

    public String findByUserUuid() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("user_uuid") + " = #{userUuid}" + limitOne();
    }

    public String findByAccessToken() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("access_token") + " = #{accessToken}" + limitOne();
    }

    public String findByRefreshToken() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("refresh_token") + " = #{refreshToken}" + limitOne();
    }
}
