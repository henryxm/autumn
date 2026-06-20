package cn.org.autumn.modules.bot.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;

public class RobotConfigDaoSql extends RuntimeSql {

    private String tbl() {
        return quote("bot_robot_config");
    }

    public String getUser() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("user") + " = #{user}" + limitOne();
    }
}
