package cn.org.autumn.modules.sys.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;

/**
 * {@link cn.org.autumn.modules.sys.dao.SysRoleDeptDao} 可移植 SQL；{@code IN (#{roleKeys})} 由 {@link cn.org.autumn.mybatis.SelectInLangDriver} 展开。
 */
public class SysRoleDeptDaoSql extends RuntimeSql {

    public String getDeptKeys() {
        return "SELECT " + quote("dept_key") + " FROM " + quote("sys_role_dept") + " WHERE " + quote("role_key") + " IN (#{roleKeys})";
    }

    public String deleteByRoleKey() {
        return "DELETE FROM " + quote("sys_role_dept") + " WHERE " + quote("role_key") + " IN (#{roleKeys})";
    }
}
