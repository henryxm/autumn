package cn.org.autumn.modules.usr.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;

/**
 * {@link cn.org.autumn.modules.usr.dao.UserLoginLogDao} 可移植 SQL（保留字列 create、white、allow、logout 等通过 {@link #quote(String)}）。
 */
public class UserLoginLogDaoSql extends RuntimeSql {

    private String tbl() {
        return quote("usr_user_login_log");
    }

    public String insertCustom() {
        String allow = quote("allow");
        String logout = quote("logout");
        String white = quote("white");
        String create = quote("create");
        String uuid = quote("uuid");
        String account = quote("account");
        String way = quote("way");
        String host = quote("host");
        String ip = quote("ip");
        String session = quote("session");
        String path = quote("path");
        String agent = quote("agent");
        String reason = quote("reason");
        return "INSERT INTO " + tbl() + " (" + uuid + ", " + account + ", " + way + ", " + host + ", " + ip + ", " + session + ", " + path + ", " + agent + ", " + reason + ", "
                + allow + ", " + logout + ", " + white + ", " + create + ") VALUES ("
                + "#{e.uuid}, #{e.account}, #{e.way}, #{e.host}, #{e.ip}, #{e.session}, #{e.path}, #{e.agent}, #{e.reason}, "
                + "#{e.allow}, #{e.logout}, #{e.white}, #{e.create})";
    }

    public String selectByUuidOrderByCreateDesc() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("uuid") + " = #{uuid} ORDER BY " + quote("create") + " DESC";
    }

    public String updateWhiteByIdCustom() {
        return "UPDATE " + tbl() + " SET " + quote("white") + " = #{whiteVal} WHERE " + quote("id") + " = #{id}";
    }

    public String deleteByUuidPattern() {
        return "DELETE FROM " + tbl() + " WHERE " + quote("uuid") + " LIKE #{pattern}";
    }

    public String countByUuidAndWhite() {
        return "SELECT COUNT(*) FROM " + tbl() + " WHERE " + quote("uuid") + " = #{uuid} AND " + quote("white") + " = #{whiteVal}";
    }
}
