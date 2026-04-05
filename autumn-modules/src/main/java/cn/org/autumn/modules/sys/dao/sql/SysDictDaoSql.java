package cn.org.autumn.modules.sys.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSqlDialect;
import cn.org.autumn.database.runtime.RuntimeSqlDialectRegistry;

/**
 * {@link cn.org.autumn.modules.sys.dao.SysDictDao} 可移植 SQL。
 */
public class SysDictDaoSql {

    private RuntimeSqlDialect d() {
        return RuntimeSqlDialectRegistry.get();
    }

    public String getByType() {
        return "SELECT * FROM " + d().quote("sys_dict") + " WHERE " + d().quote("type") + " = #{type} ORDER BY " + d().quote("order_num") + " ASC";
    }
}
