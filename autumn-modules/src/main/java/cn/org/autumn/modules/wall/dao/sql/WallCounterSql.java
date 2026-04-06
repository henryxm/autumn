package cn.org.autumn.modules.wall.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;

/**
 * 防火墙计数类表的 UPDATE 语句（COALESCE + 当前时间戳，列名按方言引用）。
 */
public class WallCounterSql extends RuntimeSql {

    public String urlBlackBump() {
        String c = quote("count");
        return "update wall_url_black set " + c + " = COALESCE(" + c + ",0) + #{count}, "
                + quote("user_agent") + " = #{userAgent}, "
                + quote("today") + " = COALESCE(" + quote("today") + ",0) + #{count}, "
                + quote("update_time") + " = " + currentTimestamp()
                + " where " + quote("url") + " = #{url}";
    }

    public String ipWhiteBump() {
        String c = quote("count");
        return "update wall_ip_white set " + c + " = COALESCE(" + c + ",0) + #{count}, "
                + quote("user_agent") + " = #{userAgent}, "
                + quote("today") + " = COALESCE(" + quote("today") + ",0) + #{count}, "
                + quote("update_time") + " = " + currentTimestamp()
                + " where " + quote("ip") + " = #{ip}";
    }

    public String ipVisitBump() {
        String c = quote("count");
        return "update wall_ip_visit set " + c + " = COALESCE(" + c + ",0) + #{count}, "
                + quote("user_agent") + " = #{userAgent}, "
                + quote("host") + " = #{host}, "
                + quote("uri") + " = #{uri}, "
                + quote("refer") + " = #{refer}, "
                + quote("today") + " = COALESCE(" + quote("today") + ",0) + #{count}, "
                + quote("update_time") + " = " + currentTimestamp()
                + " where " + quote("ip") + " = #{ip}";
    }

    public String ipBlackBump() {
        String c = quote("count");
        return "update wall_ip_black set " + c + " = COALESCE(" + c + ",0) + #{count}, "
                + quote("user_agent") + " = #{userAgent}, "
                + quote("today") + " = COALESCE(" + quote("today") + ",0) + #{count}, "
                + quote("update_time") + " = " + currentTimestamp()
                + " where " + quote("ip") + " = #{ip}";
    }

    public String hostBump() {
        String c = quote("count");
        return "update wall_host set " + c + " = COALESCE(" + c + ",0) + #{count}, "
                + quote("today") + " = COALESCE(" + quote("today") + ",0) + #{count}, "
                + quote("update_time") + " = " + currentTimestamp()
                + " where " + quote("host") + " = #{host}";
    }
}
