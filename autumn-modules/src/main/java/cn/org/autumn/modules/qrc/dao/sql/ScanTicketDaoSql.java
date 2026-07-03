package cn.org.autumn.modules.qrc.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;

public class ScanTicketDaoSql extends RuntimeSql {

    private String tbl() {
        return quote("qrc_scan_ticket");
    }

    public String getByUuid() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("uuid") + " = #{uuid}" + limitOne();
    }

    public String deleteExpiredBefore() {
        return "DELETE FROM " + tbl() + " WHERE " + quote("expired") + " IS NOT NULL AND " + quote("expired") + " < #{beforeTime}";
    }
}
