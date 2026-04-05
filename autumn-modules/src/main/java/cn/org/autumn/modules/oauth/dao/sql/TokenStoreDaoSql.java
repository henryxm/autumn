package cn.org.autumn.modules.oauth.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSqlDialect;
import cn.org.autumn.database.runtime.RuntimeSqlDialectRegistry;

/**
 * {@link cn.org.autumn.modules.oauth.dao.TokenStoreDao} 可移植 SQL。
 */
public class TokenStoreDaoSql {

    private RuntimeSqlDialect d() {
        return RuntimeSqlDialectRegistry.get();
    }

    public String findByAuthCode() {
        return "select * from oauth_token_store where auth_code = #{authCode}" + d().limitOne();
    }

    public String findByUserUuid() {
        return "select * from oauth_token_store where user_uuid = #{userUuid}" + d().limitOne();
    }

    public String findByAccessToken() {
        return "select * from oauth_token_store where access_token = #{accessToken}" + d().limitOne();
    }

    public String findByRefreshToken() {
        return "select * from oauth_token_store where refresh_token = #{refreshToken}" + d().limitOne();
    }
}
