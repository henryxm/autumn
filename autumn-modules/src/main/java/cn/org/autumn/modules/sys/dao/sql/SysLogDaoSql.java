package cn.org.autumn.modules.sys.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;

/**
 * {@link cn.org.autumn.modules.sys.dao.SysLogDao} 可移植 SQL。
 */
public class SysLogDaoSql extends RuntimeSql {

    public String clear() {
        return truncateTable("sys_log");
    }
}
