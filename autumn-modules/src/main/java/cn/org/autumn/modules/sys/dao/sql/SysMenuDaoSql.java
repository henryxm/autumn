package cn.org.autumn.modules.sys.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSqlDialect;
import cn.org.autumn.database.runtime.RuntimeSqlDialectRegistry;

/**
 * {@link cn.org.autumn.modules.sys.dao.SysMenuDao} 可移植 SQL。
 */
public class SysMenuDaoSql {

    private RuntimeSqlDialect d() {
        return RuntimeSqlDialectRegistry.get();
    }

    public String getByMenuKey() {
        return "SELECT * FROM " + d().quote("sys_menu") + " WHERE " + d().quote("menu_key") + " = #{menuKey}" + d().limitOne();
    }

    public String getByParentKey() {
        return "SELECT * FROM " + d().quote("sys_menu") + " WHERE " + d().quote("parent_key") + " = #{parentKey} ORDER BY " + d().quote("order_num") + " ASC";
    }

    public String queryNotButtonList() {
        return "SELECT * FROM " + d().quote("sys_menu") + " WHERE " + d().quote("type") + " != 2 ORDER BY " + d().quote("order_num") + " ASC";
    }

    /**
     * {@code IN (#{menuKeys})} 由 {@link cn.org.autumn.mybatis.SelectInLangDriver} 展开。
     */
    public String deleteByMenuKeys() {
        return "DELETE FROM " + d().quote("sys_menu") + " WHERE " + d().quote("menu_key") + " IN (#{menuKeys})";
    }
}
