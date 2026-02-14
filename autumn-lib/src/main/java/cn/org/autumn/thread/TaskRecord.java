package cn.org.autumn.thread;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 任务执行记录，用于记录已完成/失败的任务信息
 */
@Getter
@Setter
public class TaskRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String tag;
    private String method;
    private String typeName;
    private String id;
    private Date startTime;
    private Date endTime;
    private long duration; // 毫秒
    private String status; // COMPLETED, FAILED
    private String errorMessage;
    private long timeout;  // 超时阈值（秒），0=不限制
    private long delay;    // 错峰延迟窗口（秒），0=不延迟
    private boolean locked; // 是否使用分布式锁

    public TaskRecord() {
    }

    public static TaskRecord fromTag(Tag tag, long duration, String status, String errorMessage) {
        TaskRecord record = new TaskRecord();
        if (tag == null) {
            record.setName("Unknown");
            record.setTag("");
            record.setMethod("");
            record.setTypeName("Unknown");
            record.setId("");
            record.setStartTime(new Date());
            record.setTimeout(0);
            record.setDelay(0);
            record.setLocked(false);
        } else {
            record.setName(nullSafe(tag.getName(), "Unknown"));
            record.setTag(nullSafe(tag.getTag(), ""));
            record.setMethod(nullSafe(tag.getMethod(), ""));
            Class<?> type = tag.getType();
            record.setTypeName(type != null ? type.getSimpleName() : "Unknown");
            record.setId(nullSafe(tag.getId(), ""));
            record.setStartTime(tag.getTime() != null ? tag.getTime() : new Date());
            record.setTimeout(tag.getTimeout());
            record.setDelay(tag.getDelay());
            record.setLocked(tag.isLocked());
        }
        record.setEndTime(new Date());
        record.setDuration(Math.max(0, duration));
        record.setStatus(status != null ? status : "UNKNOWN");
        record.setErrorMessage(errorMessage);
        return record;
    }

    private static String nullSafe(String value, String defaultValue) {
        return value != null ? value : defaultValue;
    }

    public String getStartTimeStr() {
        if (startTime == null) return "";
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(startTime);
    }

    public String getEndTimeStr() {
        if (endTime == null) return "";
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(endTime);
    }

    public String getDurationStr() {
        if (duration < 1000) {
            return duration + "ms";
        } else if (duration < 60000) {
            return String.format("%.1fs", duration / 1000.0);
        } else {
            long minutes = duration / 60000;
            long seconds = (duration % 60000) / 1000;
            return minutes + "m" + seconds + "s";
        }
    }
}
