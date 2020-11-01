package cn.org.autumn.modules.job.service;

import cn.org.autumn.modules.job.service.gen.ScheduleJobLogServiceGen;
import cn.org.autumn.modules.job.task.LoopJob;
import org.springframework.stereotype.Service;

@Service
public class ScheduleJobLogService extends ScheduleJobLogServiceGen implements LoopJob.Job {

    @Override
    public int menuOrder() {
        return super.menuOrder();
    }

    @Override
    public String ico() {
        return super.ico();
    }

    public void init() {
        super.init();
        LoopJob.onOneHour(this);
    }

    @Override
    public void runJob() {
        baseMapper.clear();
    }
}
