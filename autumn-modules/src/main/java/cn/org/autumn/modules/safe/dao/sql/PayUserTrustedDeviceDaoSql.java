package cn.org.autumn.modules.safe.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;

public class PayUserTrustedDeviceDaoSql extends RuntimeSql {

    private String tbl() {
        return quote("safe_pay_user_trusted_device");
    }

    public String getByUserAndDevice() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("user_uuid") + " = #{userUuid} AND " + quote("device_id") + " = #{deviceId}" + limitOne();
    }

    public String listByUser() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("user_uuid") + " = #{userUuid} ORDER BY " + quote("last_used_time") + " DESC";
    }
}
