package cn.org.autumn.modules.job.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;

/**
 * {@link cn.org.autumn.modules.job.dao.ScheduleJobDao} 可移植 SQL；{@code IN (#{jobIds})} 由 {@link cn.org.autumn.mybatis.SelectInLangDriver} 展开。
 */
public class ScheduleJobDaoSql extends RuntimeSql {

    public String updateBatch() {
        return "UPDATE " + quote("sys_schedule_job") + " SET " + quote("status") + " = #{status} WHERE " + quote("job_id") + " IN (#{jobIds})";
    }
}
