package cn.org.autumn.modules.usr.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;

/**
 * {@link cn.org.autumn.modules.usr.dao.UserOpenDao} 可移植 SQL。
 */
public class UserOpenDaoSql extends RuntimeSql {

    private String tbl() {
        return quote("usr_user_open");
    }

    public String getByOpenidAndPlatformAndAppid() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("openid") + " = #{openid} AND " + quote("platform") + " = #{platform} AND "
                + quote("appid") + " = #{appid} AND " + quote("deleted") + " = 0" + limitOne();
    }

    public String getByUnionidAndPlatformAndAppid() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("unionid") + " = #{unionid} AND " + quote("platform") + " = #{platform} AND "
                + quote("appid") + " = #{appid} AND " + quote("deleted") + " = 0" + limitOne();
    }

    public String getByOpenidAndPlatform() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("openid") + " = #{openid} AND " + quote("platform") + " = #{platform} AND "
                + quote("deleted") + " = 0";
    }

    public String getByUnionidAndPlatform() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("unionid") + " = #{unionid} AND " + quote("platform") + " = #{platform} AND "
                + quote("deleted") + " = 0";
    }

    public String getByUuid() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("uuid") + " = #{uuid} AND " + quote("deleted") + " = 0";
    }

    public String getByUuidAndPlatform() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("uuid") + " = #{uuid} AND " + quote("platform") + " = #{platform} AND "
                + quote("deleted") + " = 0";
    }

    public String getByOpenid() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("openid") + " = #{openid} AND " + quote("deleted") + " = 0";
    }

    public String getByUnionid() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("unionid") + " = #{unionid} AND " + quote("deleted") + " = 0";
    }

    public String getAllByUuid() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("uuid") + " = #{uuid}";
    }

    public String deleteByOpenidAndPlatformAndAppid() {
        return "DELETE FROM " + tbl() + " WHERE " + quote("openid") + " = #{openid} AND " + quote("platform") + " = #{platform} AND "
                + quote("appid") + " = #{appid}";
    }

    public String deleteByUuid() {
        return "DELETE FROM " + tbl() + " WHERE " + quote("uuid") + " = #{uuid}";
    }

    public String deleteByUuidAndPlatform() {
        return "DELETE FROM " + tbl() + " WHERE " + quote("uuid") + " = #{uuid} AND " + quote("platform") + " = #{platform}";
    }

    public String deleteByUnionidAndPlatform() {
        return "DELETE FROM " + tbl() + " WHERE " + quote("unionid") + " = #{unionid} AND " + quote("platform") + " = #{platform}";
    }
}
