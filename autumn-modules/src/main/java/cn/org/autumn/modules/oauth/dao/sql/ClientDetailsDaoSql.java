package cn.org.autumn.modules.oauth.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;

/**
 * {@link cn.org.autumn.modules.oauth.dao.ClientDetailsDao} 可移植 SQL。
 */
public class ClientDetailsDaoSql extends RuntimeSql {

    public String findByClientId() {
        return "SELECT * FROM " + quote("oauth_client_details") + " WHERE " + quote("client_id") + " = #{clientId}" + limitOne();
    }

    public String getByUuid() {
        return "SELECT * FROM " + quote("oauth_client_details") + " WHERE " + quote("uuid") + " = #{uuid}" + limitOne();
    }

    public String findByClientSecret() {
        return "SELECT * FROM " + quote("oauth_client_details") + " WHERE " + quote("client_secret") + " = #{clientSecret}" + limitOne();
    }

    public String count() {
        return "SELECT COUNT(*) FROM " + quote("oauth_client_details") + " WHERE " + quote("client_id") + " = #{clientId}";
    }

    public String countClientType() {
        return "SELECT COUNT(*) FROM " + quote("oauth_client_details") + " WHERE " + quote("client_type") + " = #{clientType}";
    }

    public String updateClientType() {
        return "UPDATE " + quote("oauth_client_details") + " SET " + quote("client_type") + " = #{clientType} WHERE " + quote("client_id")
                + " = #{clientId}";
    }
}
