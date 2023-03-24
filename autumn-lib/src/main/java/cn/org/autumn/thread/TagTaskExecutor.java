package cn.org.autumn.thread;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
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
        print();
        return running;
    }

    public void print() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (Tag task : running) {
            TagValue value = null;
            Method[] methods = task.getClass().getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals("run") || method.getName().equals("exe") || method.getName().equals("call")) {
                    value = method.getDeclaredAnnotation(TagValue.class);
                    if (null != value)
                        break;
                }
            }
            if (null != value) {
                String n = value.name();
                Class<?> t = value.type();
                String me = value.method();
                String tag = value.tag();
                if (StringUtils.isNotBlank(n))
                    task.setName(n);
                if (null != t)
                    task.setType(t);
                if (StringUtils.isNotBlank(me))
                    task.setMethod(me);
                if (StringUtils.isNotBlank(tag))
                    task.setTag(tag);
                if (task.getType() == null)
                    task.setType(String.class);
            }
            if (log.isDebugEnabled())
                log.debug("线程:{}, 标记:{}, 类型:{}, 方法:{}, 时间:{}", task.getName(), task.getTag(), task.getType().getName(), task.getMethod(), format.format(task.getTime()));
        }
    }
}
