package cn.org.autumn.modules.job.task;

import cn.org.autumn.site.Factory;
import cn.org.autumn.site.LoadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

@Component
public class LoopJob extends Factory implements LoadFactory.Load {
    static Logger log = LoggerFactory.getLogger(LoopJob.class);

    static Map<Integer, Job> disabled = new HashMap<>();

    public Collection<Job> getDisabledJobs() {
        return disabled.values();
    }

    @Override
    public void load() {
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
        void runJob();

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
        void onOneSecond();
    }

    public interface ThreeSecond extends Job {
        void onThreeSecond();
    }

    public interface FiveSecond extends Job {
        void onFiveSecond();
    }

    public interface TenSecond extends Job {
        void onTenSecond();
    }

    public interface ThirtySecond extends Job {
        void onThirtySecond();
    }

    public interface OneMinute extends Job {
        void onOneMinute();
    }

    public interface TenMinute extends Job {
        void onTenMinute();
    }

    public interface ThirtyMinute extends Job {
        void onThirtyMinute();
    }

    public interface OneHour extends Job {
        void onOneHour();
    }

    public interface TenHour extends Job {
        void onTenHour();
    }

    public interface ThirtyHour extends Job {
        void onThirtyHour();
    }

    public interface OneDay extends Job {
        void onOneDay();
    }

    public interface OneWeek extends Job {
        void onOneWeek();
    }

    private static List<Job> oneSecondJobList = new CopyOnWriteArrayList<>();
    private static List<Job> threeSecondJobList = new CopyOnWriteArrayList<>();
    private static List<Job> fiveSecondJobList = new CopyOnWriteArrayList<>();
    private static List<Job> tenSecondJobList = new CopyOnWriteArrayList<>();
    private static List<Job> thirtySecondJobList = new CopyOnWriteArrayList<>();

    private static List<Job> oneMinuteJobList = new CopyOnWriteArrayList<>();
    private static List<Job> tenMinuteJobList = new CopyOnWriteArrayList<>();
    private static List<Job> thirtyMinuteJobList = new CopyOnWriteArrayList<>();

    private static List<Job> oneHourJobList = new CopyOnWriteArrayList<>();
    private static List<Job> tenHourJobList = new CopyOnWriteArrayList<>();
    private static List<Job> thirtyHourJobList = new CopyOnWriteArrayList<>();

    private static List<Job> oneDayJobList = new CopyOnWriteArrayList<>();
    private static List<Job> oneWeekJobList = new CopyOnWriteArrayList<>();

    public static void onOneSecond(Job job) {
        oneSecondJobList.add(job);
    }

    public static void onThreeSecond(Job job) {
        threeSecondJobList.add(job);
    }

    public static void onFiveSecond(Job job) {
        fiveSecondJobList.add(job);
    }

    public static void onTenSecond(Job job) {
        tenSecondJobList.add(job);
    }

    public static void onThirtySecond(Job job) {
        thirtySecondJobList.add(job);
    }

    public static void onOneMinute(Job job) {
        oneMinuteJobList.add(job);
    }

    public static void onTenMinute(Job job) {
        tenMinuteJobList.add(job);
    }

    public static void onThirtyMinute(Job job) {
        thirtyMinuteJobList.add(job);
    }

    public static void onOneHour(Job job) {
        oneHourJobList.add(job);
    }

    public static void onTenHour(Job job) {
        tenHourJobList.add(job);
    }

    public static void onThirtyHour(Job job) {
        thirtyHourJobList.add(job);
    }

    public static void onOneDay(Job job) {
        oneDayJobList.add(job);
    }

    public static void onOneWeek(Job job) {
        oneWeekJobList.add(job);
    }

    private static void print(Job job, Exception e) {
        log.error("Job(" + job.getClass().getName() + "):" + e.getMessage());
    }

    public static void runOneSecondJob() {
        if (oneSecondJobList.size() > 0) {
            for (Job job : oneSecondJobList) {
                try {
                    if (!job.isEnabled())
                        continue;
                    if (job instanceof OneSecond)
                        ((OneSecond) job).onOneSecond();
                    else
                        job.runJob();
                } catch (Exception e) {
                    print(job, e);
                }
            }
        }
    }

    public static void runThreeSecondJob() {
        if (threeSecondJobList.size() > 0) {
            for (Job job : threeSecondJobList) {
                try {
                    if (!job.isEnabled())
                        continue;
                    if (job instanceof ThreeSecond)
                        ((ThreeSecond) job).onThreeSecond();
                    else
                        job.runJob();
                } catch (Exception e) {
                    print(job, e);
                }
            }
        }
    }

