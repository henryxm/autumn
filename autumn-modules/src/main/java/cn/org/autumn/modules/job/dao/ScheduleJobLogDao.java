package cn.org.autumn.modules.job.dao;

import cn.org.autumn.modules.job.dao.sql.ScheduleJobLogDaoSql;
import cn.org.autumn.modules.job.entity.ScheduleJobLogEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.UpdateProvider;
import org.springframework.stereotype.Repository;

/**
 * 任务日志
 * 
 * @author User
 * @email henryxm@163.com
 * @date 2026-02
 */
@Mapper
@Repository
public interface ScheduleJobLogDao extends BaseMapper<ScheduleJobLogEntity> {

    @UpdateProvider(type = ScheduleJobLogDaoSql.class, method = "clear")
    void clear();
}
