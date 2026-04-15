package cn.org.autumn.redis.resilience;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Redis 韧性运行时配置（熔断、分布式锁与 Redis 操作门控）。
 * <p>
 * 配置前缀：{@code autumn.redis.resilience.*}，见 {@code docs/REDIS_RESILIENCE.md}。
 */
@Getter
@Component
@ConfigurationProperties(prefix = "autumn.redis.resilience")
public class AutumnRedisResilienceProperties {

    /**
     * 总开关；为 false 时不做熔断门控（仍受 {@code autumn.redis.open} 等控制是否装配 Redis 栈）。
     */
    @Setter
    private boolean enabled = true;

    private Circuit circuit = new Circuit();

    private Lock lock = new Lock();

    public void setCircuit(Circuit circuit) {
        if (circuit != null) {
            this.circuit = circuit;
        }
    }

    public void setLock(Lock lock) {
        if (lock != null) {
            this.lock = lock;
        }
    }

    /**
     * 滑动计数式熔断（OPEN 时拒绝 Redis/Redisson 调用，冷却结束后进入 HALF_OPEN 试探）。
     */
    @Getter
    @Setter
    public static class Circuit {

        /**
         * 连续基础设施类失败多少次进入 OPEN。
         */
        private int failureThreshold = 5;

        /**
         * HALF_OPEN 阶段连续成功多少次恢复 CLOSED（分布式锁一次完整 acquire+release 记 1 次成功）。
         */
        private int halfOpenSuccessThreshold = 1;

        /**
         * OPEN 状态持续时长（毫秒），之后进入 HALF_OPEN。
         */
        private long openWaitMs = 30000L;
    }

    /**
     * 分布式锁与 Redisson 相关策略。
     */
    @Getter
    @Setter
    public static class Lock {
        /**
         * 熔断为 OPEN 时是否跳过 Redisson tryLock，直接本地执行（推荐 true，避免雪崩时线程堆积在 Redis）。
         */
        private boolean skipRedissonWhenCircuitOpen = true;
    }
}
