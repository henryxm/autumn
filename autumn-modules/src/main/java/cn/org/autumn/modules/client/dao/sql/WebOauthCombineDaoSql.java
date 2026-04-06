package cn.org.autumn.modules.client.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;

/**
 * {@link cn.org.autumn.modules.client.dao.WebOauthCombineDao} 可移植 SQL。
 */
public class WebOauthCombineDaoSql extends RuntimeSql {

    public String getByClientId() {
        return "SELECT * FROM " + quote("client_web_oauth_combine") + " WHERE " + quote("client_id") + " = #{clientId}" + limitOne();
    }

    public String count() {
        return "SELECT COUNT(*) FROM " + quote("client_web_oauth_combine") + " WHERE " + quote("client_id") + " = #{clientId}";
    }
}
