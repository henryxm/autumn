package cn.org.autumn.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;

public class NameTaskExecutor extends ThreadPoolTaskExecutor {

    Logger log = LoggerFactory.getLogger(getClass());

    public void execute(NameRunnable task) {
        super.execute(task);
    }

    public Future<?> submit(NameRunnable task) {
        return super.submit(task);
    }

    public <T> Future<T> submit(NameCallable<T> task) {
        return super.submit(task);
    }

    public BlockingQueue<Runnable> getQueue() {
        return getThreadPoolExecutor().getQueue();
    }

    public void print() {
        if (log.isDebugEnabled()) {
            for (Runnable task : getQueue()) {
                if (task instanceof NameRunnable) {
                    log.debug("线程名称:{}", ((NameRunnable) task).name());
                }
            }
        }
    }
}
