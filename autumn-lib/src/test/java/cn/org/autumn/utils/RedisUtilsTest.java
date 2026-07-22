package cn.org.autumn.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisUtilsTest {

    private RedisUtils redisUtils;
    private RedisTemplate redisTemplate;
    private ValueOperations valueOperations;
    private RedisConnectionFactory connectionFactory;
    private RedisConnection connection;
    private RedisStringCommands stringCommands;

    @BeforeEach
    @SuppressWarnings({"rawtypes", "unchecked"})
    void setUp() {
        redisUtils = new RedisUtils();
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        connectionFactory = mock(RedisConnectionFactory.class);
        connection = mock(RedisConnection.class);
        stringCommands = mock(RedisStringCommands.class);
        ReflectionTestUtils.setField(redisUtils, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(redisUtils, "open", true);
        ReflectionTestUtils.setField(redisUtils, "maxValueBytes", 1024L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.stringCommands()).thenReturn(stringCommands);
    }

    @Test
    void get_skipsOversizedAndDeletes() {
        when(stringCommands.strLen(any(byte[].class))).thenReturn(2048L);

        assertNull(redisUtils.get("big-key"));

        verify(valueOperations, never()).get(any());
        verify(redisTemplate).delete("big-key");
    }

    @Test
    void get_returnsValueWhenUnderLimit() {
        when(stringCommands.strLen(any(byte[].class))).thenReturn(10L);
        when(valueOperations.get("k")).thenReturn("ok");

        assertEquals("ok", redisUtils.get("k"));
        verify(redisTemplate, never()).delete(eq("k"));
    }

    @Test
    void get_purgesOnDeserializationFailure() {
        when(stringCommands.strLen(any(byte[].class))).thenReturn(10L);
        when(valueOperations.get("bad")).thenThrow(new SerializationException("Cannot deserialize"));

        assertNull(redisUtils.get("bad"));
        verify(redisTemplate).delete("bad");
    }

    @Test
    void set_skipsOversizedString() {
        String big = "x".repeat(2000);
        redisUtils.set("s", big, 60);
        verify(valueOperations, never()).set(any(), any());
    }

    @Test
    void isRedisDeserializationFailure_detectsHeapAndSerialize() {
        assertTrue(RedisUtils.isRedisDeserializationFailure(new SerializationException("Cannot deserialize")));
        assertTrue(RedisUtils.isRedisDeserializationFailure(new RuntimeException(new OutOfMemoryError("Java heap space"))));
        assertFalse(RedisUtils.isRedisDeserializationFailure(new IllegalStateException("other")));
    }
}
