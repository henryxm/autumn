package cn.org.autumn.modules.qrc.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;

public class ClientGrantDaoSql extends RuntimeSql {

    private String tbl() {
        return quote("qrc_client_grant");
    }

    public String getByUuid() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("uuid") + " = #{uuid}" + limitOne();
    }

    public String getByClientId() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("client_id") + " = #{clientId}" + limitOne();
    }
}
