package cn.org.autumn.modules.usr.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSqlDialect;
import cn.org.autumn.database.runtime.RuntimeSqlDialectRegistry;

/**
 * {@link cn.org.autumn.modules.usr.dao.UserOpenDao} 可移植 SQL。
 */
public class UserOpenDaoSql {

    private RuntimeSqlDialect d() {
        return RuntimeSqlDialectRegistry.get();
    }

    private String tbl() {
        return d().quote("usr_user_open");
    }

    public String getByOpenidAndPlatformAndAppid() {
        return "SELECT * FROM " + tbl() + " WHERE " + d().quote("openid") + " = #{openid} AND " + d().quote("platform") + " = #{platform} AND "
                + d().quote("appid") + " = #{appid} AND " + d().quote("deleted") + " = 0" + d().limitOne();
    }

    public String getByUnionidAndPlatformAndAppid() {
        return "SELECT * FROM " + tbl() + " WHERE " + d().quote("unionid") + " = #{unionid} AND " + d().quote("platform") + " = #{platform} AND "
                + d().quote("appid") + " = #{appid} AND " + d().quote("deleted") + " = 0" + d().limitOne();
    }

    public String getByOpenidAndPlatform() {
        return "SELECT * FROM " + tbl() + " WHERE " + d().quote("openid") + " = #{openid} AND " + d().quote("platform") + " = #{platform} AND "
                + d().quote("deleted") + " = 0";
    }

    public String getByUnionidAndPlatform() {
        return "SELECT * FROM " + tbl() + " WHERE " + d().quote("unionid") + " = #{unionid} AND " + d().quote("platform") + " = #{platform} AND "
                + d().quote("deleted") + " = 0";
    }

    public String getByUuid() {
        return "SELECT * FROM " + tbl() + " WHERE " + d().quote("uuid") + " = #{uuid} AND " + d().quote("deleted") + " = 0";
    }

    public String getByUuidAndPlatform() {
        return "SELECT * FROM " + tbl() + " WHERE " + d().quote("uuid") + " = #{uuid} AND " + d().quote("platform") + " = #{platform} AND "
                + d().quote("deleted") + " = 0";
    }

    public String getByOpenid() {
        return "SELECT * FROM " + tbl() + " WHERE " + d().quote("openid") + " = #{openid} AND " + d().quote("deleted") + " = 0";
    }

    public String getByUnionid() {
        return "SELECT * FROM " + tbl() + " WHERE " + d().quote("unionid") + " = #{unionid} AND " + d().quote("deleted") + " = 0";
    }

    public String getAllByUuid() {
        return "SELECT * FROM " + tbl() + " WHERE " + d().quote("uuid") + " = #{uuid}";
    }

    public String deleteByOpenidAndPlatformAndAppid() {
        return "DELETE FROM " + tbl() + " WHERE " + d().quote("openid") + " = #{openid} AND " + d().quote("platform") + " = #{platform} AND "
                + d().quote("appid") + " = #{appid}";
    }

    public String deleteByUuid() {
        return "DELETE FROM " + tbl() + " WHERE " + d().quote("uuid") + " = #{uuid}";
    }

    public String deleteByUuidAndPlatform() {
        return "DELETE FROM " + tbl() + " WHERE " + d().quote("uuid") + " = #{uuid} AND " + d().quote("platform") + " = #{platform}";
    }

    public String deleteByUnionidAndPlatform() {
        return "DELETE FROM " + tbl() + " WHERE " + d().quote("unionid") + " = #{unionid} AND " + d().quote("platform") + " = #{platform}";
    }
}
