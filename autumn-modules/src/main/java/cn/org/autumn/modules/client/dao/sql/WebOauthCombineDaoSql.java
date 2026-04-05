package cn.org.autumn.modules.client.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSqlDialect;
import cn.org.autumn.database.runtime.RuntimeSqlDialectRegistry;

/**
 * {@link cn.org.autumn.modules.client.dao.WebOauthCombineDao} 可移植 SQL。
 */
public class WebOauthCombineDaoSql {

    private RuntimeSqlDialect d() {
        return RuntimeSqlDialectRegistry.get();
    }

    public String getByClientId() {
        return "SELECT * FROM " + d().quote("client_web_oauth_combine") + " WHERE " + d().quote("client_id") + " = #{clientId}" + d().limitOne();
    }

    public String count() {
        return "SELECT COUNT(*) FROM " + d().quote("client_web_oauth_combine") + " WHERE " + d().quote("client_id") + " = #{clientId}";
    }
}
