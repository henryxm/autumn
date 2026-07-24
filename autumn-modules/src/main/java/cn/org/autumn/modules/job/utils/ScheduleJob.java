package cn.org.autumn.modules.job.utils;

import cn.org.autumn.config.Config;
import cn.org.autumn.modules.job.entity.ScheduleJobEntity;
import cn.org.autumn.modules.job.entity.ScheduleJobLogEntity;
import cn.org.autumn.modules.job.service.ScheduleJobLogService;
import cn.org.autumn.thread.TagTaskExecutor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 定时任务：提交到专用有界池 {@code scheduleJobExecutor}（与 LoopJob 主池隔离），禁止私建无界线程池。
 */
@Slf4j
@Service
public class ScheduleJob extends QuartzJobBean {

    private static boolean enable = false;

    @Autowired
    private ScheduleJobLogService scheduleJobLogService;

    /** Quartz schedule_job 专用池；与 {@code asyncTaskExecutor}（LoopJob）隔离。 */
    @Autowired(required = false)
    @Qualifier("scheduleJobExecutor")
    private TagTaskExecutor scheduleJobExecutor;

    /**
     * Quartz {@code schedule_job} 单次执行等待上限（秒）。
     * {@code <=0}：不限制，长任务可一直跑完（推荐默认）；{@code >0}：超时后 cancel(true) 并记失败日志。
     * 不影响 LoopJob（onMinute/onDay 等）回调。
     */
    @Value("${autumn.job.schedule-timeout-seconds:0}")
    private long scheduleTimeoutSeconds = 0;

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
        ScheduleJobLogEntity jobLog = null;
        if (enable) {
            jobLog = new ScheduleJobLogEntity();
            jobLog.setJobId(scheduleJob.getJobId());
            jobLog.setBeanName(scheduleJob.getBeanName());
            jobLog.setMethodName(scheduleJob.getMethodName());
            jobLog.setParams(scheduleJob.getParams());
            jobLog.setCreateTime(new Date());
        }
        long startTime = System.currentTimeMillis();
        Future<?> future = null;
        try {
            ScheduleRunnable task = new ScheduleRunnable(scheduleJob.getBeanName(), scheduleJob.getMethodName(), scheduleJob.getParams());
            if (scheduleJobExecutor == null) {
                task.run();
            } else {
                future = scheduleJobExecutor.submit(task);
                if (scheduleTimeoutSeconds > 0) {
                    future.get(scheduleTimeoutSeconds, TimeUnit.SECONDS);
                } else {
                    future.get();
                }
            }
            if (null != jobLog) {
                jobLog.setTimes((int) (System.currentTimeMillis() - startTime));
                jobLog.setStatus(0);
            }
        } catch (TimeoutException e) {
            if (future != null) {
                future.cancel(true);
            }
            log.warn("ScheduleJob timeout bean={} method={} timeoutSec={}", scheduleJob.getBeanName(), scheduleJob.getMethodName(), scheduleTimeoutSeconds);
            if (null != jobLog) {
                jobLog.setTimes((int) (System.currentTimeMillis() - startTime));
                jobLog.setStatus(1);
                jobLog.setError(StringUtils.substring("timeout after " + scheduleTimeoutSeconds + "s", 0, 2000));
            }
        } catch (Exception e) {
            if (null != jobLog) {
                jobLog.setTimes((int) (System.currentTimeMillis() - startTime));
                jobLog.setStatus(1);
                jobLog.setError(StringUtils.substring(e.toString(), 0, 2000));
            }
        } finally {
            if (enable && null != scheduleJobLogService && null != jobLog)
                scheduleJobLogService.save(jobLog);
        }
    }
}
