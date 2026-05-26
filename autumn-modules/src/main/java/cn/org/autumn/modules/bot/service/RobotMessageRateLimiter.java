package cn.org.autumn.modules.bot.service;

import cn.org.autumn.exception.CodeException;
import cn.org.autumn.model.RobotQuotaConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 机器人入站消息推送限流（优先 Redis 计数，无 Redis 时本机滑动窗口降级）。
 */
@Component
public class RobotMessageRateLimiter {

    private static final ConcurrentHashMap<String, Window> LOCAL_WINDOWS = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${autumn.redis.open:false}")
    private boolean redisOpen;

    @Autowired
    @Lazy
    private RobotQuotaService robotQuotaService;

    public void check(String robotUuid) throws CodeException {
        if (robotUuid == null)
            return;
        RobotQuotaConfig config = robotQuotaService.getGlobal();
        int maxPerMinute = config == null ? 60 : config.getMaxMessagePushPerMinute();
        if (maxPerMinute <= 0)
            return;
        String key = "bot:push:rate:" + robotUuid;
        if (redisOpen && redisTemplate != null) {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L)
                redisTemplate.expire(key, 60, TimeUnit.SECONDS);
            if (count != null && count > maxPerMinute)
                throw new CodeException("消息推送过于频繁，请稍后再试");
            return;
        }
        long now = System.currentTimeMillis();
        Window window = LOCAL_WINDOWS.computeIfAbsent(robotUuid, k -> new Window());
        synchronized (window) {
            if (now - window.windowStartMs > 60_000L) {
                window.windowStartMs = now;
                window.count = 0;
            }
            window.count++;
            if (window.count > maxPerMinute)
                throw new CodeException("消息推送过于频繁，请稍后再试");
        }
    }

    private static final class Window {
        private long windowStartMs = System.currentTimeMillis();
        private int count;
    }
}
