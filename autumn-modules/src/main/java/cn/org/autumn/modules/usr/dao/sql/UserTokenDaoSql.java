package cn.org.autumn.modules.usr.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;
import cn.org.autumn.database.runtime.RuntimeSqlDialect;

/**
 * {@link cn.org.autumn.modules.usr.dao.UserTokenDao} 的可移植 SQL。
 * <p>
 * 列名经 {@link #quote(String)}；取单行用 {@link #limitOne()}，与各库方言一致（见 {@link RuntimeSqlDialect#limitOne()}）。
 */
public class UserTokenDaoSql extends RuntimeSql {

    public String getToken() {
        return "select * from usr_user_token where " + quote("token") + " = #{token}";
    }

    public String getUuid() {
        return "select * from usr_user_token where " + quote("uuid") + " = #{uuid}" + limitOne();
    }

    public String getUser() {
        return "select * from usr_user_token where " + quote("user_uuid") + " = #{userUuid}";
    }

    public String deleteUser() {
        return "delete from usr_user_token where " + quote("user_uuid") + " = #{userUuid}";
    }

    public String deleteUuid() {
        return "delete from usr_user_token where " + quote("uuid") + " = #{uuid}";
    }
}
