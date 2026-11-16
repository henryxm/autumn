package cn.org.autumn.modules.sys.redis;

import cn.org.autumn.crypto.FieldEncryptConfigSource;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.utils.RedisUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 集群启动 bootstrap：env=false 不冲掉一键开启的 write-enabled=true。
 */
class FieldEncryptRedisStoreBootstrapTest {

    private FieldEncryptRedisStore store;
    private Map<String, String> redis;
    private FieldEncryptConfigSource.Resolved resolved;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = new HashMap<>();
        store = new FieldEncryptRedisStore();

        RedisUtils redisUtils = mock(RedisUtils.class);
        when(redisUtils.isOpen()).thenReturn(true);

        StringRedisTemplate template = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(ops);
        when(ops.get(anyString())).thenAnswer(inv -> redis.get(inv.getArgument(0)));
        org.mockito.Mockito.doAnswer(inv -> {
            redis.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(ops).set(anyString(), anyString());

        SysConfigService sysConfigService = mock(SysConfigService.class);
        when(sysConfigService.getNameSpace()).thenReturn("testns");

        FieldEncryptConfigSource configSource = mock(FieldEncryptConfigSource.class);
        resolved = mock(FieldEncryptConfigSource.Resolved.class);
        when(configSource.resolveFromEnvironment()).thenReturn(resolved);
        when(resolved.getKeyBase64()).thenReturn(null);
        when(resolved.getHashKeyBase64()).thenReturn(null);
        when(configSource.validKeyBase64(org.mockito.ArgumentMatchers.any())).thenReturn(null);
        when(configSource.validHashBase64(org.mockito.ArgumentMatchers.any())).thenReturn(null);

        ReflectionTestUtils.setField(store, "redisUtils", redisUtils);
        ReflectionTestUtils.setField(store, "stringRedisTemplate", template);
        ReflectionTestUtils.setField(store, "sysConfigService", sysConfigService);
        ReflectionTestUtils.setField(store, "configSource", configSource);
    }

    @Test
    void envFalse_keepsExistingTrue() {
        when(resolved.isConfigWriteEnabled()).thenReturn(false);
        redis.put("autumn:testns:field-encrypt:write-enabled", "true");
        // RedisKeys pattern — use whatever redisKey produces
        String writeKey = (String) ReflectionTestUtils.invokeMethod(store, "redisKey", "write-enabled");
        redis.clear();
        redis.put(writeKey, "true");

        store.syncBootstrapFromEnvironment();
        assertEquals("true", redis.get(writeKey));
    }

    @Test
    void envTrue_forceOpensFalse() {
        when(resolved.isConfigWriteEnabled()).thenReturn(true);
        String writeKey = (String) ReflectionTestUtils.invokeMethod(store, "redisKey", "write-enabled");
        redis.put(writeKey, "false");

        store.syncBootstrapFromEnvironment();
        assertEquals("true", redis.get(writeKey));
    }

    @Test
    void missingKey_seededFromEnvFalse() {
        when(resolved.isConfigWriteEnabled()).thenReturn(false);
        String writeKey = (String) ReflectionTestUtils.invokeMethod(store, "redisKey", "write-enabled");
        assertNull(redis.get(writeKey));

        store.syncBootstrapFromEnvironment();
        assertEquals("false", redis.get(writeKey));
    }

    @Test
    void existingRedisKey_notOverwrittenByDifferentEnvKey() {
        when(resolved.isConfigWriteEnabled()).thenReturn(false);
        String keySlot = (String) ReflectionTestUtils.invokeMethod(store, "redisKey", "key");
        String existing = "existing-redis-key-base64-value-32b=";
        String envKey = "different-env-key-base64-value-xxxx=";
        redis.put(keySlot, existing);
        when(resolved.getKeyBase64()).thenReturn(envKey);
        FieldEncryptConfigSource configSource =
                (FieldEncryptConfigSource) ReflectionTestUtils.getField(store, "configSource");
        when(configSource.validKeyBase64(envKey)).thenReturn(envKey);
        when(configSource.validKeyBase64(existing)).thenReturn(existing);

        store.syncBootstrapFromEnvironment();
        assertEquals(existing, redis.get(keySlot));
    }
}
