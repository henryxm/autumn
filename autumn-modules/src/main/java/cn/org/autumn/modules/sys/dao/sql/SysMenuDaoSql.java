package cn.org.autumn.modules.sys.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;

/**
 * {@link cn.org.autumn.modules.sys.dao.SysMenuDao} 可移植 SQL。
 */
public class SysMenuDaoSql extends RuntimeSql {

    public String getByMenuKey() {
        return "SELECT * FROM " + quote("sys_menu") + " WHERE " + quote("menu_key") + " = #{menuKey}" + limitOne();
    }

    public String getByParentKey() {
        return "SELECT * FROM " + quote("sys_menu") + " WHERE " + quote("parent_key") + " = #{parentKey} ORDER BY " + quote("order_num") + " ASC";
    }

    public String queryNotButtonList() {
        return "SELECT * FROM " + quote("sys_menu") + " WHERE " + quote("type") + " != 2 ORDER BY " + quote("order_num") + " ASC";
    }

    /**
     * {@code IN (#{menuKeys})} 由 {@link cn.org.autumn.mybatis.SelectInLangDriver} 展开。
     */
    public String deleteByMenuKeys() {
        return "DELETE FROM " + quote("sys_menu") + " WHERE " + quote("menu_key") + " IN (#{menuKeys})";
    }
}
