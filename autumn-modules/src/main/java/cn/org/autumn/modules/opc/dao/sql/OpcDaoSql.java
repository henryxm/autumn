package cn.org.autumn.modules.opc.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;

public class OpcDaoSql extends RuntimeSql {

    public String connectAppByAppId() {
        return "SELECT * FROM " + quote("opc_connect_app") + " WHERE " + quote("app_id") + " = #{appId}" + limitOne();
    }

    public String connectAppByUser() {
        return "SELECT * FROM " + quote("opc_connect_app") + " WHERE " + quote("user") + " = #{user} ORDER BY " + quote("create") + " DESC";
    }

    public String connectBindByConnectAppAndUser() {
        return "SELECT * FROM " + quote("opc_connect_bind") + " WHERE " + quote("connect_app") + " = #{connectApp} AND " + quote("user") + " = #{user}" + limitOne();
    }

    public String connectBindByConnectAppAndOpenId() {
        return "SELECT * FROM " + quote("opc_connect_bind") + " WHERE " + quote("connect_app") + " = #{connectApp} AND " + quote("open_id") + " = #{openId}" + limitOne();
    }

    public String connectBindByConnectAppAndUnionId() {
        return "SELECT * FROM " + quote("opc_connect_bind") + " WHERE " + quote("connect_app") + " = #{connectApp} AND " + quote("union_id") + " = #{unionId}" + limitOne();
    }
}
