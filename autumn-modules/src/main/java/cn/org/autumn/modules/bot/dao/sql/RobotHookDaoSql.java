package cn.org.autumn.modules.bot.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;

public class RobotHookDaoSql extends RuntimeSql {

    private String tbl() {
        return quote("bot_robot_hook");
    }

    public String getByUuid() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("uuid") + " = #{uuid}" + limitOne();
    }

    public String listByRobot() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("robot") + " = #{robot} ORDER BY " + quote("create_time") + " DESC";
    }

    public String listActiveByRobot() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("robot") + " = #{robot} AND " + quote("status") + " = 1 ORDER BY " + quote("create_time") + " DESC";
    }

    public String countByRobot() {
        return "SELECT COUNT(*) FROM " + tbl() + " WHERE " + quote("robot") + " = #{robot}";
    }
}
