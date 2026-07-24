package cn.org.autumn.thread;

import cn.org.autumn.config.Config;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

/**
 * {@link FunctionQueue} 静态门面，便于在定时任务、工具类中无需注入即可投递。
 * <p>
 * Spring 未就绪或 Bean 不可用时，调用会被拒绝并打 warn，不会抛异常。
 * </p>
 *
 * @see FunctionQueue
 */
@Slf4j
public final class FunctionQueues {

    private FunctionQueues() {
    }

    public static boolean offer(Runnable task) {
        FunctionQueue q = queue();
        if (q == null)
            return false;
        return q.offer(task);
    }

    public static boolean offer(String name, Runnable task) {
        FunctionQueue q = queue();
        if (q == null)
            return false;
        return q.offer(name, task);
    }

    public static <T> boolean offer(T arg, Consumer<T> action) {
        FunctionQueue q = queue();
        if (q == null)
            return false;
        return q.offer(arg, action);
    }

    public static <T> boolean offer(String name, T arg, Consumer<T> action) {
        FunctionQueue q = queue();
        if (q == null)
            return false;
        return q.offer(name, arg, action);
    }

    public static int size() {
        FunctionQueue q = queue();
        return q == null ? 0 : q.size();
    }

    public static boolean isEmpty() {
        FunctionQueue q = queue();
        return q == null || q.isEmpty();
    }

    private static FunctionQueue queue() {
        Object bean = Config.getBean(FunctionQueue.class);
        if (bean instanceof FunctionQueue)
            return (FunctionQueue) bean;
        log.warn("FunctionQueue bean unavailable, task rejected");
        return null;
    }
}
