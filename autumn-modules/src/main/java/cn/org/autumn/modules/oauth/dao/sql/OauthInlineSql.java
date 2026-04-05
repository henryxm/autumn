package cn.org.autumn.modules.oauth.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSqlDialect;
import cn.org.autumn.database.runtime.RuntimeSqlDialectRegistry;

/**
 * OAuth 模块中带保留字列名的可移植 SQL。
 * <p>
 * 列名经 {@link RuntimeSqlDialect#quote(String)}；单行限制 {@link RuntimeSqlDialect#limitOne()}。
 */
public class OauthInlineSql {

    private RuntimeSqlDialect d() {
        return RuntimeSqlDialectRegistry.get();
    }

    public String securityRequestLatest() {
        return "select * from oauth_security_request where " + d().quote("enabled") + " = 1 order by " + d().quote("create") + " desc" + d().limitOne();
    }

    public String securityRequestDeleteBefore() {
        return "delete from oauth_security_request where " + d().quote("create") + " < #{deadline}";
    }

    public String encryptKeyBySession() {
        return "select * from oauth_encrypt_key where " + d().quote("session") + " = #{session}";
    }

    public String securityRequestByAuth() {
        return "select * from oauth_security_request where " + d().quote("enabled") + " = 1 and " + d().quote("auth") + " = #{auth}" + d().limitOne();
    }

    public String deleteExpiredKeys() {
        return "DELETE FROM " + d().quote("oauth_encrypt_key") + " WHERE " + d().quote("expire") + " IS NOT NULL AND " + d().quote("expire")
                + " < #{cleanBeforeTime}";
    }
}
