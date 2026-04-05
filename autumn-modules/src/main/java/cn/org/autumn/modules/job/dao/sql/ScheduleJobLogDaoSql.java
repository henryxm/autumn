package cn.org.autumn.modules.job.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSqlDialect;
import cn.org.autumn.database.runtime.RuntimeSqlDialectRegistry;

/**
 * {@link cn.org.autumn.modules.job.dao.ScheduleJobLogDao} 可移植 SQL。
 */
public class ScheduleJobLogDaoSql {

    private RuntimeSqlDialect d() {
        return RuntimeSqlDialectRegistry.get();
    }

    public String clear() {
        return d().truncateTable("sys_schedule_job_log");
    }
}
