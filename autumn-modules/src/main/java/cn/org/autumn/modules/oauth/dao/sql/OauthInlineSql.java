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

    private String tblSecurityRequest() {
        return quote("oauth_security_request");
    }

    private String tblEncryptKey() {
        return quote("oauth_encrypt_key");
    }

    public String securityRequestLatest() {
        return "SELECT * FROM " + tblSecurityRequest() + " WHERE " + quote("enabled") + " = " + enabledTrueSqlLiteral()
                + " ORDER BY " + quote("create") + " DESC" + limitOne();
    }

    public String securityRequestDeleteBefore() {
        return "DELETE FROM " + tblSecurityRequest() + " WHERE " + quote("create") + " < #{deadline}";
    }

    public String encryptKeyBySession() {
        return "SELECT * FROM " + tblEncryptKey() + " WHERE " + quote("session") + " = #{session}";
    }

    public String securityRequestByAuth() {
        String on = enabledTrueSqlLiteral();
        return "SELECT * FROM " + tblSecurityRequest() + " WHERE " + quote("enabled") + " = " + on
                + " AND " + quote("auth") + " = " + MB_AUTH + limitOne();
    }

    public String deleteExpiredKeys() {
        return "DELETE FROM " + tblEncryptKey() + " WHERE " + quote("expire") + " IS NOT NULL AND " + quote("expire")
                + " < #{cleanBeforeTime}";
    }
}
