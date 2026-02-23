package cn.org.autumn.utils;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Workaround for Redisson StackOverflowError on RedisTemplate.expire().
 * RedissonConnection.keyCommands() and DefaultedRedisConnection.pExpire()
 * form an infinite recursion. This utility bypasses it via Lua script.
 */
public class RedisExpireUtil {

    private static final DefaultRedisScript<Long> EXPIRE_SCRIPT = new DefaultRedisScript<>(
            "return redis.call('expire', KEYS[1], ARGV[1])", Long.class);

    private static final StringRedisSerializer STRING_SERIALIZER = new StringRedisSerializer();

    @SuppressWarnings("unchecked")
    public static boolean expire(RedisTemplate redisTemplate, String key, long timeout, TimeUnit unit) {
        long seconds = unit.toSeconds(timeout);
        Long result = (Long) redisTemplate.execute(EXPIRE_SCRIPT, STRING_SERIALIZER, STRING_SERIALIZER, Collections.singletonList(key), String.valueOf(seconds));
        return result > 0;
    }
}