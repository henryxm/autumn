package cn.org.autumn.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.Callable;

public abstract class TagCallable<V> implements Callable<V>, Tag {
    Logger log = LoggerFactory.getLogger(getClass());

    String name = Thread.currentThread().getName();

    Date time = new Date();

    String tag = "";

    String method = "";

    Class<?> type = null;

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

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    @Override
    public Class<?> getType() {
        return type;
    }

    @Override
    public void setType(Class<?> type) {
        this.type = type;
    }

    @Override
    public V call() throws Exception {
        long start = System.currentTimeMillis();
        try {
            return exe();
        } catch (Throwable t) {
            if (log.isDebugEnabled()) {
                log.debug("任务名称:{}, 异常信息:{}", getTag(), t.getMessage());
            }
            return null;
        } finally {
            long end = System.currentTimeMillis();
            long time = (end - start);
            if (log.isDebugEnabled()) {
                log.debug("执行任务:{}, 用时:{}毫秒, 线程名:{}", getTag(), time, getName());
            }
            TagTaskExecutor.remove(TagCallable.this);
        }
    }

    public V exe() throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("执行任务:{}, 时间:{}, 线程名:{}", getTag(), time, getName());
        }
        return null;
    }
}