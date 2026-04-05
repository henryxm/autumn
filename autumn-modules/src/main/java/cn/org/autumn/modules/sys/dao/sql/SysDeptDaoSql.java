package cn.org.autumn.modules.sys.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSqlDialect;
import cn.org.autumn.database.runtime.RuntimeSqlDialectRegistry;

/**
 * {@link cn.org.autumn.modules.sys.dao.SysDeptDao} 可移植 SQL。
 */
public class SysDeptDaoSql {

    private RuntimeSqlDialect d() {
        return RuntimeSqlDialectRegistry.get();
    }

    public String getByParentKey() {
        return "SELECT " + d().quote("dept_key") + " FROM " + d().quote("sys_dept") + " WHERE " + d().quote("parent_key") + " = #{value} AND "
                + d().quote("del_flag") + " = 0";
    }

    public String getByDeptKey() {
        return "SELECT * FROM " + d().quote("sys_dept") + " WHERE " + d().quote("dept_key") + " = #{deptKey}";
    }
}
