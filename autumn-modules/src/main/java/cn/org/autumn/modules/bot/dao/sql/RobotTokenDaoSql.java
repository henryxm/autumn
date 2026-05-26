package cn.org.autumn.modules.bot.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;

public class RobotTokenDaoSql extends RuntimeSql {

    private String tbl() {
        return quote("bot_robot_token");
    }

    public String getByUuid() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("uuid") + " = #{uuid}" + limitOne();
    }

    public String getByTokenHash() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("token") + " = #{token}" + limitOne();
    }

    public String listByTokenPrefix() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("token_prefix") + " = #{tokenPrefix}";
    }

    public String listActiveByRobot() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("robot") + " = #{robot} AND " + quote("status") + " = 1";
    }

    public String countByRobot() {
        return "SELECT COUNT(*) FROM " + tbl() + " WHERE " + quote("robot") + " = #{robot}";
    }

    public String countActiveByRobot() {
        return "SELECT COUNT(*) FROM " + tbl() + " WHERE " + quote("robot") + " = #{robot} AND " + quote("status") + " = 1";
    }

    public String revokeByRobot() {
        return "UPDATE " + tbl() + " SET " + quote("status") + " = 0, " + quote("update_time") + " = #{updateTime} WHERE " + quote("robot") + " = #{robot}";
    }

    /** 查询最旧已作废令牌（供按主键删除，兼容 H2 等不支持 DELETE ORDER BY LIMIT 的库）。 */
    public String getOldestRevokedByRobot() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("robot") + " = #{robot} AND " + quote("status") + " = 0 ORDER BY " + quote("id") + " ASC" + limitOne();
    }

    public String deleteOldestRevokedByRobot() {
        return "DELETE FROM " + tbl() + " WHERE " + quote("robot") + " = #{robot} AND " + quote("status") + " = 0 ORDER BY " + quote("id") + " ASC" + limitOne();
    }

    public String deleteOldestByRobot() {
        return "DELETE FROM " + tbl() + " WHERE " + quote("robot") + " = #{robot} ORDER BY " + quote("id") + " ASC" + limitOne();
    }

    public String revokeByUuid() {
        return "UPDATE " + tbl() + " SET " + quote("status") + " = 0, " + quote("update_time") + " = #{updateTime} WHERE " + quote("uuid") + " = #{uuid} AND " + quote("status") + " = 1";
    }
}
