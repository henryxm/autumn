package cn.org.autumn.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface NameRunnable extends Runnable {
    Logger log = LoggerFactory.getLogger(NameRunnable.class);

    default String name() {
        return null;
    }

    @Override
    default void run() {
        long start = System.currentTimeMillis();
        try {
            exe();
        } catch (Throwable t) {
            if (log.isDebugEnabled()) {
                log.debug("任务名称:{}, 异常信息:{}", name(), t.getMessage());
            }
        } finally {
            long end = System.currentTimeMillis();
            long time = (end - start);
            if (log.isDebugEnabled()) {
                log.debug("执行任务:{}, 用时:{}毫秒, 线程名:{}", name(), time, Thread.currentThread().getName());
            }
        }
    }

    void exe();
}
