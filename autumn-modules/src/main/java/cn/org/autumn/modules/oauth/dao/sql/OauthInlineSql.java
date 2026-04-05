package cn.org.autumn.modules.oauth.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSqlDialect;
import cn.org.autumn.database.runtime.RuntimeSqlDialectRegistry;

/**
 * OAuth 模块中带保留字列名的可移植 SQL。
 * <p>
 * 列名经 {@link RuntimeSqlDialect#quote(String)}；单行限制 {@link RuntimeSqlDialect#limitOne()}。
 */
public class OauthInlineSql {

    /** MyBatis 占位符 #{auth}；拆写避免部分解析器误判 */
    private static final String MB_AUTH = "#" + "{" + "auth" + "}";

    private RuntimeSqlDialect d() {
        return RuntimeSqlDialectRegistry.get();
    }

    public String securityRequestLatest() {
        return "select * from oauth_security_request where " + d().quote("enabled") + " = " + d().enabledTrueSqlLiteral()
                + " order by " + d().quote("create") + " desc" + d().limitOne();
    }

    public String securityRequestDeleteBefore() {
        return "delete from oauth_security_request where " + d().quote("create") + " < #{deadline}";
    }

    public String encryptKeyBySession() {
        return "select * from oauth_encrypt_key where " + d().quote("session") + " = #{session}";
    }

    public String securityRequestByAuth() {
        String on = d().enabledTrueSqlLiteral();
        return "select * from oauth_security_request where " + d().quote("enabled") + " = " + on
                + " and " + d().quote("auth") + " = " + MB_AUTH + d().limitOne();
    }

    public String deleteExpiredKeys() {
        return "DELETE FROM " + d().quote("oauth_encrypt_key") + " WHERE " + d().quote("expire") + " IS NOT NULL AND " + d().quote("expire")
                + " < #{cleanBeforeTime}";
    }
}
