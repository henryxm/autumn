package cn.org.autumn.modules.usr.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSqlDialect;
import cn.org.autumn.database.runtime.RuntimeSqlDialectRegistry;

/**
 * {@link cn.org.autumn.modules.usr.dao.UserProfileDao} 的可移植 SQL。
 * <p>
 * 列名统一 {@link RuntimeSqlDialect#quote(String)}；时间戳 {@link RuntimeSqlDialect#currentTimestamp()}；
 * 单行 {@link RuntimeSqlDialect#limitOne()}，避免保留字与各库标识符规则差异。
 */
public class UserProfileDaoSql {

    private RuntimeSqlDialect d() {
        return RuntimeSqlDialectRegistry.get();
    }

    public String getByUuid() {
        return "select * from usr_user_profile where " + d().quote("uuid") + " = #{uuid}" + d().limitOne();
    }

    public String getByOpenId() {
        return "select * from usr_user_profile where " + d().quote("open_id") + " = #{openId}" + d().limitOne();
    }

    public String getByUnionId() {
        return "select * from usr_user_profile where " + d().quote("union_id") + " = #{unionId}" + d().limitOne();
    }

    public String getByUsername() {
        return "select * from usr_user_profile where " + d().quote("username") + " = #{username}" + d().limitOne();
    }

    public String setUuid() {
        return "update usr_user_profile set " + d().quote("uuid") + " = #{uuid} where " + d().quote("username") + " = #{username}";
    }

    public String touchLogin() {
        return "update usr_user_profile set " + d().quote("login_ip") + " = #{ip}, " + d().quote("visit_ip") + " = #{ip}, "
                + d().quote("user_agent") + " = #{userAgent}, " + d().quote("login_time") + " = " + d().currentTimestamp()
                + ", " + d().quote("visit_time") + " = " + d().currentTimestamp() + " where " + d().quote("uuid") + " = #{uuid}";
    }

    public String touchVisit() {
        return "update usr_user_profile set " + d().quote("visit_ip") + " = #{ip}, " + d().quote("user_agent") + " = #{userAgent}, "
                + d().quote("visit_time") + " = " + d().currentTimestamp() + " where " + d().quote("uuid") + " = #{uuid}";
    }
}
