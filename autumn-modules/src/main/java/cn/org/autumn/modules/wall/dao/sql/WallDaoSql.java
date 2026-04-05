package cn.org.autumn.modules.wall.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSqlDialect;
import cn.org.autumn.database.runtime.RuntimeSqlDialectRegistry;

/**
 * 防火墙相关 Mapper 的可移植 SQL。
 * <p>
 * 列名 {@link RuntimeSqlDialect#quote(String)}；仅「非聚合、可能多行」的 {@code SELECT *} 使用 {@link RuntimeSqlDialect#limitOne()}；
 * {@code SELECT COUNT(*)} 聚合不再追加 limit（见 {@link RuntimeSqlDialect#limitOne()} 说明）。
 */
public class WallDaoSql {

    private RuntimeSqlDialect d() {
        return RuntimeSqlDialectRegistry.get();
    }

    public String urlBlackGetByUrl() {
        return "select * from wall_url_black where " + d().quote("url") + " = #{url}" + d().limitOne();
    }

    public String urlBlackHasUrl() {
        return "select count(*) from wall_url_black where " + d().quote("url") + " = #{url}";
    }

    public String urlBlackRefreshToday() {
        return "update wall_url_black set " + d().quote("today") + " = 0";
    }

    public String urlBlackGetUrls() {
        return "select wh." + d().quote("url") + " from wall_url_black wh where wh." + d().quote("forbidden") + " = #{forbidden}";
    }

    public String ipWhiteHasTag() {
        return "select count(*) from wall_ip_white where " + d().quote("tag") + " = #{tag}";
    }

    public String ipWhiteGetByIp() {
        return "select * from wall_ip_white where " + d().quote("ip") + " = #{ip}" + d().limitOne();
    }

    public String ipWhiteHasIp() {
        return "select count(*) from wall_ip_white where " + d().quote("ip") + " = #{ip}";
    }

    public String ipWhiteRefreshToday() {
        return "update wall_ip_white set " + d().quote("today") + " = 0";
    }

    public String ipWhiteGetIps() {
        return "select wi." + d().quote("ip") + " from wall_ip_white wi where wi." + d().quote("forbidden") + " = #{forbidden}";
    }

    public String ipVisitGetByIp() {
        return "select * from wall_ip_visit where " + d().quote("ip") + " = #{ip}" + d().limitOne();
    }

    public String ipVisitHasIp() {
        return "select count(*) from wall_ip_visit where " + d().quote("ip") + " = #{ip}";
    }

    public String ipVisitRefreshToday() {
        return "update wall_ip_visit set " + d().quote("today") + " = 0";
    }

    public String ipBlackGetByIp() {
        return "select * from wall_ip_black where " + d().quote("ip") + " = #{ip}" + d().limitOne();
    }

    public String ipBlackHasIp() {
        return "select count(*) from wall_ip_black where " + d().quote("ip") + " = #{ip}";
    }

    public String ipBlackRefreshToday() {
        return "update wall_ip_black set " + d().quote("today") + " = 0";
    }

    public String ipBlackGetIps() {
        return "select wi." + d().quote("ip") + " from wall_ip_black wi where wi." + d().quote("available") + " = #{available}";
    }

    public String hostGetByHost() {
        return "select * from wall_host where " + d().quote("host") + " = #{host}" + d().limitOne();
    }

    public String hostHasHost() {
        return "select count(*) from wall_host where " + d().quote("host") + " = #{host}";
    }

    public String hostRefreshToday() {
        return "update wall_host set " + d().quote("today") + " = 0";
    }

    public String hostGetHosts() {
        return "select wh." + d().quote("host") + " from wall_host wh where wh." + d().quote("forbidden") + " = #{forbidden}";
    }

    public String shieldUris() {
        return "select s." + d().quote("uri") + " from wall_shield as s where " + d().quote("enable") + " = 1";
    }

    public String shieldDefault() {
        return "select * from wall_shield where " + d().quote("uri") + " = '/'";
    }

    public String shieldHasDefault() {
        return "select count(*) from wall_shield where " + d().quote("uri") + " = '/'";
    }

    public String jumpEnabled() {
        return "select * from wall_jump where " + d().quote("enable") + " = 1";
    }
}
