package cn.org.autumn.modules.job.utils;

import cn.org.autumn.config.Config;
import cn.org.autumn.modules.job.entity.ScheduleJobEntity;
import cn.org.autumn.modules.job.entity.ScheduleJobLogEntity;
import cn.org.autumn.modules.job.service.ScheduleJobLogService;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Service;

/**
 * 定时任务
 */
@Slf4j
@Service
public class ScheduleJob extends QuartzJobBean {

    private static boolean enable = false;

    @Autowired
    private ScheduleJobLogService scheduleJobLogService;

    public static void toggle() {
        enable = !enable;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) {
        ScheduleJobEntity scheduleJob = (ScheduleJobEntity) context.getMergedJobDataMap().get(ScheduleJobEntity.JOB_PARAM_KEY);
        String mode = scheduleJob.getMode();
        if (StringUtils.isEmpty(mode) || "off".equalsIgnoreCase(mode))
            return;
        if (!"all".equalsIgnoreCase(mode)) {
            String[] modes = mode.split(",");
            boolean has = false;
            for (String m : modes) {
                for (String a : Config.getInstance().ENVs) {
                    if (a.contains(m)) {
                        has = true;
                        break;
                    }
                }
                if (has) {
                    break;
                }
            }
            if (!has)
                return;
        }
        //数据库保存执行记录
        ScheduleJobLogEntity jobLog = null;
        if (enable) {
            jobLog = new ScheduleJobLogEntity();
            jobLog.setJobId(scheduleJob.getJobId());
            jobLog.setBeanName(scheduleJob.getBeanName());
            jobLog.setMethodName(scheduleJob.getMethodName());
            jobLog.setParams(scheduleJob.getParams());
            jobLog.setCreateTime(new Date());
        }
        //任务开始时间
        long startTime = System.currentTimeMillis();
        try {
            ScheduleRunnable task = new ScheduleRunnable(scheduleJob.getBeanName(), scheduleJob.getMethodName(), scheduleJob.getParams());
            task.run();
            if (null != jobLog) {
                //任务执行总时长
                long times = System.currentTimeMillis() - startTime;
                jobLog.setTimes((int) times);
                //任务状态    0：成功    1：失败
                jobLog.setStatus(0);
            }
        } catch (Exception e) {
            if (null != jobLog) {
                //任务执行总时长
                long times = System.currentTimeMillis() - startTime;
                jobLog.setTimes((int) times);
                //任务状态    0：成功    1：失败
                jobLog.setStatus(1);
                jobLog.setError(StringUtils.substring(e.toString(), 0, 2000));
            }
        } finally {
            if (enable && null != scheduleJobLogService && null != jobLog)
                scheduleJobLogService.insert(jobLog);
        }
    }
}
