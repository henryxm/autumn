package cn.org.autumn.utils;

import com.alibaba.fastjson2.JSON;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

import java.util.concurrent.TimeUnit;

/**
 * Redis工具类
 */
@Component
public class RedisUtils {
    @Autowired
    private RedisTemplate redisTemplate;

    @Resource(name = "redisTemplate")
    private ValueOperations<String, Object> valueOperations;
    @Resource(name = "redisTemplate")
    private HashOperations<String, String, Object> hashOperations;
    @Resource(name = "redisTemplate")
    private ListOperations<String, Object> listOperations;
    @Resource(name = "redisTemplate")
    private SetOperations<String, Object> setOperations;
    @Resource(name = "redisTemplate")
    private ZSetOperations<String, Object> zSetOperations;
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
        if (open) {
            valueOperations.set(key, value);
            if (expire != NOT_EXPIRE) {
                RedisExpireUtil.expire(redisTemplate, key, expire, TimeUnit.SECONDS);
            }
        }
    }

    public void set(String key, Object value) {
        if (open)
            set(key, value, DEFAULT_EXPIRE);
    }

    public Object get(String key) {
        if (open)
            return valueOperations.get(key);
        return null;
    }

    public void delete(String key) {
        if (open)
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
}
