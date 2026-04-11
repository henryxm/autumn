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
        return "SELECT * FROM " + quote("wall_url_black") + " WHERE " + quote("url") + " = #{url}" + limitOne();
    }

    public String urlBlackHasUrl() {
        return "SELECT COUNT(*) FROM " + quote("wall_url_black") + " WHERE " + quote("url") + " = #{url}";
    }

    public String urlBlackRefreshToday() {
        return "UPDATE " + quote("wall_url_black") + " SET " + quote("today") + " = 0";
    }

    public String urlBlackGetUrls() {
        return "SELECT wh." + quote("url") + " FROM " + quote("wall_url_black") + " wh WHERE wh." + quote("forbidden") + " = #{forbidden}";
    }

    public String ipWhiteHasTag() {
        return "SELECT COUNT(*) FROM " + quote("wall_ip_white") + " WHERE " + quote("tag") + " = #{tag}";
    }

    public String ipWhiteGetByIp() {
        return "SELECT * FROM " + quote("wall_ip_white") + " WHERE " + quote("ip") + " = #{ip}" + limitOne();
    }

    public String ipWhiteHasIp() {
        return "SELECT COUNT(*) FROM " + quote("wall_ip_white") + " WHERE " + quote("ip") + " = #{ip}";
    }

    public String ipWhiteRefreshToday() {
        return "UPDATE " + quote("wall_ip_white") + " SET " + quote("today") + " = 0";
    }

    public String ipWhiteGetIps() {
        return "SELECT wi." + quote("ip") + " FROM " + quote("wall_ip_white") + " wi WHERE wi." + quote("forbidden") + " = #{forbidden}";
    }

    public String ipVisitGetByIp() {
        return "SELECT * FROM " + quote("wall_ip_visit") + " WHERE " + quote("ip") + " = #{ip}" + limitOne();
    }

    public String ipVisitHasIp() {
        return "SELECT COUNT(*) FROM " + quote("wall_ip_visit") + " WHERE " + quote("ip") + " = #{ip}";
    }

    public String ipVisitRefreshToday() {
        return "UPDATE " + quote("wall_ip_visit") + " SET " + quote("today") + " = 0";
    }

    public String ipBlackGetByIp() {
        return "SELECT * FROM " + quote("wall_ip_black") + " WHERE " + quote("ip") + " = #{ip}" + limitOne();
    }

    public String ipBlackHasIp() {
        return "SELECT COUNT(*) FROM " + quote("wall_ip_black") + " WHERE " + quote("ip") + " = #{ip}";
    }

    public String ipBlackRefreshToday() {
        return "UPDATE " + quote("wall_ip_black") + " SET " + quote("today") + " = 0";
    }

    public String ipBlackGetIps() {
        return "SELECT wi." + quote("ip") + " FROM " + quote("wall_ip_black") + " wi WHERE wi." + quote("available") + " = #{available}";
    }

    public String hostGetByHost() {
        return "SELECT * FROM " + quote("wall_host") + " WHERE " + quote("host") + " = #{host}" + limitOne();
    }

    public String hostHasHost() {
        return "SELECT COUNT(*) FROM " + quote("wall_host") + " WHERE " + quote("host") + " = #{host}";
    }

    public String hostRefreshToday() {
        return "UPDATE " + quote("wall_host") + " SET " + quote("today") + " = 0";
    }

    public String hostGetHosts() {
        return "SELECT wh." + quote("host") + " FROM " + quote("wall_host") + " wh WHERE wh." + quote("forbidden") + " = #{forbidden}";
    }

    public String shieldUris() {
        return "SELECT s." + quote("uri") + " FROM " + quote("wall_shield") + " s WHERE s." + quote("enable") + " = " + enabledTrueSqlLiteral();
    }

    public String shieldDefault() {
        return "SELECT * FROM " + quote("wall_shield") + " WHERE " + quote("uri") + " = '/'";
    }

    public String shieldHasDefault() {
        return "SELECT COUNT(*) FROM " + quote("wall_shield") + " WHERE " + quote("uri") + " = '/'";
    }

    public String jumpEnabled() {
        return "SELECT * FROM " + quote("wall_jump") + " WHERE " + quote("enable") + " = " + enabledTrueSqlLiteral();
    }
}
