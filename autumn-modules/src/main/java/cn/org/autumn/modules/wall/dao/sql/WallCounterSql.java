package cn.org.autumn.modules.wall.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSqlDialect;
import cn.org.autumn.database.runtime.RuntimeSqlDialectRegistry;

/**
 * 防火墙计数类表的 UPDATE 语句（COALESCE + 当前时间戳，列名按方言引用）。
 */
public class WallCounterSql {

    private RuntimeSqlDialect d() {
        return RuntimeSqlDialectRegistry.get();
    }

    public String urlBlackBump() {
        String c = d().quote("count");
        return "update wall_url_black set " + c + " = COALESCE(" + c + ",0) + #{count}, "
                + d().quote("user_agent") + " = #{userAgent}, "
                + d().quote("today") + " = COALESCE(" + d().quote("today") + ",0) + #{count}, "
                + d().quote("update_time") + " = " + d().currentTimestamp()
                + " where " + d().quote("url") + " = #{url}";
    }

    public String ipWhiteBump() {
        String c = d().quote("count");
        return "update wall_ip_white set " + c + " = COALESCE(" + c + ",0) + #{count}, "
                + d().quote("user_agent") + " = #{userAgent}, "
                + d().quote("today") + " = COALESCE(" + d().quote("today") + ",0) + #{count}, "
                + d().quote("update_time") + " = " + d().currentTimestamp()
                + " where " + d().quote("ip") + " = #{ip}";
    }

    public String ipVisitBump() {
        String c = d().quote("count");
        return "update wall_ip_visit set " + c + " = COALESCE(" + c + ",0) + #{count}, "
                + d().quote("user_agent") + " = #{userAgent}, "
                + d().quote("host") + " = #{host}, "
                + d().quote("uri") + " = #{uri}, "
                + d().quote("refer") + " = #{refer}, "
                + d().quote("today") + " = COALESCE(" + d().quote("today") + ",0) + #{count}, "
                + d().quote("update_time") + " = " + d().currentTimestamp()
                + " where " + d().quote("ip") + " = #{ip}";
    }

    public String ipBlackBump() {
        String c = d().quote("count");
        return "update wall_ip_black set " + c + " = COALESCE(" + c + ",0) + #{count}, "
                + d().quote("user_agent") + " = #{userAgent}, "
                + d().quote("today") + " = COALESCE(" + d().quote("today") + ",0) + #{count}, "
                + d().quote("update_time") + " = " + d().currentTimestamp()
                + " where " + d().quote("ip") + " = #{ip}";
    }

    public String hostBump() {
        String c = d().quote("count");
        return "update wall_host set " + c + " = COALESCE(" + c + ",0) + #{count}, "
                + d().quote("today") + " = COALESCE(" + d().quote("today") + ",0) + #{count}, "
                + d().quote("update_time") + " = " + d().currentTimestamp()
                + " where " + d().quote("host") + " = #{host}";
    }
}
