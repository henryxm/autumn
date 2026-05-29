package cn.org.autumn.modules.safe.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;

public class PayGateAttemptDaoSql extends RuntimeSql {

    private String tbl() {
        return quote("safe_pay_gate_attempt");
    }

    public String countSameAmountSince() {
        return "SELECT COUNT(1) FROM " + tbl() + " WHERE " + quote("user_uuid") + " = #{userUuid} AND " + quote("amount_cent") + " = #{amountCent} AND " + quote("authorized") + " = 1 AND " + quote("create_time") + " >= #{since}";
    }

    public String deleteOlderThan() {
        return "DELETE FROM " + tbl() + " WHERE " + quote("create_time") + " < #{before}";
    }

    public String countByOrderIdSince() {
        return "SELECT COUNT(1) FROM " + tbl() + " WHERE " + quote("user_uuid") + " = #{userUuid} AND " + quote("order_id") + " = #{orderId} AND " + quote("create_time") + " >= #{since}";
    }

    public String countPasswordlessSince() {
        return "SELECT COUNT(1) FROM " + tbl() + " WHERE " + quote("user_uuid") + " = #{userUuid} AND " + quote("auth_mode") + " = 'PASSWORDLESS' AND " + quote("authorized") + " = 1 AND " + quote("create_time") + " >= #{since}";
    }

    public String sumPasswordlessAmountSince() {
        return "SELECT COALESCE(SUM(" + quote("amount_cent") + "),0) FROM " + tbl() + " WHERE " + quote("user_uuid") + " = #{userUuid} AND " + quote("auth_mode") + " = 'PASSWORDLESS' AND " + quote("authorized") + " = 1 AND " + quote("create_time") + " >= #{since}";
    }
}
