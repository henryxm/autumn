package cn.org.autumn.modules.client.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;

/**
 * {@link cn.org.autumn.modules.client.dao.WebOauthBindDao} 可移植 SQL。
 */
public class WebOauthBindDaoSql extends RuntimeSql {

    public String getByAuthenticationAndUpper() {
        return "SELECT * FROM " + quote("client_web_oauth_bind") + " WHERE " + quote("authentication") + " = #{authentication} AND " + quote("upper") + " = #{upper}" + limitOne();
    }

    public String getByAuthenticationAndUser() {
        return "SELECT * FROM " + quote("client_web_oauth_bind") + " WHERE " + quote("authentication") + " = #{authentication} AND " + quote("user") + " = #{user}" + limitOne();
    }
}
