package cn.org.autumn.integration.support;

import org.junit.jupiter.api.Assumptions;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * 集成测试前置假设（如 Redis 不可用则跳过，避免 CI 误报）。
 */
public final class IntegrationAssumptions {

    private IntegrationAssumptions() {
    }

    public static void requireRedis(RedisConnectionFactory factory) {
        Assumptions.assumeTrue(factory != null, "跳过：未配置 RedisConnectionFactory");
        RedisConnection connection = null;
        try {
            connection = factory.getConnection();
            connection.ping();
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "跳过：Redis 不可达（请启动 Redis 或设置 AUTUMN_IT_REDIS_*）: " + e.getMessage());
        } finally {
            if (connection != null)
                connection.close();
        }
    }
}
