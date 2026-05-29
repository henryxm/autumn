package cn.org.autumn.modules.safe.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;

public class PayUserBiometricDaoSql extends RuntimeSql {

    private String tbl() {
        return quote("safe_pay_user_biometric");
    }

    public String getByUserAndDevice() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("user_uuid") + " = #{userUuid} AND " + quote("device_id") + " = #{deviceId}" + limitOne();
    }

    public String listActiveByUser() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("user_uuid") + " = #{userUuid} AND " + quote("status") + " = 1 ORDER BY " + quote("id") + " DESC";
    }

    public String countActiveByUser() {
        return "SELECT COUNT(*) FROM " + tbl() + " WHERE " + quote("user_uuid") + " = #{userUuid} AND " + quote("status") + " = 1";
    }

    public String revokeByUserAndDevice() {
        return "UPDATE " + tbl() + " SET " + quote("status") + " = 0, " + quote("update_time") + " = #{updateTime} WHERE " + quote("user_uuid") + " = #{userUuid} AND " + quote("device_id") + " = #{deviceId} AND " + quote("status") + " = 1";
    }
}
