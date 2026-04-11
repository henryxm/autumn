package cn.org.autumn.modules.usr.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;
import cn.org.autumn.database.runtime.RuntimeSqlDialect;

/**
 * {@link cn.org.autumn.modules.usr.dao.UserTokenDao} 可移植 SQL。
 * <p>
 * 表/列经 {@link RuntimeSqlDialect#quote(String)}；单行 {@link #limitOne()}。
 */
public class UserTokenDaoSql extends RuntimeSql {

    private String tbl() {
        return quote("usr_user_token");
    }

    public String getToken() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("token") + " = #{token}";
    }

    public String getUuid() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("uuid") + " = #{uuid}" + limitOne();
    }

    public String getUser() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("user_uuid") + " = #{userUuid}";
    }

    public String deleteUser() {
        return "DELETE FROM " + tbl() + " WHERE " + quote("user_uuid") + " = #{userUuid}";
    }

    public String deleteUuid() {
        return "DELETE FROM " + tbl() + " WHERE " + quote("uuid") + " = #{uuid}";
    }
}
