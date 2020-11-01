package cn.org.autumn.modules.job.task;

import cn.org.autumn.annotation.TaskAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JobTask {
    private Logger logger = LoggerFactory.getLogger(getClass());

    private boolean secondJobLock = false;

    private int secondJobCounter = 7;

    private boolean minuteJobLock = false;

    private int minuteJobCounter = 0;

    private boolean hourJobLock = false;

    private int hourJobCounter = 0;

    private boolean dayJobLock = false;

    private boolean weekJobLock = false;

    /**
     * 定时循环任务
     */
    @TaskAware(mode = "all", remark = "秒级定时触发器(每秒触发一次)", cronExpression = "* * * * * ? *")
    public void SecondJob() {
        secondJobCounter++;
        if (secondJobLock)
            return;
        secondJobLock = true;
        try {
            logger.debug("每秒定时触发一次,如果该任务一秒内未执行完毕，下一秒跳过执行, Counter=" + secondJobCounter);
            LoopJob.runOneSecondJob();
            if (secondJobCounter % 10 == 0) {
                logger.debug("每10秒定时触发一次, Counter=" + secondJobCounter);
                LoopJob.runTenSecondJob();
            }
            if (secondJobCounter % 30 == 0) {
                logger.debug("每30秒定时触发一次, Counter=" + secondJobCounter);
                LoopJob.runThirtySecondJob();
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            secondJobLock = false;
            if (secondJobCounter >= 60)
                secondJobCounter = 0;
        }
    }

    @TaskAware(mode = "all", remark = "分钟级定时触发器(每分钟触发一次)", cronExpression = "0 * * * * ? *")
    public void MinuteJob() {
        minuteJobCounter++;
        if (minuteJobLock)
            return;
        minuteJobLock = true;
        try {
            logger.debug("每分钟定时触发一次,如果该任务一分钟内未执行完毕，下一分钟跳过执行, Counter=" + minuteJobCounter);
            LoopJob.runOneMinuteJob();
            if (minuteJobCounter % 10 == 0) {
                logger.debug("每10分钟定时触发一次, Counter=" + minuteJobCounter);
                LoopJob.runTenMinuteJob();
            }
            if (minuteJobCounter % 30 == 0) {
                logger.debug("每30分钟定时触发一次, Counter=" + minuteJobCounter);
                LoopJob.runThirtyMinuteJob();
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            minuteJobLock = false;
            if (minuteJobCounter >= 60)
                minuteJobCounter = 0;
        }
    }

    @TaskAware(mode = "all", remark = "小时级定时触发器(每小时触发一次)", cronExpression = "0 0 * * * ? *")
    public void HourJob() {
        hourJobCounter++;
        if (hourJobLock)
            return;
        hourJobLock = true;
        try {
            logger.debug("每小时定时触发一次,如果该任务一小时内未执行完毕，下一小时跳过执行, Counter=" + hourJobCounter);
            LoopJob.runOneHourJob();
            if (hourJobCounter % 10 == 0) {
                logger.debug("每10小时定时触发一次, Counter=" + hourJobCounter);
                LoopJob.runTenHourJob();
            }
            if (hourJobCounter % 30 == 0) {
                logger.debug("每30小时定时触发一次, Counter=" + hourJobCounter);
                LoopJob.runThirtyHourJob();
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            hourJobLock = false;
            if (hourJobCounter >= 60)
                hourJobCounter = 0;
        }
    }

    @TaskAware(mode = "all", remark = "每天定时触发器(每天触发一次)", cronExpression = "0 0 0 * * ? *")
    public void DayJob() {
        if (dayJobLock)
            return;
        dayJobLock = true;
        try {
            logger.debug("每天定时触发一次,如果该任务一天内未执行完毕，下一天跳过执行");
            LoopJob.runOneDayJob();
        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            dayJobLock = false;
        }
    }

    @TaskAware(mode = "all", remark = "每周定时触发器(每周触发一次)", cronExpression = "0 0 0 ? * MON")
    public void WeekJob() {
        if (weekJobLock)
            return;
        weekJobLock = true;
        try {
            logger.debug("每周定时触发一次,如果该任务一周内未执行完毕，下一周跳过执行");
            LoopJob.runOneWeekJob();
        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            weekJobLock = false;
        }
    }
}
