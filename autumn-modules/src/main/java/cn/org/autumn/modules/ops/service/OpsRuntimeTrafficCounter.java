package cn.org.autumn.modules.ops.service;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import org.springframework.stereotype.Component;

/**
 * 进程级简易请求计数：总量 + 近 60 秒滑动窗口近似 QPS。
 * 由 wall 入口轻量递增，供运行快照读取。
 */
@Component
public class OpsRuntimeTrafficCounter {

    private static final int WINDOW_SECONDS = 60;

    private final AtomicLong total = new AtomicLong();
    private final LongAdder[] buckets = new LongAdder[WINDOW_SECONDS];
    private volatile long lastSecond = currentSecond();

    public OpsRuntimeTrafficCounter() {
        for (int i = 0; i < WINDOW_SECONDS; i++) {
            buckets[i] = new LongAdder();
        }
    }

    public void record() {
        total.incrementAndGet();
        long sec = currentSecond();
        rotateIfNeeded(sec);
        buckets[(int) (sec % WINDOW_SECONDS)].increment();
    }

    public long getTotal() {
        return total.get();
    }

    /** 近 60 秒请求数 / 60，近似 QPS。 */
    public double getApproxQps() {
        long sec = currentSecond();
        rotateIfNeeded(sec);
        long sum = 0;
        for (LongAdder b : buckets) {
            sum += b.sum();
        }
        return sum / (double) WINDOW_SECONDS;
    }

    private synchronized void rotateIfNeeded(long sec) {
        long prev = lastSecond;
        if (sec == prev) {
            return;
        }
        long gap = sec - prev;
        if (gap >= WINDOW_SECONDS) {
            for (LongAdder b : buckets) {
                b.reset();
            }
        } else {
            for (long s = prev + 1; s <= sec; s++) {
                buckets[(int) (s % WINDOW_SECONDS)].reset();
            }
        }
        lastSecond = sec;
    }

    private static long currentSecond() {
        return System.currentTimeMillis() / 1000L;
    }
}
