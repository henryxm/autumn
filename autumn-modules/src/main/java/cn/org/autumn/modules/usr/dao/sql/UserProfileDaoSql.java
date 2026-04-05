package cn.org.autumn.modules.usr.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSqlDialect;
import cn.org.autumn.database.runtime.RuntimeSqlDialectRegistry;

/**
 * {@link cn.org.autumn.modules.usr.dao.UserProfileDao} 的可移植 SQL。
 */
public class UserProfileDaoSql {

    private RuntimeSqlDialect d() {
        return RuntimeSqlDialectRegistry.get();
    }

    public String getByUuid() {
        return "select * from usr_user_profile where uuid = #{uuid}" + d().limitOne();
    }

    public String getByOpenId() {
        return "select * from usr_user_profile where open_id = #{openId}" + d().limitOne();
    }

    public String getByUnionId() {
        return "select * from usr_user_profile where union_id = #{unionId}" + d().limitOne();
    }

    public String getByUsername() {
        return "select * from usr_user_profile where username = #{username}" + d().limitOne();
    }

    public String setUuid() {
        return "update usr_user_profile set uuid = #{uuid} where username = #{username}";
    }

    public String touchLogin() {
        return "update usr_user_profile set login_ip = #{ip}, visit_ip = #{ip}, user_agent = #{userAgent}, login_time = " + d().currentTimestamp()
                + ", visit_time = " + d().currentTimestamp() + " where uuid = #{uuid}";
    }

    public String touchVisit() {
        return "update usr_user_profile set visit_ip = #{ip}, user_agent = #{userAgent}, visit_time = " + d().currentTimestamp() + " where uuid = #{uuid}";
    }
}
