package cn.org.autumn.utils;

import com.alibaba.fastjson2.JSON;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Redis工具类。热路径 GET 前用 STRLEN 护栏，反序列化失败或堆压力时删键回空，避免大值 JDK 反序列化打满堆。
 */
@Slf4j
@Component
public class RedisUtils {
    @Autowired(required = false)
    private RedisTemplate redisTemplate;
    /**
     * 默认过期时长，单位：秒
     */
    public final static long DEFAULT_EXPIRE = 60 * 60 * 24;
    /**
     * 不设置过期时长
     */
    public final static long NOT_EXPIRE = -1;

    /** STRING 值字节上限；超限跳过 GET 并删键。默认 1MB。 */
    public final static long DEFAULT_MAX_VALUE_BYTES = 1048576L;

    @Getter
    @Value("${autumn.redis.open: false}")
    private boolean open;

    @Getter
    @Value("${autumn.redis.max-value-bytes:1048576}")
    private long maxValueBytes = DEFAULT_MAX_VALUE_BYTES;

    public void set(String key, Object value, long expire) {
        if (!open || redisTemplate == null) {
            return;
        }
        if (isOversizedPlainValue(value)) {
            log.warn("Redis set skipped oversized value key={} limitBytes={}", key, maxValueBytes);
            return;
        }
        redisTemplate.opsForValue().set(key, value);
        if (expire != NOT_EXPIRE) {
            RedisExpireUtil.expire(redisTemplate, key, expire, TimeUnit.SECONDS);
        }
    }

    public void set(String key, Object value) {
        if (open && redisTemplate != null)
            set(key, value, DEFAULT_EXPIRE);
    }

    public Object get(String key) {
        if (!open || redisTemplate == null) {
            return null;
        }
        try {
            Long len = stringLength(key);
            if (len != null && maxValueBytes > 0 && len > maxValueBytes) {
                log.warn("Redis get skipped oversized key={} strlen={} limitBytes={}", key, len, maxValueBytes);
                delete(key);
                return null;
            }
            return redisTemplate.opsForValue().get(key);
        } catch (OutOfMemoryError e) {
            log.warn("Redis get OOM key={} msg={}", key, e.getMessage());
            safeDelete(key);
            return null;
        } catch (Exception e) {
            if (!isRedisDeserializationFailure(e)) {
                throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
            }
            log.warn("Redis get deserialize fail key={} msg={}", key, e.getMessage());
            safeDelete(key);
            return null;
        }
    }

    public void delete(String key) {
        if (open && redisTemplate != null)
            redisTemplate.delete(key);
    }

    /**
     * Object转成JSON数据
     */
    private String toJson(Object object) {
        if (object instanceof Integer || object instanceof Long || object instanceof Float ||
                object instanceof Double || object instanceof Boolean || object instanceof String) {
            return String.valueOf(object);
        }
        return JSON.toJSONString(object);
    }

    /**
     * JSON数据，转成Object
     */
    private <T> T fromJson(String json, Class<T> clazz) {
        return JSON.parseObject(json, clazz);
    }

    boolean isOversizedPlainValue(Object value) {
        if (maxValueBytes <= 0 || value == null) {
            return false;
        }
        if (value instanceof String) {
            return ((String) value).getBytes(StandardCharsets.UTF_8).length > maxValueBytes;
        }
        if (value instanceof byte[]) {
            return ((byte[]) value).length > maxValueBytes;
        }
        return false;
    }

    Long stringLength(String key) {
        RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
        if (factory == null) {
            return null;
        }
        try (RedisConnection conn = factory.getConnection()) {
            return conn.stringCommands().strLen(key.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.debug("Redis STRLEN failed key={} msg={}", key, e.getMessage());
            return null;
        }
    }

    private void safeDelete(String key) {
        try {
            delete(key);
        } catch (Throwable ignored) {
            // ignore purge failure under heap pressure
        }
    }

    static boolean isRedisDeserializationFailure(Throwable e) {
        for (Throwable x = e; x != null; x = x.getCause()) {
            if (x instanceof SerializationException || x instanceof OutOfMemoryError) {
                return true;
            }
            String name = x.getClass().getName();
            if (name.contains("SerializationException") || name.contains("InvalidClassException")) {
                return true;
            }
            String msg = x.getMessage();
            if (msg != null && (msg.contains("Cannot deserialize") || msg.contains("Java heap space"))) {
                return true;
            }
        }
        return false;
    }
}
