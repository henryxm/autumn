package cn.org.autumn.modules.sys.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSqlDialect;
import cn.org.autumn.database.runtime.RuntimeSqlDialectRegistry;

/**
 * {@link cn.org.autumn.modules.sys.dao.SysConfigDao} 可移植 SQL。
 */
public class SysConfigDaoSql {

    private RuntimeSqlDialect d() {
        return RuntimeSqlDialectRegistry.get();
    }

    public String queryByKey() {
        return "SELECT * FROM " + d().quote("sys_config") + " WHERE " + d().quote("param_key") + " = #{paramKey}";
    }

    public String hasKey() {
        return "SELECT COUNT(*) FROM " + d().quote("sys_config") + " WHERE " + d().quote("param_key") + " = #{paramKey}";
    }

    public String updateValueByKey() {
        return "UPDATE " + d().quote("sys_config") + " SET " + d().quote("param_value") + " = #{paramValue} WHERE " + d().quote("param_key") + " = #{paramKey}";
    }
}
