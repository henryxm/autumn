package cn.org.autumn.modules.sys.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;

/**
 * {@link cn.org.autumn.modules.sys.dao.SysCategoryDao} 可移植 SQL。
 */
public class SysCategoryDaoSql extends RuntimeSql {

    public String has() {
        return "SELECT COUNT(*) FROM " + quote("sys_category") + " WHERE " + quote("category") + " = #{category}";
    }

    public String getByCategory() {
        return "SELECT * FROM " + quote("sys_category") + " WHERE " + quote("category") + " = #{category}";
    }
}
