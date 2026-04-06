package cn.org.autumn.modules.client.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;

/**
 * {@link cn.org.autumn.modules.client.dao.WebAuthenticationDao} 可移植 SQL。
 */
public class WebAuthenticationDaoSql extends RuntimeSql {

    public String getByClientId() {
        return "SELECT * FROM " + quote("client_web_authentication") + " WHERE " + quote("client_id") + " = #{clientId}" + limitOne();
    }

    public String getByUuid() {
        return "SELECT * FROM " + quote("client_web_authentication") + " WHERE " + quote("uuid") + " = #{uuid}" + limitOne();
    }

    public String count() {
        return "SELECT COUNT(*) FROM " + quote("client_web_authentication") + " WHERE " + quote("client_id") + " = #{clientId}";
    }

    public String countClientType() {
        return "SELECT COUNT(*) FROM " + quote("client_web_authentication") + " WHERE " + quote("client_type") + " = #{clientType}";
    }

    public String hasClientId() {
        return "SELECT COUNT(*) FROM " + quote("client_web_authentication") + " WHERE " + quote("client_id") + " = #{clientId}";
    }

    public String updateClientType() {
        return "UPDATE " + quote("client_web_authentication") + " SET " + quote("client_type") + " = #{clientType} WHERE " + quote("client_id")
                + " = #{clientId}";
    }
}
