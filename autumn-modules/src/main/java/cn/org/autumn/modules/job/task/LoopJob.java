package cn.org.autumn.modules.job.task;

import cn.org.autumn.site.Factory;
import cn.org.autumn.site.LoadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class LoopJob extends Factory implements LoadFactory.Must {
    static Logger log = LoggerFactory.getLogger(LoopJob.class);

    static Map<Integer, Job> disabled = new HashMap<>();

    public Collection<Job> getDisabledJobs() {
        return disabled.values();
    }

    @Override
    public void must() {
        List<OneSecond> oneSeconds = getOrderList(OneSecond.class, "onOneSecond");
        for (OneSecond oneSecond : oneSeconds) {
            onOneSecond(oneSecond);
        }

        List<ThreeSecond> threeSeconds = getOrderList(ThreeSecond.class, "onThreeSecond");
        for (ThreeSecond threeSecond : threeSeconds) {
            onThreeSecond(threeSecond);
        }

        List<FiveSecond> fiveSeconds = getOrderList(FiveSecond.class, "onFiveSecond");
        for (FiveSecond fiveSecond : fiveSeconds) {
            onFiveSecond(fiveSecond);
        }

        List<TenSecond> tenSeconds = getOrderList(TenSecond.class, "onTenSecond");
        for (TenSecond tenSecond : tenSeconds) {
            onTenSecond(tenSecond);
        }

        List<ThirtySecond> thirtySeconds = getOrderList(ThirtySecond.class, "onThirtySecond");
        for (ThirtySecond thirtySecond : thirtySeconds) {
            onThirtySecond(thirtySecond);
        }

        List<OneMinute> oneMinutes = getOrderList(OneMinute.class, "onOneMinute");
        for (OneMinute oneMinute : oneMinutes) {
            onOneMinute(oneMinute);
        }

        List<FiveMinute> fiveMinutes = getOrderList(FiveMinute.class, "onFiveMinute");
        for (FiveMinute fiveMinute : fiveMinutes) {
            onFiveMinute(fiveMinute);
        }

        List<TenMinute> tenMinutes = getOrderList(TenMinute.class, "onTenMinute");
        for (TenMinute tenMinute : tenMinutes) {
            onTenMinute(tenMinute);
        }

        List<ThirtyMinute> thirtyMinutes = getOrderList(ThirtyMinute.class, "onThirtyMinute");
        for (ThirtyMinute thirtyMinute : thirtyMinutes) {
            onThirtyMinute(thirtyMinute);
        }

        List<OneHour> oneHours = getOrderList(OneHour.class, "onOneHour");
        for (OneHour oneHour : oneHours) {
            onOneHour(oneHour);
        }

        List<TenHour> tenHours = getOrderList(TenHour.class, "onTenHour");
        for (TenHour tenHour : tenHours) {
            onTenHour(tenHour);
        }

        List<ThirtyHour> thirtyHours = getOrderList(ThirtyHour.class, "onThirtyHour");
        for (ThirtyHour thirtyHour : thirtyHours) {
            onThirtyHour(thirtyHour);
        }

        List<OneDay> oneDays = getOrderList(OneDay.class, "onOneDay");
        for (OneDay oneDay : oneDays) {
            onOneDay(oneDay);
        }

        List<OneWeek> oneWeeks = getOrderList(OneWeek.class, "onOneWeek");
        for (OneWeek oneWeek : oneWeeks) {
            onOneWeek(oneWeek);
        }
    }

    public interface Job {
        default void runJob() {
        }

        default boolean isEnabled() {
            return !disabled.containsKey(hashCode());
        }

        default void enable() {
            disabled.remove(hashCode());
        }

        default void disable() {
            disabled.put(hashCode(), this);
        }
    }

    public interface OneSecond extends Job {
        default void onOneSecond() {
        }
    }

    public interface ThreeSecond extends Job {
        default void onThreeSecond() {
        }
    }

    public interface FiveSecond extends Job {
        default void onFiveSecond() {
        }
    }

    public interface TenSecond extends Job {
        default void onTenSecond() {
        }
    }

    public interface ThirtySecond extends Job {
        default void onThirtySecond() {
        }
    }

    public interface OneMinute extends Job {
        default void onOneMinute() {
        }
    }

    public interface FiveMinute extends Job {
        default void onFiveMinute() {
        }
    }

    public interface TenMinute extends Job {
        default void onTenMinute() {
        }
    }

    public interface ThirtyMinute extends Job {
        default void onThirtyMinute() {
        }
    }

    public interface OneHour extends Job {
        default void onOneHour() {
        }
    }

    public interface TenHour extends Job {
        default void onTenHour() {
        }
    }

    public interface ThirtyHour extends Job {
        default void onThirtyHour() {
        }
    }

    public interface OneDay extends Job {
        default void onOneDay() {
        }
    }

    public interface OneWeek extends Job {
        default void onOneWeek() {
        }
    }

    private static final List<Job> oneSecondJobList = new CopyOnWriteArrayList<>();
    private static final List<Job> threeSecondJobList = new CopyOnWriteArrayList<>();
    private static final List<Job> fiveSecondJobList = new CopyOnWriteArrayList<>();
    private static final List<Job> tenSecondJobList = new CopyOnWriteArrayList<>();
    private static final List<Job> thirtySecondJobList = new CopyOnWriteArrayList<>();

    private static final List<Job> oneMinuteJobList = new CopyOnWriteArrayList<>();
    private static final List<Job> fiveMinuteJobList = new CopyOnWriteArrayList<>();
    private static final List<Job> tenMinuteJobList = new CopyOnWriteArrayList<>();
    private static final List<Job> thirtyMinuteJobList = new CopyOnWriteArrayList<>();

    private static final List<Job> oneHourJobList = new CopyOnWriteArrayList<>();
    private static final List<Job> tenHourJobList = new CopyOnWriteArrayList<>();
    private static final List<Job> thirtyHourJobList = new CopyOnWriteArrayList<>();

    private static final List<Job> oneDayJobList = new CopyOnWriteArrayList<>();
    private static final List<Job> oneWeekJobList = new CopyOnWriteArrayList<>();

    public static void onOneSecond(Job job) {
        if (!oneSecondJobList.contains(job))
            oneSecondJobList.add(job);
    }

    public static void onThreeSecond(Job job) {
        if (!threeSecondJobList.contains(job))
            threeSecondJobList.add(job);
    }

    public static void onFiveSecond(Job job) {
        if (!fiveSecondJobList.contains(job))
            fiveSecondJobList.add(job);
    }

    public static void onTenSecond(Job job) {
        if (!tenSecondJobList.contains(job))
            tenSecondJobList.add(job);
    }

    public static void onThirtySecond(Job job) {
        if (!thirtySecondJobList.contains(job))
            thirtySecondJobList.add(job);
    }

    public static void onOneMinute(Job job) {
        if (!oneMinuteJobList.contains(job))
            oneMinuteJobList.add(job);
    }

    public static void onFiveMinute(Job job) {
        if (!fiveMinuteJobList.contains(job))
            fiveMinuteJobList.add(job);
    }

    public static void onTenMinute(Job job) {
        if (!tenMinuteJobList.contains(job))
            tenMinuteJobList.add(job);
    }

    public static void onThirtyMinute(Job job) {
        if (!thirtyMinuteJobList.contains(job))
            thirtyMinuteJobList.add(job);
    }

    public static void onOneHour(Job job) {
        if (!oneHourJobList.contains(job))
            oneHourJobList.add(job);
    }

    public static void onTenHour(Job job) {
        if (!tenHourJobList.contains(job))
            tenHourJobList.add(job);
    }

    public static void onThirtyHour(Job job) {
        if (!thirtyHourJobList.contains(job))
            thirtyHourJobList.add(job);
    }

    public static void onOneDay(Job job) {
        if (!oneDayJobList.contains(job))
            oneDayJobList.add(job);
    }

    public static void onOneWeek(Job job) {
        if (!oneWeekJobList.contains(job))
            oneWeekJobList.add(job);
    }

    private static void print(Job job, Exception e) {
        log.error("Job(" + job.getClass().getName() + "):" + e.getMessage());
    }

    public static void runOneSecondJob() {
        for (Job job : oneSecondJobList) {
            try {
                if (!job.isEnabled())
                    continue;
                log.debug("Run One Second Job:{}", job.getClass().getSimpleName());
                if (job instanceof OneSecond)
                    ((OneSecond) job).onOneSecond();
                else
                    job.runJob();
            } catch (Exception e) {
                print(job, e);
            }
        }
    }

    public static void runThreeSecondJob() {
        for (Job job : threeSecondJobList) {
            try {
                if (!job.isEnabled())
                    continue;
                log.debug("Run Three Second Job:{}", job.getClass().getSimpleName());
                if (job instanceof ThreeSecond)
                    ((ThreeSecond) job).onThreeSecond();
                else
                    job.runJob();
            } catch (Exception e) {
                print(job, e);
            }
        }
    }

    public static void runFiveSecondJob() {
        for (Job job : fiveSecondJobList) {
            try {
                if (!job.isEnabled())
                    continue;
                log.debug("Run Five Second Job:{}", job.getClass().getSimpleName());
                if (job instanceof FiveSecond)
                    ((FiveSecond) job).onFiveSecond();
                else
                    job.runJob();
            } catch (Exception e) {
                print(job, e);
            }
        }
    }

    public static void runTenSecondJob() {
        for (Job job : tenSecondJobList) {
            try {
                if (!job.isEnabled())
                    continue;
                log.debug("Run Ten Second Job:{}", job.getClass().getSimpleName());
                if (job instanceof TenSecond)
                    ((TenSecond) job).onTenSecond();
                else
                    job.runJob();
            } catch (Exception e) {
                print(job, e);
            }
        }
    }

    public static void runThirtySecondJob() {
        for (Job job : thirtySecondJobList) {
            try {
                if (!job.isEnabled())
                    continue;
                log.debug("Run Thirty Second Job:{}", job.getClass().getSimpleName());
                if (job instanceof ThirtySecond)
                    ((ThirtySecond) job).onThirtySecond();
                else
                    job.runJob();
            } catch (Exception e) {
                print(job, e);
            }
        }
    }

    public static void runOneMinuteJob() {
        for (Job job : oneMinuteJobList) {
            try {
                if (!job.isEnabled())
                    continue;
                log.debug("Run One Minute Job:{}", job.getClass().getSimpleName());
                if (job instanceof OneMinute)
                    ((OneMinute) job).onOneMinute();
                else
                    job.runJob();
            } catch (Exception e) {
                print(job, e);
            }
        }
    }

    public static void runFiveMinuteJob() {
        for (Job job : fiveMinuteJobList) {
            try {
                if (!job.isEnabled())
                    continue;
                log.debug("Run Five Minute Job:{}", job.getClass().getSimpleName());
                if (job instanceof FiveMinute)
                    ((FiveMinute) job).onFiveMinute();
                else
                    job.runJob();
            } catch (Exception e) {
                print(job, e);
            }
        }
    }

    public static void runTenMinuteJob() {
        for (Job job : tenMinuteJobList) {
            try {
                if (!job.isEnabled())
                    continue;
                log.debug("Run Ten Minute Job:{}", job.getClass().getSimpleName());
                if (job instanceof TenMinute)
                    ((TenMinute) job).onTenMinute();
                else
                    job.runJob();
            } catch (Exception e) {
                print(job, e);
            }
        }
    }

    public static void runThirtyMinuteJob() {
        for (Job job : thirtyMinuteJobList) {
            try {
                if (!job.isEnabled())
                    continue;
                log.debug("Run Thirty Minute Job:{}", job.getClass().getSimpleName());
                if (job instanceof ThirtyMinute)
                    ((ThirtyMinute) job).onThirtyMinute();
                else
                    job.runJob();
            } catch (Exception e) {
                print(job, e);
            }
        }
    }


    public static void runOneHourJob() {
        for (Job job : oneHourJobList) {
            try {
                if (!job.isEnabled())
                    continue;
                log.debug("Run One Hour Job:{}", job.getClass().getSimpleName());
                if (job instanceof OneHour)
                    ((OneHour) job).onOneHour();
                else
                    job.runJob();
            } catch (Exception e) {
                print(job, e);
            }
        }
    }

    public static void runTenHourJob() {
        for (Job job : tenHourJobList) {
            try {
                if (!job.isEnabled())
                    continue;
                log.debug("Run Ten Hour Job:{}", job.getClass().getSimpleName());
                if (job instanceof TenHour)
                    ((TenHour) job).onTenHour();
                else
                    job.runJob();
            } catch (Exception e) {
                print(job, e);
            }
        }
    }

    public static void runOneDayJob() {
        for (Job job : oneDayJobList) {
            try {
                if (!job.isEnabled())
                    continue;
                log.debug("Run Day Job:{}", job.getClass().getSimpleName());
                if (job instanceof OneDay)
                    ((OneDay) job).onOneDay();
                else
                    job.runJob();
            } catch (Exception e) {
                print(job, e);
            }
        }
    }

    public static void runOneWeekJob() {
        for (Job job : oneWeekJobList) {
            try {
                if (!job.isEnabled())
                    continue;
                log.debug("Run Week Job:{}", job.getClass().getSimpleName());
                if (job instanceof OneWeek)
                    ((OneWeek) job).onOneWeek();
                else
                    job.runJob();
            } catch (Exception e) {
                print(job, e);
            }
        }
    }

    public static Map<String, List<String>> print() {
        Map<String, List<String>> map = new HashMap<>();
        List<String> oneSecondJobName = new ArrayList<>();
        for (Job job : oneSecondJobList) {
            oneSecondJobName.add(job.getClass().getName());
        }
        map.put("OneSecondJob", oneSecondJobName);

        List<String> thirtySecondJobName = new ArrayList<>();
        for (Job job : thirtySecondJobList) {
            thirtySecondJobName.add(job.getClass().getName());
        }
        map.put("ThirtySecondJob", thirtySecondJobName);

        List<String> tenSecondJobName = new ArrayList<>();
        for (Job job : tenSecondJobList) {
            tenSecondJobName.add(job.getClass().getName());
        }
        map.put("TenSecondJob", tenSecondJobName);
        return map;
    }
}