package cn.org.autumn.modules.oauth.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSqlDialect;
import cn.org.autumn.database.runtime.RuntimeSqlDialectRegistry;

/**
 * OAuth 模块中带保留字列名的可移植 SQL。
 */
public class OauthInlineSql {

    private RuntimeSqlDialect d() {
        return RuntimeSqlDialectRegistry.get();
    }

    public String securityRequestLatest() {
        return "select * from oauth_security_request where enabled = 1 order by " + d().quote("create") + " desc" + d().limitOne();
    }

    public String securityRequestDeleteBefore() {
        return "delete from oauth_security_request where " + d().quote("create") + " < #{deadline}";
    }

    public String encryptKeyBySession() {
        return "select * from oauth_encrypt_key where " + d().quote("session") + " = #{session}";
    }

    public String securityRequestByAuth() {
        return "select * from oauth_security_request where enabled = 1 and auth = #{auth}" + d().limitOne();
    }
}
