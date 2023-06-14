package cn.org.autumn.modules.job.task;

import cn.org.autumn.annotation.TaskAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JobTask {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private boolean secondJobLock = false;

    private boolean threeSecondJobLock = false;

    private boolean fiveSecondJobLock = false;

    private boolean tenSecondJobLock = false;

    private boolean thirtySecondJobLock = false;

    private static boolean minuteJobLock = false;

    private static boolean fiveMinuteJobLock = false;

    private static boolean tenMinuteJobLock = false;

    private static boolean thirtyMinuteJobLock = false;

    private boolean hourJobLock = false;

    private boolean tenHourJobLock = false;

    private boolean dayJobLock = false;

    private boolean weekJobLock = false;

    /**
     * 定时循环任务
     */
    @TaskAware(mode = "all", remark = "一秒定时触发器", cronExpression = "*/1 * * * * ? *")
    public void SecondJob() {
        try {
            if (secondJobLock)
                return;
            secondJobLock = true;
            if (log.isDebugEnabled())
                log.debug("一秒定时任务");
            LoopJob.runOneSecondJob();
        } catch (Exception e) {
            log.error("SecondJob:", e);
        } finally {
            secondJobLock = false;
        }
    }

    @TaskAware(mode = "all", remark = "三秒定时触发器", cronExpression = "*/3 * * * * ? *")
    public void ThreeSecondJob() {
        try {
            if (threeSecondJobLock)
                return;
            threeSecondJobLock = true;
            if (log.isDebugEnabled())
                log.debug("三秒定时任务");
            LoopJob.runThreeSecondJob();
        } catch (Exception e) {
            log.error("三秒任务:", e);
        } finally {
            threeSecondJobLock = false;
        }
    }

    @TaskAware(mode = "all", remark = "五秒定时触发器", cronExpression = "*/5 * * * * ? *")
    public void FiveSecondJob() {
        try {
            if (fiveSecondJobLock)
                return;
            fiveSecondJobLock = true;
            if (log.isDebugEnabled())
                log.debug("五秒定时任务");
            LoopJob.runFiveSecondJob();
        } catch (Exception e) {
            log.error("五秒任务:", e);
        } finally {
            fiveSecondJobLock = false;
        }
    }

    @TaskAware(mode = "all", remark = "五秒定时触发器", cronExpression = "*/10 * * * * ? *")
    public void TenSecondJob() {
        try {
            if (tenSecondJobLock)
                return;
            tenSecondJobLock = true;
            if (log.isDebugEnabled())
                log.debug("十秒定时任务");
            LoopJob.runTenSecondJob();
        } catch (Exception e) {
            log.error("十秒任务:", e);
        } finally {
            tenSecondJobLock = false;
        }
    }

    @TaskAware(mode = "all", remark = "三十秒定时触发器", cronExpression = "*/30 * * * * ? *")
    public void ThirtySecondJob() {
        try {
            if (thirtySecondJobLock)
                return;
            thirtySecondJobLock = true;
            if (log.isDebugEnabled())
                log.debug("三十秒定时任务");
            LoopJob.runThirtySecondJob();
        } catch (Exception e) {
            log.error("三十秒任务:", e);
        } finally {
            thirtySecondJobLock = false;
        }
    }

    @TaskAware(mode = "all", remark = "一分钟定时触发器", cronExpression = "0 */1 * * * ? *")
    public void MinuteJob() {
        try {
            if (minuteJobLock)
                return;
            minuteJobLock = true;
            if (log.isDebugEnabled())
                log.debug("一分钟定时任务");
            LoopJob.runOneMinuteJob();
        } catch (Exception e) {
            log.error("一分钟任务:", e);
        } finally {
            minuteJobLock = false;
        }
    }

    @TaskAware(mode = "all", remark = "五分钟定时触发器", cronExpression = "0 */5 * * * ? *")
    public void FiveMinuteJob() {
        try {
            if (fiveMinuteJobLock)
                return;
            fiveMinuteJobLock = true;
            if (log.isDebugEnabled())
                log.debug("五分钟定时任务");
            LoopJob.runFiveMinuteJob();
        } catch (Exception e) {
            log.error("五分钟任务:", e);
        } finally {
            fiveMinuteJobLock = false;
        }
    }

    @TaskAware(mode = "all", remark = "十分钟定时触发器", cronExpression = "0 */10 * * * ? *")
    public void TenMinuteJob() {
        try {
            if (tenMinuteJobLock)
                return;
            tenMinuteJobLock = true;
            if (log.isDebugEnabled())
                log.debug("十分钟定时任务");
            LoopJob.runTenMinuteJob();
        } catch (Exception e) {
            log.error("十分钟任务:", e);
        } finally {
            tenMinuteJobLock = false;
        }
    }

    @TaskAware(mode = "all", remark = "三十分钟定时触发器", cronExpression = "0 */30 * * * ? *")
    public void ThirtyMinuteJob() {
        try {
            if (thirtyMinuteJobLock)
                return;
            thirtyMinuteJobLock = true;
            if (log.isDebugEnabled())
                log.debug("三十分钟定时任务");
            LoopJob.runThirtyMinuteJob();
        } catch (Exception e) {
            log.error("三十分钟任务:", e);
        } finally {
            thirtyMinuteJobLock = false;
        }
    }

    @TaskAware(mode = "all", remark = "一小时定时触发器", cronExpression = "0 0 */1 * * ? *")
    public void HourJob() {
        try {
            if (hourJobLock)
                return;
            hourJobLock = true;
            if (log.isDebugEnabled())
                log.debug("一小时定时任务");
            LoopJob.runOneHourJob();
        } catch (Exception e) {
            log.error("一小时任务:", e);
        } finally {
            hourJobLock = false;
        }
    }

    @TaskAware(mode = "all", remark = "十小时定时触发器", cronExpression = "0 0 */10 * * ? *")
    public void TenHourJob() {
        try {
            if (tenHourJobLock)
                return;
            tenHourJobLock = true;
            if (log.isDebugEnabled())
                log.debug("十小时定时任务");
            LoopJob.runTenHourJob();
        } catch (Exception e) {
            log.error("十小时任务:", e);
        } finally {
            tenHourJobLock = false;
        }
    }

    @TaskAware(mode = "all", remark = "一天定时触发器", cronExpression = "0 0 0 * * ? *")
    public void DayJob() {
        try {
            if (dayJobLock)
                return;
            dayJobLock = true;
            if (log.isDebugEnabled())
                log.debug("一天定时任务");
            LoopJob.runOneDayJob();
        } catch (Exception e) {
            log.error("一天任务:", e);
        } finally {
            dayJobLock = false;
        }
    }

    @TaskAware(mode = "all", remark = "一周定时触发器", cronExpression = "0 0 0 ? * MON")
    public void WeekJob() {
        try {
            if (weekJobLock)
                return;
            weekJobLock = true;
            if (log.isDebugEnabled())
                log.debug("一周定时任务");
            LoopJob.runOneWeekJob();
        } catch (Exception e) {
            log.error("一周任务:", e);
        } finally {
            weekJobLock = false;
        }
    }
}
