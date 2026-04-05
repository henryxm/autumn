package cn.org.autumn.modules.job.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSqlDialect;
import cn.org.autumn.database.runtime.RuntimeSqlDialectRegistry;

/**
 * {@link cn.org.autumn.modules.job.dao.ScheduleJobDao} 可移植 SQL；{@code IN (#{jobIds})} 由 {@link cn.org.autumn.mybatis.SelectInLangDriver} 展开。
 */
public class ScheduleJobDaoSql {

    private RuntimeSqlDialect d() {
        return RuntimeSqlDialectRegistry.get();
    }

    public String updateBatch() {
        return "UPDATE " + d().quote("sys_schedule_job") + " SET " + d().quote("status") + " = #{status} WHERE " + d().quote("job_id") + " IN (#{jobIds})";
    }
}
