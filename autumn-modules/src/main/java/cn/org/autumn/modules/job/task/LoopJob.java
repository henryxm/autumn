package cn.org.autumn.modules.job.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

public class LoopJob {
    static Logger log = LoggerFactory.getLogger(LoopJob.class);

    private LoopJob() {
    }

    public interface Job {
        void runJob();
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
                    job.runJob();
                } catch (Exception e) {
                    print(job, e);
                }
            }
        }
    }
}