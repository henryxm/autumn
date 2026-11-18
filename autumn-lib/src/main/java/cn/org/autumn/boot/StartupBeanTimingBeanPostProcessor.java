package cn.org.autumn.boot;

import lombok.extern.slf4j.Slf4j;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.Environment;
import org.springframework.lang.Nullable;

/**
 * 统计 Bean 初始化「自身耗时」（扣除嵌套依赖创建），在上下文刷新完成后输出 Top-N。
 * <p>
 * 配置：{@code autumn.startup.bean-timing.enabled}（默认 true）、
 * {@code autumn.startup.bean-timing.threshold-ms}（默认 50）、
 * {@code autumn.startup.bean-timing.top}（默认 25）。
 */
@Slf4j
public class StartupBeanTimingBeanPostProcessor implements BeanPostProcessor, ApplicationListener<ContextRefreshedEvent>,
        PriorityOrdered {

    private final Environment environment;
    private final ConcurrentHashMap<String, Long> startNs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> selfNs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> childNs = new ConcurrentHashMap<>();
    private final ThreadLocal<Deque<String>> stack = ThreadLocal.withInitial(ArrayDeque::new);
    private final AtomicBoolean reported = new AtomicBoolean(false);

    public StartupBeanTimingBeanPostProcessor(Environment environment) {
        this.environment = environment;
    }

    private boolean enabled() {
        return Boolean.parseBoolean(environment.getProperty("autumn.startup.bean-timing.enabled", "true"));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (!enabled()) {
            return bean;
        }
        long now = System.nanoTime();
        Deque<String> s = stack.get();
        s.push(beanName);
        startNs.put(beanName, now);
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!enabled()) {
            return bean;
        }
        Long start = startNs.remove(beanName);
        if (start == null) {
            return bean;
        }
        long total = System.nanoTime() - start;
        Long children = childNs.remove(beanName);
        long self = total - (children == null ? 0L : children);
        if (self < 0L) {
            self = 0L;
        }
        selfNs.merge(beanName, self, Long::sum);
        Deque<String> s = stack.get();
        if (!s.isEmpty() && beanName.equals(s.peek())) {
            s.pop();
        }
        if (!s.isEmpty()) {
            childNs.merge(s.peek(), total, Long::sum);
        }
        return bean;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (event.getApplicationContext().getParent() != null) {
            return;
        }
        if (!enabled() || !reported.compareAndSet(false, true)) {
            return;
        }
        report();
    }

    private void report() {
        long thresholdMs = parseLong(environment.getProperty("autumn.startup.bean-timing.threshold-ms"), 50L);
        int top = (int) parseLong(environment.getProperty("autumn.startup.bean-timing.top"), 25L);
        List<Map.Entry<String, Long>> rows = new ArrayList<>(selfNs.entrySet());
        rows.sort(Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue).reversed());
        int shown = 0;
        long sumMs = 0L;
        StringBuilder sb = new StringBuilder(512);
        for (Map.Entry<String, Long> e : rows) {
            long ms = e.getValue() / 1_000_000L;
            sumMs += ms;
            if (ms < thresholdMs || shown >= top) {
                continue;
            }
            if (shown > 0) {
                sb.append("; ");
            }
            sb.append(e.getKey()).append('=').append(ms).append("ms");
            shown++;
        }
        if (shown == 0) {
            log.debug("Startup bean timing: none above thresholdMs={} (beans={} selfSumMs={})", thresholdMs, rows.size(), sumMs);
            return;
        }
        log.debug("Startup bean timing top{}(>= {}ms): {} | beans={} selfSumMs={}", shown, thresholdMs, sb, rows.size(), sumMs);
    }

    private static long parseLong(@Nullable String raw, long def) {
        if (raw == null || raw.isBlank()) {
            return def;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
