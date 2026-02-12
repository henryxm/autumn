package cn.org.autumn.modules.job.dao;

import cn.org.autumn.modules.job.entity.ScheduleJobEntity;
import cn.org.autumn.mybatis.SelectInLangDriver;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

/**
 * 定时任务
 *
 * @author User
 * @email henryxm@163.com
 * @date 2026-02
 */
@Mapper
@Repository
public interface ScheduleJobDao extends BaseMapper<ScheduleJobEntity> {

    @Update("UPDATE sys_schedule_job SET status = #{status} where job_id IN (#{jobIds})")
    @Lang(SelectInLangDriver.class)
    int updateBatch(@Param("jobIds") Long[] jobIds, @Param("status") int status);
}
