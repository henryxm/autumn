package cn.org.autumn.modules.sys.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSqlDialect;
import cn.org.autumn.database.runtime.RuntimeSqlDialectRegistry;

/**
 * {@link cn.org.autumn.modules.sys.dao.SysRoleMenuDao} 可移植 SQL；{@code IN (#{roleKeys})} 由 {@link cn.org.autumn.mybatis.SelectInLangDriver} 展开。
 */
public class SysRoleMenuDaoSql {

    private RuntimeSqlDialect d() {
        return RuntimeSqlDialectRegistry.get();
    }

    public String getMenuKeys() {
        return "SELECT " + d().quote("menu_key") + " FROM " + d().quote("sys_role_menu") + " WHERE " + d().quote("role_key") + " = #{roleKey}";
    }

    public String deleteByRoleKeys() {
        return "DELETE FROM " + d().quote("sys_role_menu") + " WHERE " + d().quote("role_key") + " IN (#{roleKeys})";
    }
}
