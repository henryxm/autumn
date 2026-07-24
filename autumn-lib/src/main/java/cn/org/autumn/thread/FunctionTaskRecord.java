package cn.org.autumn.thread;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

/**
 * 函数队列任务执行记录（有界历史，供运维页展示）。
 */
@Getter
@Setter
public class FunctionTaskRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private Date enqueueTime;
    private Date startTime;
    private Date endTime;
    private long waitMs;
    private long durationMs;
    private String status;
    private String errorMessage;

    public FunctionTaskRecord() {
    }

    public static FunctionTaskRecord of(String name, long enqueueTimeMs, long startTimeMs, long endTimeMs, String status, String errorMessage) {
        FunctionTaskRecord r = new FunctionTaskRecord();
        r.setName(name != null ? name : "anonymous");
        r.setEnqueueTime(new Date(enqueueTimeMs));
        r.setStartTime(new Date(startTimeMs));
        r.setEndTime(new Date(endTimeMs));
        r.setWaitMs(Math.max(0, startTimeMs - enqueueTimeMs));
        r.setDurationMs(Math.max(0, endTimeMs - startTimeMs));
        r.setStatus(status != null ? status : "UNKNOWN");
        r.setErrorMessage(errorMessage);
        return r;
    }

    public String getEnqueueTimeStr() {
        return format(enqueueTime);
    }

    public String getStartTimeStr() {
        return format(startTime);
    }

    public String getEndTimeStr() {
        return format(endTime);
    }

    public String getDurationStr() {
        return formatDuration(durationMs);
    }

    public String getWaitStr() {
        return formatDuration(waitMs);
    }

    private static String format(Date date) {
        if (date == null)
            return "";
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }

    static String formatDuration(long millis) {
        if (millis < 1000)
            return millis + "ms";
        if (millis < 60000)
            return String.format("%.1fs", millis / 1000.0);
        long minutes = millis / 60000;
        long seconds = (millis % 60000) / 1000;
        return minutes + "m" + seconds + "s";
    }
}
