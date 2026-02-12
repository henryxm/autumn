package cn.org.autumn.modules.job.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.modules.job.task.LoopJob;
import org.springframework.stereotype.Service;
import cn.org.autumn.modules.job.dao.ScheduleJobLogDao;
import cn.org.autumn.modules.job.entity.ScheduleJobLogEntity;

@Service
public class ScheduleJobLogService extends ModuleService<ScheduleJobLogDao, ScheduleJobLogEntity> implements LoopJob.OneHour {
    @Override
    public void onOneHour() {
        baseMapper.clear();
    }
}
