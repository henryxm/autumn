package cn.org.autumn.modules.job.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;

/**
 * {@link cn.org.autumn.modules.job.dao.ScheduleJobLogDao} 可移植 SQL。
 */
public class ScheduleJobLogDaoSql extends RuntimeSql {

    public String clear() {
        return truncateTable("sys_schedule_job_log");
    }
}
