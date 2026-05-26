package cn.org.autumn.modules.bot.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;

public class RobotDaoSql extends RuntimeSql {

    private String tbl() {
        return quote("bot_robot");
    }

    public String getByUuid() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("uuid") + " = #{uuid}" + limitOne();
    }

    public String countByUuid() {
        return "SELECT COUNT(*) FROM " + tbl() + " WHERE " + quote("uuid") + " = #{uuid}";
    }

    public String listByOwner() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("owner") + " = #{owner} ORDER BY " + quote("create_time") + " DESC";
    }

    public String listByOwnerManaged() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("owner") + " = #{owner} AND " + quote("status") + " >= 0 ORDER BY " + quote("create_time") + " DESC";
    }

    public String countByOwner() {
        return "SELECT COUNT(*) FROM " + tbl() + " WHERE " + quote("owner") + " = #{owner}";
    }
}
