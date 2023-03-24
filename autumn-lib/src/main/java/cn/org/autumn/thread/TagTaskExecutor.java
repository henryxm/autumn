package cn.org.autumn.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

public class TagTaskExecutor extends ThreadPoolTaskExecutor {

    static Logger log = LoggerFactory.getLogger(TagTaskExecutor.class);

    static List<Tag> running = new CopyOnWriteArrayList<>();

    public static void remove(Tag task) {
        running.remove(task);
    }

    public void execute(TagRunnable task) {
        running.add(task);
        super.execute(task);
    }

    public Future<?> submit(TagRunnable task) {
        running.add(task);
        return super.submit(task);
    }

    public <T> Future<T> submit(TagCallable<T> task) {
        running.add(task);
        return super.submit(task);
    }

    public List<Tag> getRunning() {
        return running;
    }

    public void print() {
        if (log.isDebugEnabled()) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            for (Tag task : running) {
                log.debug("任务:{}, 标记:{}, 名称:{}", task.getName(), task.getTag(), format.format(task.getTime()));
            }
        }
    }
}
