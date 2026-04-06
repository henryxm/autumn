package cn.org.autumn.modules.wall.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;
import cn.org.autumn.database.runtime.RuntimeSqlDialect;

/**
 * 防火墙相关 Mapper 的可移植 SQL。
 * <p>
 * 列名 {@link RuntimeSqlDialect#quote(String)}；仅「非聚合、可能多行」的 {@code SELECT *} 使用 {@link #limitOne()}；
 * {@code SELECT COUNT(*)} 聚合不再追加 limit（见 {@link RuntimeSqlDialect#limitOne()} 说明）。
 */
public class WallDaoSql extends RuntimeSql {

    public String urlBlackGetByUrl() {
        return "select * from wall_url_black where " + quote("url") + " = #{url}" + limitOne();
    }

    public String urlBlackHasUrl() {
        return "select count(*) from wall_url_black where " + quote("url") + " = #{url}";
    }

    public String urlBlackRefreshToday() {
        return "update wall_url_black set " + quote("today") + " = 0";
    }

    public String urlBlackGetUrls() {
        return "select wh." + quote("url") + " from wall_url_black wh where wh." + quote("forbidden") + " = #{forbidden}";
    }

    public String ipWhiteHasTag() {
        return "select count(*) from wall_ip_white where " + quote("tag") + " = #{tag}";
    }

    public String ipWhiteGetByIp() {
        return "select * from wall_ip_white where " + quote("ip") + " = #{ip}" + limitOne();
    }

    public String ipWhiteHasIp() {
        return "select count(*) from wall_ip_white where " + quote("ip") + " = #{ip}";
    }

    public String ipWhiteRefreshToday() {
        return "update wall_ip_white set " + quote("today") + " = 0";
    }

    public String ipWhiteGetIps() {
        return "select wi." + quote("ip") + " from wall_ip_white wi where wi." + quote("forbidden") + " = #{forbidden}";
    }

    public String ipVisitGetByIp() {
        return "select * from wall_ip_visit where " + quote("ip") + " = #{ip}" + limitOne();
    }

    public String ipVisitHasIp() {
        return "select count(*) from wall_ip_visit where " + quote("ip") + " = #{ip}";
    }

    public String ipVisitRefreshToday() {
        return "update wall_ip_visit set " + quote("today") + " = 0";
    }

    public String ipBlackGetByIp() {
        return "select * from wall_ip_black where " + quote("ip") + " = #{ip}" + limitOne();
    }

    public String ipBlackHasIp() {
        return "select count(*) from wall_ip_black where " + quote("ip") + " = #{ip}";
    }

    public String ipBlackRefreshToday() {
        return "update wall_ip_black set " + quote("today") + " = 0";
    }

    public String ipBlackGetIps() {
        return "select wi." + quote("ip") + " from wall_ip_black wi where wi." + quote("available") + " = #{available}";
    }

    public String hostGetByHost() {
        return "select * from wall_host where " + quote("host") + " = #{host}" + limitOne();
    }

    public String hostHasHost() {
        return "select count(*) from wall_host where " + quote("host") + " = #{host}";
    }

    public String hostRefreshToday() {
        return "update wall_host set " + quote("today") + " = 0";
    }

    public String hostGetHosts() {
        return "select wh." + quote("host") + " from wall_host wh where wh." + quote("forbidden") + " = #{forbidden}";
    }

    public String shieldUris() {
        return "select s." + quote("uri") + " from wall_shield as s where " + quote("enable") + " = " + enabledTrueSqlLiteral();
    }

    public String shieldDefault() {
        return "select * from wall_shield where " + quote("uri") + " = '/'";
    }

    public String shieldHasDefault() {
        return "select count(*) from wall_shield where " + quote("uri") + " = '/'";
    }

    public String jumpEnabled() {
        return "select * from wall_jump where " + quote("enable") + " = " + enabledTrueSqlLiteral();
    }
}
