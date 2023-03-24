package cn.org.autumn.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public abstract class TagRunnable implements Runnable, Tag {
    Logger log = LoggerFactory.getLogger(getClass());

    String name = Thread.currentThread().getName();

    Date time = new Date();

    String tag = "";

    @Override
    public String getName() {
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
        return tag;
    }

    @Override
    public void setTag(String tag) {
        this.tag = tag;
    }

    @Override
    public void run() {
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
                log.debug("执行任务:{}, 用时:{}毫秒, 线程名:{}", getTag(), time, getName());
            }
            TagTaskExecutor.remove(TagRunnable.this);
        }
    }

    public abstract void exe();
}