    public static void runFiveSecondJob() {
        if (fiveSecondJobList.size() > 0) {
            for (Job job : fiveSecondJobList) {
                try {
                    if (!job.isEnabled())
                        continue;
                    if (job instanceof FiveSecond)
                        ((FiveSecond) job).onFiveSecond();
                    else
                        job.runJob();
                } catch (Exception e) {
                    print(job, e);
                }
            }
        }
    }

    public static void runTenSecondJob() {
        if (tenSecondJobList.size() > 0) {
            for (Job job : tenSecondJobList) {
                try {
                    if (!job.isEnabled())
                        continue;
                    if (job instanceof TenSecond)
                        ((TenSecond) job).onTenSecond();
                    else
                        job.runJob();
                } catch (Exception e) {
                    print(job, e);
                }
            }
        }
    }

    public static void runThirtySecondJob() {
        if (thirtySecondJobList.size() > 0) {
            for (Job job : thirtySecondJobList) {
                try {
                    if (!job.isEnabled())
                        continue;
                    if (job instanceof ThirtySecond)
                        ((ThirtySecond) job).onThirtySecond();
                    else
                        job.runJob();
                } catch (Exception e) {
                    print(job, e);
                }
            }
        }
    }

    public static void runOneMinuteJob() {
        if (oneMinuteJobList.size() > 0) {
            for (Job job : oneMinuteJobList) {
                try {
                    if (!job.isEnabled())
                        continue;
                    if (job instanceof OneMinute)
                        ((OneMinute) job).onOneMinute();
                    else
                        job.runJob();
                } catch (Exception e) {
                    print(job, e);
                }
            }
        }
    }

    public static void runTenMinuteJob() {
        if (tenMinuteJobList.size() > 0) {
            for (Job job : tenMinuteJobList) {
                try {
                    if (!job.isEnabled())
                        continue;
                    if (job instanceof TenMinute)
                        ((TenMinute) job).onTenMinute();
                    else
                        job.runJob();
                } catch (Exception e) {
                    print(job, e);
                }
            }
        }
    }

    public static void runThirtyMinuteJob() {
        if (thirtyMinuteJobList.size() > 0) {
            for (Job job : thirtyMinuteJobList) {
                try {
                    if (!job.isEnabled())
                        continue;
                    if (job instanceof ThirtyMinute)
                        ((ThirtyMinute) job).onThirtyMinute();
                    else
                        job.runJob();
                } catch (Exception e) {
                    print(job, e);
                }
            }
        }
    }


    public static void runOneHourJob() {
        if (oneHourJobList.size() > 0) {
            for (Job job : oneHourJobList) {
                try {
                    if (!job.isEnabled())
                        continue;
                    if (job instanceof OneHour)
                        ((OneHour) job).onOneHour();
                    else
                        job.runJob();
                } catch (Exception e) {
                    print(job, e);
                }
            }
        }
    }

    public static void runTenHourJob() {
        if (tenHourJobList.size() > 0) {
            for (Job job : tenHourJobList) {
                try {
                    if (!job.isEnabled())
                        continue;
                    if (job instanceof TenHour)
                        ((TenHour) job).onTenHour();
                    else
                        job.runJob();
                } catch (Exception e) {
                    print(job, e);
                }
            }
        }
    }

    public static void runThirtyHourJob() {
        if (thirtyHourJobList.size() > 0) {
            for (Job job : thirtyHourJobList) {
                try {
                    if (!job.isEnabled())
                        continue;
                    if (job instanceof ThirtyHour)
                        ((ThirtyHour) job).onThirtyHour();
                    else
                        job.runJob();
                } catch (Exception e) {
                    print(job, e);
                }
            }
        }
    }

    public static void runOneDayJob() {
        if (oneDayJobList.size() > 0) {
            for (Job job : oneDayJobList) {
                try {
                    if (!job.isEnabled())
                        continue;
                    if (job instanceof OneDay)
                        ((OneDay) job).onOneDay();
                    else
                        job.runJob();
                } catch (Exception e) {
                    print(job, e);
                }
            }
        }
    }

    public static void runOneWeekJob() {
        if (oneWeekJobList.size() > 0) {
            for (Job job : oneWeekJobList) {
                try {
                    if (!job.isEnabled())
                        continue;
                    if (job instanceof OneWeek)
                        ((OneWeek) job).onOneWeek();
                    else
                        job.runJob();
                } catch (Exception e) {
                    print(job, e);
                }
            }
        }
    }
}