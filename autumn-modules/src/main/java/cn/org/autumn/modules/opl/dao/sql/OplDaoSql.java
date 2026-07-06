package cn.org.autumn.modules.opl.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;

public class OplDaoSql extends RuntimeSql {

    public String openAccountByUser() {
        return "SELECT * FROM " + quote("opl_open_account") + " WHERE " + quote("user") + " = #{user}" + limitOne();
    }

    public String openAccountByUuid() {
        return "SELECT * FROM " + quote("opl_open_account") + " WHERE " + quote("uuid") + " = #{uuid}" + limitOne();
    }

    public String openAppByAppId() {
        return "SELECT * FROM " + quote("opl_open_app") + " WHERE " + quote("app_id") + " = #{appId}" + limitOne();
    }

    public String openAppByAccount() {
        return "SELECT * FROM " + quote("opl_open_app") + " WHERE " + quote("account") + " = #{account} ORDER BY " + quote("create") + " DESC";
    }

    public String openIdentityByAppIdAndUser() {
        return "SELECT * FROM " + quote("opl_open_identity") + " WHERE " + quote("app_id") + " = #{appId} AND " + quote("user") + " = #{user}" + limitOne();
    }

    public String openIdentityByOpenId() {
        return "SELECT * FROM " + quote("opl_open_identity") + " WHERE " + quote("open_id") + " = #{openId}" + limitOne();
    }

    public String openUnionByAccountAndUser() {
        return "SELECT * FROM " + quote("opl_open_union") + " WHERE " + quote("account") + " = #{account} AND " + quote("user") + " = #{user}" + limitOne();
    }

    public String openUnionByUnionId() {
        return "SELECT * FROM " + quote("opl_open_union") + " WHERE " + quote("union_id") + " = #{unionId}" + limitOne();
    }

    public String openCodeByCode() {
        return "SELECT * FROM " + quote("opl_open_code") + " WHERE " + quote("code") + " = #{code}" + limitOne();
    }

    public String openTokenByAccessToken() {
        return "SELECT * FROM " + quote("opl_open_token") + " WHERE " + quote("access_token") + " = #{accessToken}" + limitOne();
    }

    public String openTokenByRefreshToken() {
        return "SELECT * FROM " + quote("opl_open_token") + " WHERE " + quote("refresh_token") + " = #{refreshToken}" + limitOne();
    }

    public String openTokenByAuthCode() {
        return "SELECT * FROM " + quote("opl_open_token") + " WHERE " + quote("auth_code") + " = #{authCode}" + limitOne();
    }
}
