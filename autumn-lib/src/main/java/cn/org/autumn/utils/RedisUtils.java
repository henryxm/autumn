package cn.org.autumn.utils;

import java.util.concurrent.TimeUnit;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis工具类
 */
@Component
public class RedisUtils {
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    /**
     * 默认过期时长，单位：秒
     */
    public final static long DEFAULT_EXPIRE = 60 * 60 * 24;
    /**
     * 不设置过期时长
     */
    public final static long NOT_EXPIRE = -1;

    @Getter
    @Value("${autumn.redis.open: false}")
    private boolean open;

    public void set(String key, Object value, long expire) {
        if (open && redisTemplate != null) {
            redisTemplate.opsForValue().set(key, value);
            if (expire != NOT_EXPIRE) {
                redisTemplate.expire(key, expire, TimeUnit.SECONDS);
            }
        }
    }

    public void set(String key, Object value) {
        if (open && redisTemplate != null)
            set(key, value, DEFAULT_EXPIRE);
    }

    public Object get(String key) {
        if (open && redisTemplate != null)
            return redisTemplate.opsForValue().get(key);
        return null;
    }

    public void delete(String key) {
        if (open && redisTemplate != null)
            redisTemplate.delete(key);
    }
}
