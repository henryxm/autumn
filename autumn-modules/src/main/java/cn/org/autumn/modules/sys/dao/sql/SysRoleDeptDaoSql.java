package cn.org.autumn.modules.sys.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSqlDialect;
import cn.org.autumn.database.runtime.RuntimeSqlDialectRegistry;

/**
 * {@link cn.org.autumn.modules.sys.dao.SysRoleDeptDao} 可移植 SQL；{@code IN (#{roleKeys})} 由 {@link cn.org.autumn.mybatis.SelectInLangDriver} 展开。
 */
public class SysRoleDeptDaoSql {

    private RuntimeSqlDialect d() {
        return RuntimeSqlDialectRegistry.get();
    }

    public String getDeptKeys() {
        return "SELECT " + d().quote("dept_key") + " FROM " + d().quote("sys_role_dept") + " WHERE " + d().quote("role_key") + " IN (#{roleKeys})";
    }

    public String deleteByRoleKey() {
        return "DELETE FROM " + d().quote("sys_role_dept") + " WHERE " + d().quote("role_key") + " IN (#{roleKeys})";
    }
}
