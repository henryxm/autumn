package cn.org.autumn.modules.sys.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;

/**
 * {@link cn.org.autumn.modules.sys.dao.SysRoleDao} 可移植 SQL。
 */
public class SysRoleDaoSql extends RuntimeSql {

    public String getByRoleKey() {
        return "SELECT * FROM " + quote("sys_role") + " WHERE " + quote("role_key") + " = #{roleKey}";
    }
}
