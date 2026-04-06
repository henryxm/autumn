package cn.org.autumn.modules.sys.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;

/**
 * {@link cn.org.autumn.modules.sys.dao.SysDictDao} 可移植 SQL。
 */
public class SysDictDaoSql extends RuntimeSql {

    public String getByType() {
        return "SELECT * FROM " + quote("sys_dict") + " WHERE " + quote("type") + " = #{type} ORDER BY " + quote("order_num") + " ASC";
    }
}
