package cn.org.autumn.modules.job.task;

import cn.org.autumn.annotation.TaskAware;
import cn.org.autumn.site.UpgradeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
public class JobTask implements UpgradeFactory.Upgrade {
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

    private static boolean ready = false;

    public static void ready() {
        ready = true;
    }

    @Override
    @Order(Integer.MAX_VALUE / 10)
    public void upgrade() {
        ready();
    }

    /**
     * 定时循环任务
     */
    @TaskAware(mode = "all", remark = "一秒定时触发器", cronExpression = "*/1 * * * * ? *")
    public void SecondJob() {
        try {
            if (secondJobLock || !ready)
                return;
            secondJobLock = true;
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
            if (threeSecondJobLock || !ready)
                return;
            threeSecondJobLock = true;
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
            if (fiveSecondJobLock || !ready)
                return;
            fiveSecondJobLock = true;
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
            if (tenSecondJobLock || !ready)
                return;
            tenSecondJobLock = true;
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
            if (thirtySecondJobLock || !ready)
                return;
            thirtySecondJobLock = true;
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
            if (minuteJobLock || !ready)
                return;
            minuteJobLock = true;
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
            if (fiveMinuteJobLock || !ready)
                return;
            fiveMinuteJobLock = true;
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
            if (tenMinuteJobLock || !ready)
                return;
            tenMinuteJobLock = true;
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
            if (thirtyMinuteJobLock || !ready)
                return;
            thirtyMinuteJobLock = true;
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
            if (hourJobLock || !ready)
                return;
            hourJobLock = true;
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
            if (tenHourJobLock || !ready)
                return;
            tenHourJobLock = true;
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
            if (dayJobLock || !ready)
                return;
            dayJobLock = true;
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
            if (weekJobLock || !ready)
                return;
            weekJobLock = true;
            LoopJob.runOneWeekJob();
        } catch (Exception e) {
            log.error("一周任务:", e);
        } finally {
            weekJobLock = false;
        }
    }
}
