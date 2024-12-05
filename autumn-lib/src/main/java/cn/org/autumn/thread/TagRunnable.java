package cn.org.autumn.thread;

import cn.org.autumn.config.Config;
import cn.org.autumn.site.UpgradeFactory;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Date;

public abstract class TagRunnable implements Runnable, Tag {
    Logger log = LoggerFactory.getLogger(getClass());

    String name = Thread.currentThread().getName();

    Date time = new Date();

    String tag = "";

    String id = "";

    String method = "";

    Class<?> type = null;

    TagValue tagValue = null;

    static UpgradeFactory upgradeFactory;

    public TagRunnable() {
    }

    public TagRunnable(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TagValue getTagValue() {
        if (null != tagValue)
            return tagValue;
        try {
            Method mt = getClass().getDeclaredMethod("exe");
            tagValue = mt.getDeclaredAnnotation(TagValue.class);
            return tagValue;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getName() {
        if (StringUtils.isBlank(name)) {
            TagValue t = getTagValue();
            if (null != t)
                return t.name();
        }
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Date getTime() {
        return time;
    }

    @Override
    public void setTime(Date time) {
        this.time = time;
    }

    @Override
    public String getTag() {
        if (StringUtils.isBlank(tag)) {
            TagValue t = getTagValue();
            if (null != t)
                return t.tag();
        }
        return tag;
    }

    @Override
    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getMethod() {
        if (StringUtils.isBlank(method)) {
            TagValue t = getTagValue();
            if (null != t)
                return t.method();
        }
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    @Override
    public Class<?> getType() {
        if (null == type) {
            TagValue t = getTagValue();
            if (null != t)
                return t.type();
        }
        return type;
    }

    @Override
    public void setType(Class<?> type) {
        this.type = type;
    }

    public boolean can() {
        if (null == upgradeFactory) {
            upgradeFactory = (UpgradeFactory) Config.getBean(UpgradeFactory.class);
        }
        //如果系统没启动完成，在不执行定时任务线程
        return null == upgradeFactory || upgradeFactory.isDone();
    }

    @Override
    public void run() {
        if (can()) {
            long start = System.currentTimeMillis();
            try {
                exe();
            } catch (Throwable t) {
                if (log.isDebugEnabled()) {
                    log.debug("任务名称:{}, 异常信息:{}", getTag(), t.getMessage());
                }
            } finally {
                long end = System.currentTimeMillis();
                long time = (end - start);
                if (log.isDebugEnabled()) {
                    log.debug("执行任务:{}, 方法:{}, 用时:{}毫秒, 线程:{}", getTag(), getMethod(), time, getName());
                }
            }
        }
        TagTaskExecutor.remove(TagRunnable.this);
    }

    public void exe() {
        if (log.isDebugEnabled()) {
            log.debug("执行任务:{}, 时间:{}, 线程名:{}", getTag(), time, getName());
        }
    }
}
