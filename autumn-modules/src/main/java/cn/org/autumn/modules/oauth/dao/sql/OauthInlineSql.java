package cn.org.autumn.modules.oauth.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;
import cn.org.autumn.database.runtime.RuntimeSqlDialect;

/**
 * OAuth 模块中带保留字列名的可移植 SQL。
 * <p>
 * 列名经 {@link #quote(String)}；单行限制 {@link #limitOne()}（与各库方言一致，见 {@link RuntimeSqlDialect#limitOne()}）。
 */
public class OauthInlineSql extends RuntimeSql {

    /** MyBatis 占位符 #{auth}；拆写避免部分解析器误判 */
    private static final String MB_AUTH = "#" + "{" + "auth" + "}";

    public String securityRequestLatest() {
        return "select * from oauth_security_request where " + quote("enabled") + " = " + enabledTrueSqlLiteral()
                + " order by " + quote("create") + " desc" + limitOne();
    }

    public String securityRequestDeleteBefore() {
        return "delete from oauth_security_request where " + quote("create") + " < #{deadline}";
    }

    public String encryptKeyBySession() {
        return "select * from oauth_encrypt_key where " + quote("session") + " = #{session}";
    }

    public String securityRequestByAuth() {
        String on = enabledTrueSqlLiteral();
        return "select * from oauth_security_request where " + quote("enabled") + " = " + on
                + " and " + quote("auth") + " = " + MB_AUTH + limitOne();
    }

    public String deleteExpiredKeys() {
        return "DELETE FROM " + quote("oauth_encrypt_key") + " WHERE " + quote("expire") + " IS NOT NULL AND " + quote("expire")
                + " < #{cleanBeforeTime}";
    }
}
