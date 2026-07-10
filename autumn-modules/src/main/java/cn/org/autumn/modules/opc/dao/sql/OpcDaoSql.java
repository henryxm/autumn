package cn.org.autumn.modules.opc.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;

public class OpcDaoSql extends RuntimeSql {

    public String connectAppByAppId() {
        return "SELECT * FROM " + quote("opc_connect_app") + " WHERE " + quote("app_id") + " = #{appId}" + limitOne();
    }

    public String connectAppByUser() {
        return "SELECT * FROM " + quote("opc_connect_app") + " WHERE " + quote("user") + " = #{user} ORDER BY " + quote("create") + " DESC";
    }

    /**
     * 登录页图标文件 hash 是否仍被 OPC 接入应用引用（供 UsingHandler 与文件清理）。
     */
    public String connectAppCountByHashInUse() {
        return "SELECT COUNT(*) FROM " + quote("opc_connect_app")
                + " WHERE " + quote("hash") + " = #{hash}";
    }

    /** 登录页 Tab 显式展示的活跃 OPC 接入应用（pageLogin 1 或 3）。 */
    public String connectAppListPageLoginActive() {
        return "SELECT * FROM " + quote("opc_connect_app")
                + " WHERE " + quote("page_login") + " IN (1, 3) AND " + quote("status") + " = 1"
                + " ORDER BY " + quote("create") + " DESC";
    }

    /** 登录页扫码展示的活跃 OPC 接入应用（pageLogin 2 或 3）。 */
    public String connectAppListPageQrActive() {
        return "SELECT * FROM " + quote("opc_connect_app")
                + " WHERE " + quote("page_login") + " IN (2, 3) AND " + quote("status") + " = 1"
                + " ORDER BY " + quote("create") + " DESC";
    }

    /** appSecret 列是否已落库（密文或明文均可，供登录页 Provider 准入）。 */
    public String connectAppCountSecretByAppId() {
        return "SELECT COUNT(*) FROM " + quote("opc_connect_app")
                + " WHERE " + quote("app_id") + " = #{appId} AND " + quote("app_secret") + " IS NOT NULL"
                + " AND " + quote("app_secret") + " <> ''";
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
