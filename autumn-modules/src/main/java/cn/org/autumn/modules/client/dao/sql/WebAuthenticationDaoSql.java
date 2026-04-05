package cn.org.autumn.modules.client.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSqlDialect;
import cn.org.autumn.database.runtime.RuntimeSqlDialectRegistry;

/**
 * {@link cn.org.autumn.modules.client.dao.WebAuthenticationDao} 可移植 SQL。
 */
public class WebAuthenticationDaoSql {

    private RuntimeSqlDialect d() {
        return RuntimeSqlDialectRegistry.get();
    }

    public String getByClientId() {
        return "SELECT * FROM " + d().quote("client_web_authentication") + " WHERE " + d().quote("client_id") + " = #{clientId}" + d().limitOne();
    }

    public String getByUuid() {
        return "SELECT * FROM " + d().quote("client_web_authentication") + " WHERE " + d().quote("uuid") + " = #{uuid}" + d().limitOne();
    }

    public String count() {
        return "SELECT COUNT(*) FROM " + d().quote("client_web_authentication") + " WHERE " + d().quote("client_id") + " = #{clientId}";
    }

    public String countClientType() {
        return "SELECT COUNT(*) FROM " + d().quote("client_web_authentication") + " WHERE " + d().quote("client_type") + " = #{clientType}";
    }

    public String hasClientId() {
        return "SELECT COUNT(*) FROM " + d().quote("client_web_authentication") + " WHERE " + d().quote("client_id") + " = #{clientId}";
    }

    public String updateClientType() {
        return "UPDATE " + d().quote("client_web_authentication") + " SET " + d().quote("client_type") + " = #{clientType} WHERE " + d().quote("client_id")
                + " = #{clientId}";
    }
}
