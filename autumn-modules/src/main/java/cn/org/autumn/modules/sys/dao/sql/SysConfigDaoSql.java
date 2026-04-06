package cn.org.autumn.modules.sys.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;

/**
 * {@link cn.org.autumn.modules.sys.dao.SysConfigDao} 可移植 SQL。
 */
public class SysConfigDaoSql extends RuntimeSql {

    public String queryByKey() {
        return "SELECT * FROM " + quote("sys_config") + " WHERE " + quote("param_key") + " = #{paramKey}";
    }

    public String hasKey() {
        return "SELECT COUNT(*) FROM " + quote("sys_config") + " WHERE " + quote("param_key") + " = #{paramKey}";
    }

    public String updateValueByKey() {
        return "UPDATE " + quote("sys_config") + " SET " + quote("param_value") + " = #{paramValue} WHERE " + quote("param_key") + " = #{paramKey}";
    }
}
