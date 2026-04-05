package cn.org.autumn.modules.sys.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSqlDialect;
import cn.org.autumn.database.runtime.RuntimeSqlDialectRegistry;

/**
 * {@link cn.org.autumn.modules.sys.dao.SysCategoryDao} 可移植 SQL。
 */
public class SysCategoryDaoSql {

    private RuntimeSqlDialect d() {
        return RuntimeSqlDialectRegistry.get();
    }

    public String has() {
        return "SELECT COUNT(*) FROM " + d().quote("sys_category") + " WHERE " + d().quote("category") + " = #{category}";
    }

    public String getByCategory() {
        return "SELECT * FROM " + d().quote("sys_category") + " WHERE " + d().quote("category") + " = #{category}";
    }
}
