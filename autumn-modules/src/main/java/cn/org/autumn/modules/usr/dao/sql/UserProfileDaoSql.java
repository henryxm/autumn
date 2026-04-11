package cn.org.autumn.modules.usr.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;
import cn.org.autumn.database.runtime.RuntimeSqlDialect;

/**
 * {@link cn.org.autumn.modules.usr.dao.UserProfileDao} 的可移植 SQL。
 * <p>
 * 列名统一 {@link #quote(String)}；时间戳 {@link #currentTimestamp()}；
 * 单行 {@link #limitOne()}，避免保留字与各库标识符规则差异。
 */
public class UserProfileDaoSql extends RuntimeSql {

    private String tbl() {
        return quote("usr_user_profile");
    }

    public String getByUuid() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("uuid") + " = #{uuid}" + limitOne();
    }

    public String getByOpenId() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("open_id") + " = #{openId}" + limitOne();
    }

    public String getByUnionId() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("union_id") + " = #{unionId}" + limitOne();
    }

    public String getByUsername() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("username") + " = #{username}" + limitOne();
    }

    public String setUuid() {
        return "UPDATE " + tbl() + " SET " + quote("uuid") + " = #{uuid} WHERE " + quote("username") + " = #{username}";
    }

    public String touchLogin() {
        return "UPDATE " + tbl() + " SET " + quote("login_ip") + " = #{ip}, " + quote("visit_ip") + " = #{ip}, "
                + quote("user_agent") + " = #{userAgent}, " + quote("login_time") + " = " + currentTimestamp()
                + ", " + quote("visit_time") + " = " + currentTimestamp() + " WHERE " + quote("uuid") + " = #{uuid}";
    }

    public String touchVisit() {
        return "UPDATE " + tbl() + " SET " + quote("visit_ip") + " = #{ip}, " + quote("user_agent") + " = #{userAgent}, "
                + quote("visit_time") + " = " + currentTimestamp() + " WHERE " + quote("uuid") + " = #{uuid}";
    }
}
