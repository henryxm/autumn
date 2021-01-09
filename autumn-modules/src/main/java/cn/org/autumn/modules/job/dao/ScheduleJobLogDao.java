package cn.org.autumn.modules.job.dao;

import cn.org.autumn.modules.job.entity.ScheduleJobLogEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface ScheduleJobLogDao extends BaseMapper<ScheduleJobLogEntity> {

    @Delete("truncate table sys_schedule_job_log")
    void clear();
}
