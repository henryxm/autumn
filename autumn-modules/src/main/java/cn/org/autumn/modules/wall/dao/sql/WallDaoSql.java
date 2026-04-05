package cn.org.autumn.modules.wall.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSqlDialect;
import cn.org.autumn.database.runtime.RuntimeSqlDialectRegistry;

/**
 * 防火墙相关 Mapper 的可移植 SQL（统一 {@code LIMIT 1} 等）。
 */
public class WallDaoSql {

    private RuntimeSqlDialect d() {
        return RuntimeSqlDialectRegistry.get();
    }

    public String urlBlackGetByUrl() {
        return "select * from wall_url_black where url = #{url}" + d().limitOne();
    }

    public String urlBlackHasUrl() {
        return "select count(*) from wall_url_black where url = #{url}" + d().limitOne();
    }

    public String urlBlackRefreshToday() {
        return "update wall_url_black set today = 0";
    }

    public String urlBlackGetUrls() {
        return "select wh.url from wall_url_black wh where wh.forbidden = #{forbidden}";
    }

    public String ipWhiteHasTag() {
        return "select count(*) from wall_ip_white where tag = #{tag}";
    }

    public String ipWhiteGetByIp() {
        return "select * from wall_ip_white where ip = #{ip}" + d().limitOne();
    }

    public String ipWhiteHasIp() {
        return "select count(*) from wall_ip_white where ip = #{ip}" + d().limitOne();
    }

    public String ipWhiteRefreshToday() {
        return "update wall_ip_white set today = 0";
    }

    public String ipWhiteGetIps() {
        return "select wi.ip from wall_ip_white wi where wi.forbidden = #{forbidden}";
    }

    public String ipVisitGetByIp() {
        return "select * from wall_ip_visit where ip = #{ip}" + d().limitOne();
    }

    public String ipVisitHasIp() {
        return "select count(*) from wall_ip_visit where ip = #{ip}";
    }

    public String ipVisitRefreshToday() {
        return "update wall_ip_visit set today = 0";
    }

    public String ipBlackGetByIp() {
        return "select * from wall_ip_black where ip = #{ip}" + d().limitOne();
    }

    public String ipBlackHasIp() {
        return "select count(*) from wall_ip_black where ip = #{ip}" + d().limitOne();
    }

    public String ipBlackRefreshToday() {
        return "update wall_ip_black set today = 0";
    }

    public String ipBlackGetIps() {
        return "select wi.ip from wall_ip_black wi where wi.available = #{available}";
    }

    public String hostGetByHost() {
        return "select * from wall_host where host = #{host}" + d().limitOne();
    }

    public String hostHasHost() {
        return "select count(*) from wall_host where host = #{host}" + d().limitOne();
    }

    public String hostRefreshToday() {
        return "update wall_host set today = 0";
    }

    public String hostGetHosts() {
        return "select wh.host from wall_host wh where wh.forbidden = #{forbidden}";
    }

    public String shieldUris() {
        return "select s.uri from wall_shield as s where enable = 1";
    }

    public String shieldDefault() {
        return "select * from wall_shield where uri = '/'";
    }

    public String shieldHasDefault() {
        return "select count(*) from wall_shield where uri = '/'";
    }

    public String jumpEnabled() {
        return "select * from wall_jump where enable = 1";
    }
}
