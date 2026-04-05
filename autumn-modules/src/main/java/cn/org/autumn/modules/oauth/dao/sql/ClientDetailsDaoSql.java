package cn.org.autumn.modules.oauth.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSqlDialect;
import cn.org.autumn.database.runtime.RuntimeSqlDialectRegistry;

/**
 * {@link cn.org.autumn.modules.oauth.dao.ClientDetailsDao} 可移植 SQL。
 */
public class ClientDetailsDaoSql {

    private RuntimeSqlDialect d() {
        return RuntimeSqlDialectRegistry.get();
    }

    public String findByClientId() {
        return "SELECT * FROM " + d().quote("oauth_client_details") + " WHERE " + d().quote("client_id") + " = #{clientId}" + d().limitOne();
    }

    public String getByUuid() {
        return "SELECT * FROM " + d().quote("oauth_client_details") + " WHERE " + d().quote("uuid") + " = #{uuid}" + d().limitOne();
    }

    public String findByClientSecret() {
        return "SELECT * FROM " + d().quote("oauth_client_details") + " WHERE " + d().quote("client_secret") + " = #{clientSecret}" + d().limitOne();
    }

    public String count() {
        return "SELECT COUNT(*) FROM " + d().quote("oauth_client_details") + " WHERE " + d().quote("client_id") + " = #{clientId}";
    }

    public String countClientType() {
        return "SELECT COUNT(*) FROM " + d().quote("oauth_client_details") + " WHERE " + d().quote("client_type") + " = #{clientType}";
    }

    public String updateClientType() {
        return "UPDATE " + d().quote("oauth_client_details") + " SET " + d().quote("client_type") + " = #{clientType} WHERE " + d().quote("client_id")
                + " = #{clientId}";
    }
}
