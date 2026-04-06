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

    public String getByUuid() {
        return "select * from usr_user_profile where " + quote("uuid") + " = #{uuid}" + limitOne();
    }

    public String getByOpenId() {
        return "select * from usr_user_profile where " + quote("open_id") + " = #{openId}" + limitOne();
    }

    public String getByUnionId() {
        return "select * from usr_user_profile where " + quote("union_id") + " = #{unionId}" + limitOne();
    }

    public String getByUsername() {
        return "select * from usr_user_profile where " + quote("username") + " = #{username}" + limitOne();
    }

    public String setUuid() {
        return "update usr_user_profile set " + quote("uuid") + " = #{uuid} where " + quote("username") + " = #{username}";
    }

    public String touchLogin() {
        return "update usr_user_profile set " + quote("login_ip") + " = #{ip}, " + quote("visit_ip") + " = #{ip}, "
                + quote("user_agent") + " = #{userAgent}, " + quote("login_time") + " = " + currentTimestamp()
                + ", " + quote("visit_time") + " = " + currentTimestamp() + " where " + quote("uuid") + " = #{uuid}";
    }

    public String touchVisit() {
        return "update usr_user_profile set " + quote("visit_ip") + " = #{ip}, " + quote("user_agent") + " = #{userAgent}, "
                + quote("visit_time") + " = " + currentTimestamp() + " where " + quote("uuid") + " = #{uuid}";
    }
}
