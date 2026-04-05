package cn.org.autumn.modules.usr.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSqlDialect;
import cn.org.autumn.database.runtime.RuntimeSqlDialectRegistry;

public class UserTokenDaoSql {

    private RuntimeSqlDialect d() {
        return RuntimeSqlDialectRegistry.get();
    }

    public String getToken() {
        return "select * from usr_user_token where " + d().quote("token") + " = #{token}";
    }

    public String getUuid() {
        return "select * from usr_user_token where " + d().quote("uuid") + " = #{uuid}" + d().limitOne();
    }

    public String getUser() {
        return "select * from usr_user_token where " + d().quote("user_uuid") + " = #{userUuid}";
    }

    public String deleteUser() {
        return "delete from usr_user_token where " + d().quote("user_uuid") + " = #{userUuid}";
    }

    public String deleteUuid() {
        return "delete from usr_user_token where " + d().quote("uuid") + " = #{uuid}";
    }
}
