package cn.org.autumn.modules.job.task;

import java.util.ArrayList;
import java.util.List;

public class LoopJob {

    private LoopJob() {
    }

    public interface Job {
        void runJob();
    }

    private static List<Job> oneSecondJobList = new ArrayList<>();
    private static List<Job> tenSecondJobList = new ArrayList<>();
    private static List<Job> thirtySecondJobList = new ArrayList<>();


    private static List<Job> oneMinuteJobList = new ArrayList<>();
    private static List<Job> tenMinuteJobList = new ArrayList<>();
    private static List<Job> thirtyMinuteJobList = new ArrayList<>();


    private static List<Job> oneHourJobList = new ArrayList<>();
    private static List<Job> tenHourJobList = new ArrayList<>();
    private static List<Job> thirtyHourJobList = new ArrayList<>();

    private static List<Job> oneDayJobList = new ArrayList<>();
    private static List<Job> oneWeekJobList = new ArrayList<>();

    public static void onOneSecond(Job job) {
        oneSecondJobList.add(job);
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

    public static void runOneSecondJob() {
        if (oneSecondJobList.size() > 0) {
            for (Job job : oneSecondJobList) {
                job.runJob();
            }
        }
    }

    public static void runTenSecondJob() {
        if (tenSecondJobList.size() > 0) {
            for (Job job : tenSecondJobList) {
                job.runJob();
            }
        }
    }

    public static void runThirtySecondJob() {
        if (thirtySecondJobList.size() > 0) {
            for (Job job : thirtySecondJobList) {
                job.runJob();
            }
        }
    }

    public static void runOneMinuteJob() {
        if (oneMinuteJobList.size() > 0) {
            for (Job job : oneMinuteJobList) {
                job.runJob();
            }
        }
    }

    public static void runTenMinuteJob() {
        if (tenMinuteJobList.size() > 0) {
            for (Job job : tenMinuteJobList) {
                job.runJob();
            }
        }
    }

    public static void runThirtyMinuteJob() {
        if (thirtyMinuteJobList.size() > 0) {
            for (Job job : thirtyMinuteJobList) {
                job.runJob();
            }
        }
    }


    public static void runOneHourJob() {
        if (oneHourJobList.size() > 0) {
            for (Job job : oneHourJobList) {
                job.runJob();
            }
        }
    }

    public static void runTenHourJob() {
        if (tenHourJobList.size() > 0) {
            for (Job job : tenHourJobList) {
                job.runJob();
            }
        }
    }

    public static void runThirtyHourJob() {
        if (thirtyHourJobList.size() > 0) {
            for (Job job : thirtyHourJobList) {
                job.runJob();
            }
        }
    }

    public static void runOneDayJob() {
        if (oneDayJobList.size() > 0) {
            for (Job job : oneDayJobList) {
                job.runJob();
            }
        }
    }

    public static void runOneWeekJob() {
        if (oneWeekJobList.size() > 0) {
            for (Job job : oneWeekJobList) {
                job.runJob();
            }
        }
    }
}
