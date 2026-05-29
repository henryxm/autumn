package cn.org.autumn.modules.bot.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;
import cn.org.autumn.modules.bot.entity.RobotEntity;

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

    /**
     * 占用创建配额：正常(1) + 停用(0)；不含软删(-1)与销毁(-2)。
     */
    public String countByOwnerForQuota() {
        return "SELECT COUNT(*) FROM " + tbl() + " WHERE " + quote("owner") + " = #{owner} AND " + quote("status") + " >= 0";
    }

    public String countSoftDeletedByOwner() {
        return "SELECT COUNT(*) FROM " + tbl() + " WHERE " + quote("owner") + " = #{owner} AND " + quote("status") + " = -1";
    }

    public String listUuidsSoftDeletedByOwner() {
        return "SELECT " + quote("uuid") + " FROM " + tbl()
                + " WHERE " + quote("owner") + " = #{owner} AND " + quote("status") + " = -1";
    }

    public String listUuidsDeletedBefore() {
        return "SELECT " + quote("uuid") + " FROM " + tbl()
                + " WHERE " + quote("status") + " = -1"
                + " AND " + quote("delete_time") + " IS NOT NULL AND " + quote("delete_time") + " < #{beforeTime}"
                + " ORDER BY " + quote("delete_time") + " ASC";
    }

    /**
     * 头像文件 hash 是否仍被未销毁的机器人引用（供 UsingHandler 与文件清理）。
     */
    public String countByHashInUse() {
        return "SELECT COUNT(*) FROM " + tbl()
                + " WHERE " + quote("hash") + " = #{hash}"
                + " AND " + quote("status") + " > " + RobotEntity.STATUS_DESTROYED;
    }
}
