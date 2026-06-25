package cn.org.autumn.modules.job.task;

import cn.org.autumn.annotation.TaskAware;
import cn.org.autumn.site.UpgradeFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JobTask implements UpgradeFactory.Upgrade {

    /**
     * 使用 AtomicBoolean 替代原始 boolean，通过 compareAndSet 保证原子性的检查并加锁操作。
     * 修复原始代码中 check-then-act 竞态条件和非 volatile 内存可见性两个严重并发缺陷。
     */
    private final AtomicBoolean secondJobLock = new AtomicBoolean(false);
    private final AtomicBoolean threeSecondJobLock = new AtomicBoolean(false);
    private final AtomicBoolean fiveSecondJobLock = new AtomicBoolean(false);
    private final AtomicBoolean tenSecondJobLock = new AtomicBoolean(false);
    private final AtomicBoolean thirtySecondJobLock = new AtomicBoolean(false);
    private final AtomicBoolean minuteJobLock = new AtomicBoolean(false);
    private final AtomicBoolean fiveMinuteJobLock = new AtomicBoolean(false);
    private final AtomicBoolean tenMinuteJobLock = new AtomicBoolean(false);
    private final AtomicBoolean thirtyMinuteJobLock = new AtomicBoolean(false);
    private final AtomicBoolean hourJobLock = new AtomicBoolean(false);
    private final AtomicBoolean tenHourJobLock = new AtomicBoolean(false);
    private final AtomicBoolean dayJobLock = new AtomicBoolean(false);
    private final AtomicBoolean weekJobLock = new AtomicBoolean(false);

    private static volatile boolean ready = false;

    public static void ready() {
        ready = true;
    }

    @Override
    @Order(Integer.MAX_VALUE / 10)
    public void upgrade() {
        ready();
    }

    /**
     * 一秒定时触发器
     * <p>
     * 使用 compareAndSet(false, true) 原子地完成"检查锁+加锁"两步操作，
     * 避免原始 boolean 的竞态条件（两个线程同时读到 false 同时进入执行）。
     */
    @TaskAware(mode = "all", remark = "一秒定时触发器", cronExpression = "*/1 * * * * ? *")
    public void SecondJob() {
        if (!ready || !secondJobLock.compareAndSet(false, true))
            return;
        try {
            LoopJob.runOneSecondJob();
        } catch (Exception e) {
            log.error("SecondJob:", e);
        } finally {
            secondJobLock.set(false);
        }
    }

    @TaskAware(mode = "all", remark = "三秒定时触发器", cronExpression = "*/3 * * * * ? *")
    public void ThreeSecondJob() {
        if (!ready || !threeSecondJobLock.compareAndSet(false, true))
            return;
        try {
            LoopJob.runThreeSecondJob();
        } catch (Exception e) {
            log.error("Three-second job:", e);
        } finally {
            threeSecondJobLock.set(false);
        }
    }

    @TaskAware(mode = "all", remark = "五秒定时触发器", cronExpression = "*/5 * * * * ? *")
    public void FiveSecondJob() {
        if (!ready || !fiveSecondJobLock.compareAndSet(false, true))
            return;
        try {
            LoopJob.runFiveSecondJob();
        } catch (Exception e) {
            log.error("Five-second job:", e);
        } finally {
            fiveSecondJobLock.set(false);
        }
    }

    @TaskAware(mode = "all", remark = "十秒定时触发器", cronExpression = "*/10 * * * * ? *")
    public void TenSecondJob() {
        if (!ready || !tenSecondJobLock.compareAndSet(false, true))
            return;
        try {
            LoopJob.runTenSecondJob();
        } catch (Exception e) {
            log.error("Ten-second job:", e);
        } finally {
            tenSecondJobLock.set(false);
        }
    }

    @TaskAware(mode = "all", remark = "三十秒定时触发器", cronExpression = "*/30 * * * * ? *")
    public void ThirtySecondJob() {
        if (!ready || !thirtySecondJobLock.compareAndSet(false, true))
            return;
        try {
            LoopJob.runThirtySecondJob();
        } catch (Exception e) {
            log.error("Thirty-second job:", e);
        } finally {
            thirtySecondJobLock.set(false);
        }
    }

    @TaskAware(mode = "all", remark = "一分钟定时触发器", cronExpression = "0 */1 * * * ? *")
    public void MinuteJob() {
        if (!ready || !minuteJobLock.compareAndSet(false, true))
            return;
        try {
            LoopJob.runOneMinuteJob();
        } catch (Exception e) {
            log.error("One-minute job:", e);
        } finally {
            minuteJobLock.set(false);
        }
    }

    @TaskAware(mode = "all", remark = "五分钟定时触发器", cronExpression = "0 */5 * * * ? *")
    public void FiveMinuteJob() {
        if (!ready || !fiveMinuteJobLock.compareAndSet(false, true))
            return;
        try {
            LoopJob.runFiveMinuteJob();
        } catch (Exception e) {
            log.error("Five-minute job:", e);
        } finally {
            fiveMinuteJobLock.set(false);
        }
    }

    @TaskAware(mode = "all", remark = "十分钟定时触发器", cronExpression = "0 */10 * * * ? *")
    public void TenMinuteJob() {
        if (!ready || !tenMinuteJobLock.compareAndSet(false, true))
            return;
        try {
            LoopJob.runTenMinuteJob();
        } catch (Exception e) {
            log.error("Ten-minute job:", e);
        } finally {
            tenMinuteJobLock.set(false);
        }
    }

    @TaskAware(mode = "all", remark = "三十分钟定时触发器", cronExpression = "0 */30 * * * ? *")
    public void ThirtyMinuteJob() {
        if (!ready || !thirtyMinuteJobLock.compareAndSet(false, true))
            return;
        try {
            LoopJob.runThirtyMinuteJob();
        } catch (Exception e) {
            log.error("Thirty-minute job:", e);
        } finally {
            thirtyMinuteJobLock.set(false);
        }
    }

    @TaskAware(mode = "all", remark = "一小时定时触发器", cronExpression = "0 0 */1 * * ? *")
    public void HourJob() {
        if (!ready || !hourJobLock.compareAndSet(false, true))
            return;
        try {
            LoopJob.runOneHourJob();
        } catch (Exception e) {
            log.error("One-hour job:", e);
        } finally {
            hourJobLock.set(false);
        }
    }

    @TaskAware(mode = "all", remark = "十小时定时触发器", cronExpression = "0 0 */10 * * ? *")
    public void TenHourJob() {
        if (!ready || !tenHourJobLock.compareAndSet(false, true))
            return;
        try {
            LoopJob.runTenHourJob();
        } catch (Exception e) {
            log.error("Ten-hour job:", e);
        } finally {
            tenHourJobLock.set(false);
        }
    }

    @TaskAware(mode = "all", remark = "一天定时触发器", cronExpression = "0 0 0 * * ? *")
    public void DayJob() {
        if (!ready || !dayJobLock.compareAndSet(false, true))
            return;
        try {
            LoopJob.runOneDayJob();
        } catch (Exception e) {
            log.error("One-day job:", e);
        } finally {
            dayJobLock.set(false);
        }
    }

    @TaskAware(mode = "all", remark = "一周定时触发器", cronExpression = "0 0 0 ? * MON")
    public void WeekJob() {
        if (!ready || !weekJobLock.compareAndSet(false, true))
            return;
        try {
            LoopJob.runOneWeekJob();
        } catch (Exception e) {
            log.error("One-week job:", e);
        } finally {
            weekJobLock.set(false);
        }
    }
}
