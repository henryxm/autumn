package cn.org.autumn.modules.safe.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;

public class PayUserSecuritySettingDaoSql extends RuntimeSql {

    private String tbl() {
        return quote("safe_pay_user_security_setting");
    }

    public String getByUserUuid() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("user_uuid") + " = #{userUuid}" + limitOne();
    }
}
