package cn.org.autumn.modules.sys.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;

/**
 * {@link cn.org.autumn.modules.sys.dao.SysShiroSessionDao} 可移植 SQL。
 */
public class SysShiroSessionDaoSql extends RuntimeSql {

    private String tbl() {
        return quote("sys_shiro_session");
    }

    public String getBySessionId() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("session_id") + " = #{sessionId}" + limitOne();
    }

    public String deleteBySessionId() {
        return "DELETE FROM " + tbl() + " WHERE " + quote("session_id") + " = #{sessionId}";
    }

    /** 一条 SQL 删除全部已过期会话。 */
    public String deleteExpiredBefore() {
        return "DELETE FROM " + tbl() + " WHERE " + quote("expire_time") + " < #{now}";
    }

    public String deleteByUser() {
        return "DELETE FROM " + tbl() + " WHERE " + quote("user") + " = #{user}";
    }

    /** 未过期会话 id（按用户），供强制下线 / 自助列表合并。 */
    public String listValidSessionIdsByUser() {
        return "SELECT " + quote("session_id") + " FROM " + tbl()
                + " WHERE " + quote("user") + " = #{user} AND (" + quote("expire_time") + " IS NULL OR "
                + quote("expire_time") + " >= #{now}) "
                + limitOffsetMybatisParams("#{limit}", "#{offset}");
    }

    /** 未过期会话 id（全表限量），供管理端会话列表合并。 */
    public String listValidSessionIds() {
        return "SELECT " + quote("session_id") + " FROM " + tbl()
                + " WHERE " + quote("expire_time") + " IS NULL OR " + quote("expire_time") + " >= #{now} "
                + limitOffsetMybatisParams("#{limit}", "#{offset}");
    }
}
