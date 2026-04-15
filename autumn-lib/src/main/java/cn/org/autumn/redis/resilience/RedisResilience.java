package cn.org.autumn.redis.resilience;

import cn.org.autumn.install.InstallMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Redis / Redisson 韧性统一入口：熔断、单机降级门控、可组合业务降级。
 * <p>
 * <b>开发者用法</b>（任选其一，按场景组合）：
 * <ul>
 *   <li>分布式锁：优先使用 {@link cn.org.autumn.service.DistributedLockService}（已内置熔断与连接失败降级）。</li>
 *   <li>自定义访问 Redis：将「可能失败」的代码包在 {@link #execute(Callable, Supplier)} / {@link #executeUnchecked(Callable, Supplier)} 中。</li>
 *   <li>仅判断当前是否应走分布式锁：{@link #allowDistributedLock()}（{@link cn.org.autumn.thread.DistributedLockHelper} 已调用）。</li>
 * </ul>
 * <p>
 * 熔断仅统计<b>基础设施类</b>异常（超时、断连、网络不可达等），业务异常透传且不打开熔断。
 */
@Slf4j
@Component
public class RedisResilience {

    private final Environment environment;
    private final AutumnRedisResilienceProperties properties;
    private final ObjectProvider<RedisConnectionFactory> connectionFactoryProvider;

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicInteger halfOpenSuccesses = new AtomicInteger(0);
    private volatile long openUntilMillis;

    public RedisResilience(Environment environment,
                           AutumnRedisResilienceProperties properties,
                           ObjectProvider<RedisConnectionFactory> connectionFactoryProvider) {
        this.environment = environment;
        this.properties = properties;
        this.connectionFactoryProvider = connectionFactoryProvider;
    }

    /**
     * 是否允许尝试 Redisson 分布式锁（熔断 OPEN 且配置为跳过时返回 false，直接走本地逻辑）。
     */
    public boolean allowDistributedLock() {
        if (!resilienceGloballyActive()) {
            return true;
        }
        if (InstallMode.isActive(environment)) {
            return false;
        }
        if (!properties.getLock().isSkipRedissonWhenCircuitOpen()) {
            return true;
        }
        State s = state.get();
        if (s == State.CLOSED || s == State.HALF_OPEN) {
            return true;
        }
        if (System.currentTimeMillis() >= openUntilMillis) {
            tryHalfOpen();
            return true;
        }
        if (log.isDebugEnabled()) {
            log.debug("redis circuit OPEN, skip distributed lock until cooldown ends");
        }
        return false;
    }

    /**
     * 在熔断允许时执行 Redis 操作；失败时返回降级结果。
     *
     * @param redisAction 访问 Redis / Redisson 的代码
     * @param fallback    熔断打开或基础设施失败时的降级
     */
    public <T> T execute(Callable<T> redisAction, Supplier<T> fallback) throws Exception {
        if (redisAction == null) {
            return null;
        }
        if (!resilienceGloballyActive()) {
            return redisAction.call();
        }
        if (!allowProbe()) {
            return fallback == null ? null : fallback.get();
        }
        try {
            T v = redisAction.call();
            recordSuccess();
            return v;
        } catch (Exception e) {
            if (isInfrastructureFailure(e)) {
                recordFailure();
                if (log.isDebugEnabled()) {
                    log.debug("redis resilience: infrastructure failure, use fallback: {}", e.toString());
                }
            }
            if (fallback != null) {
                return fallback.get();
            }
            throw e;
        }
    }

    /**
     * 同 {@link #execute(Callable, Supplier)}，任意异常由降级兜底（降级再抛则包装为 {@link RuntimeException}）。
     */
    public <T> T executeUnchecked(Callable<T> redisAction, Supplier<T> fallback) {
        try {
            return execute(redisAction, fallback);
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new IllegalStateException("redis resilience execute failed", e);
        }
    }

    /**
     * 轻量探测 Redis 连接（短超时 ping），成功则重置失败计数；失败记一次基础设施失败。
     */
    public boolean probeConnectionQuick() {
        if (!resilienceGloballyActive()) {
            return true;
        }
        RedisConnectionFactory factory = connectionFactoryProvider.getIfAvailable();
        if (factory == null) {
            return false;
        }
        if (!allowProbe()) {
            return false;
        }
        try (RedisConnection conn = factory.getConnection()) {
            String pong = conn.ping();
            if ("PONG".equalsIgnoreCase(pong)) {
                recordSuccess();
                return true;
            }
        } catch (Exception e) {
            if (isInfrastructureFailure(e)) {
                recordFailure();
            }
            if (log.isDebugEnabled()) {
                log.debug("redis probe failed: {}", e.toString());
            }
        }
        return false;
    }

    public void recordSuccess() {
        if (!resilienceGloballyActive()) {
            return;
        }
        State s = state.get();
        if (s == State.HALF_OPEN) {
            int ok = halfOpenSuccesses.incrementAndGet();
            if (ok >= Math.max(1, properties.getCircuit().getHalfOpenSuccessThreshold())) {
                state.set(State.CLOSED);
                consecutiveFailures.set(0);
                halfOpenSuccesses.set(0);
                if (log.isDebugEnabled()) {
                    log.debug("redis circuit recovered to CLOSED");
                }
            }
            return;
        }
        consecutiveFailures.set(0);
    }

    public void recordFailure() {
        if (!resilienceGloballyActive()) {
            return;
        }
        State s = state.get();
        if (s == State.HALF_OPEN) {
            tripOpen();
            return;
        }
        int n = consecutiveFailures.incrementAndGet();
        if (n >= Math.max(1, properties.getCircuit().getFailureThreshold())) {
            tripOpen();
        }
    }

    public State getCircuitState() {
        return state.get();
    }

    private boolean resilienceGloballyActive() {
        return properties != null && properties.isEnabled();
    }

    private boolean allowProbe() {
        if (!resilienceGloballyActive()) {
            return true;
        }
        State s = state.get();
        if (s == State.OPEN) {
            if (System.currentTimeMillis() >= openUntilMillis) {
                tryHalfOpen();
                return true;
            }
            return false;
        }
        return true;
    }

    private void tryHalfOpen() {
        if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
            halfOpenSuccesses.set(0);
            if (log.isDebugEnabled()) {
                log.debug("redis circuit HALF_OPEN (probe)");
            }
        }
    }

    private void tripOpen() {
        long wait = Math.max(1000L, properties.getCircuit().getOpenWaitMs());
        openUntilMillis = System.currentTimeMillis() + wait;
        state.set(State.OPEN);
        consecutiveFailures.set(0);
        halfOpenSuccesses.set(0);
        log.warn("redis circuit OPEN for {} ms (infrastructure failures)", wait);
    }

    /**
     * 是否属于「基础设施」类失败（参与熔断）；避免业务 IllegalArgumentException 误伤熔断。
     */
    public static boolean isInfrastructureFailure(Throwable t) {
        Throwable c = t;
        int depth = 0;
        while (c != null && depth++ < 12) {
            if (c instanceof SocketTimeoutException
                    || c instanceof ConnectException
                    || c instanceof UnknownHostException
                    || c instanceof ClosedChannelException) {
                return true;
            }
            String name = c.getClass().getName();
            if (name.startsWith("org.redisson.client.")
                    || name.startsWith("redis.clients.jedis.")
                    || name.contains("RedisConnectionFailure")
                    || name.contains("QueryTimeoutException")) {
                return true;
            }
            if (c instanceof java.io.IOException && !(c instanceof java.io.FileNotFoundException)) {
                return true;
            }
            c = c.getCause();
        }
        return false;
    }

    public enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }
}
