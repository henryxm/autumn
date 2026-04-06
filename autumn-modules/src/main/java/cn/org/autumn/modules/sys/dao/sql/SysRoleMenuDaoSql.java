package cn.org.autumn.modules.sys.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;

/**
 * {@link cn.org.autumn.modules.sys.dao.SysRoleMenuDao} 可移植 SQL；{@code IN (#{roleKeys})} 由 {@link cn.org.autumn.mybatis.SelectInLangDriver} 展开。
 */
public class SysRoleMenuDaoSql extends RuntimeSql {

    public String getMenuKeys() {
        return "SELECT " + quote("menu_key") + " FROM " + quote("sys_role_menu") + " WHERE " + quote("role_key") + " = #{roleKey}";
    }

    public String deleteByRoleKeys() {
        return "DELETE FROM " + quote("sys_role_menu") + " WHERE " + quote("role_key") + " IN (#{roleKeys})";
    }
}
