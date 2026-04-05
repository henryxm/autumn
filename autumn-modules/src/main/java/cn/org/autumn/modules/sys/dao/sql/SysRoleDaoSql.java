package cn.org.autumn.modules.sys.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSqlDialect;
import cn.org.autumn.database.runtime.RuntimeSqlDialectRegistry;

/**
 * {@link cn.org.autumn.modules.sys.dao.SysRoleDao} 可移植 SQL。
 */
public class SysRoleDaoSql {

    private RuntimeSqlDialect d() {
        return RuntimeSqlDialectRegistry.get();
    }

    public String getByRoleKey() {
        return "SELECT * FROM " + d().quote("sys_role") + " WHERE " + d().quote("role_key") + " = #{roleKey}";
    }
}
