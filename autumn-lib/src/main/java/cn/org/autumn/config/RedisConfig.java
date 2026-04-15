package cn.org.autumn.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 模板与 Ops Bean。
 * <p>
 * {@link RedisConnectionFactory} 使用 {@code @Autowired(required = false)}，由 Spring Boot 在
 * {@code autumn.redis.open=true} 且 Redis 自动配置未被排除时注入；本类<strong>不</strong>使用
 * {@code @ConditionalOnBean(RedisConnectionFactory)}、<strong>不</strong>使用 {@code @AutoConfigureAfter}，
 * 以便 {@link #redisTemplate()} 等 {@link Bean} 在刷新阶段正常注册与执行。
 */
@Configuration
public class RedisConfig {

    @Autowired
    ObjectProvider<RedisConnectionFactory> provider;

    /**
     * 默认 Redis 模板（String 键 + JDK 值序列化）；标为 {@link Primary}，避免与同类型的
     * {@link #jackson2JsonRedisSerializerTemplate()} 并存时按类型注入出现歧义。
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate() {
        RedisConnectionFactory factory = provider.getIfAvailable();
        if (null == factory)
            return null;
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
        redisTemplate.setConnectionFactory(factory);
        return redisTemplate;
    }

    @Bean
    public RedisTemplate<String, Object> jackson2JsonRedisSerializerTemplate() {
        RedisConnectionFactory factory = provider.getIfAvailable();
        if (null == factory)
            return null;
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<Object>(Object.class));
        redisTemplate.setConnectionFactory(factory);
        return redisTemplate;
    }
}
