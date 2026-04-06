package cn.org.autumn.modules.sys.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;

/**
 * {@link cn.org.autumn.modules.sys.dao.SysDeptDao} 可移植 SQL。
 */
public class SysDeptDaoSql extends RuntimeSql {

    public String getByParentKey() {
        return "SELECT " + quote("dept_key") + " FROM " + quote("sys_dept") + " WHERE " + quote("parent_key") + " = #{value} AND "
                + quote("del_flag") + " = 0";
    }

    public String getByDeptKey() {
        return "SELECT * FROM " + quote("sys_dept") + " WHERE " + quote("dept_key") + " = #{deptKey}";
    }
}
