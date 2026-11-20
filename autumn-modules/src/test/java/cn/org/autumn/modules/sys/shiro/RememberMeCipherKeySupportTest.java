package cn.org.autumn.modules.sys.shiro;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

public class RememberMeCipherKeySupportTest {

    @TempDir
    Path temp;

    @Test
    void rejectsLegacyAndPersistsRandom() {
        Path keyFile = temp.resolve("remember-me.key");
        byte[] a = RememberMeCipherKeySupport.resolveCipherKey(
                RememberMeCipherKeySupport.FORBIDDEN_LEGACY_BASE64, keyFile.toString());
        assertEquals(16, a.length);
        assertTrue(Files.isRegularFile(keyFile));
        byte[] b = RememberMeCipherKeySupport.resolveCipherKey("", keyFile.toString());
        assertArrayEquals(a, b);
    }

    @Test
    void usesConfiguredKey() {
        byte[] raw = new byte[16];
        for (int i = 0; i < 16; i++) raw[i] = (byte) i;
        String b64 = Base64.getEncoder().encodeToString(raw);
        byte[] decoded = RememberMeCipherKeySupport.resolveCipherKey(b64, temp.resolve("x.key").toString());
        assertArrayEquals(raw, decoded);
    }

    @Test
    void resolveKeyFileUnderNodeHome() {
        Path home = temp.resolve("node-home");
        Path key = RememberMeCipherKeySupport.resolveKeyFile("", home);
        assertEquals(home.resolve("security/remember-me.key").toAbsolutePath().normalize(), key);
    }

    @Test
    void selfHealRegeneratesWhenFileDeleted() throws Exception {
        Path keyFile = temp.resolve("security").resolve("remember-me.key");
        byte[] a = RememberMeCipherKeySupport.resolveCipherKey("", keyFile, null, "default");
        assertTrue(Files.isRegularFile(keyFile));
        Files.delete(keyFile);
        byte[] b = RememberMeCipherKeySupport.resolveCipherKey("", keyFile, null, "default");
        assertTrue(Files.isRegularFile(keyFile));
        assertEquals(16, b.length);
        // 无 Redis 时删除文件 = 新钥，与旧钥不同（旧 Cookie 失效属预期）
        assertFalse(Arrays.equals(a, b));
    }

    @Test
    void configuredKeyWritesLocalFile() throws Exception {
        byte[] raw = new byte[16];
        for (int i = 0; i < 16; i++) raw[i] = (byte) (i + 1);
        String b64 = Base64.getEncoder().encodeToString(raw);
        Path keyFile = temp.resolve("security").resolve("remember-me.key");
        byte[] decoded = RememberMeCipherKeySupport.resolveCipherKey(b64, keyFile, null, "default");
        assertArrayEquals(raw, decoded);
        assertEquals(b64, Files.readString(keyFile, StandardCharsets.UTF_8).trim());
    }

    @Test
    void redisWinsWhenConfiguredDiffers() throws Exception {
        byte[] redisRaw = new byte[16];
        for (int i = 0; i < 16; i++) {
            redisRaw[i] = (byte) (10 + i);
        }
        byte[] envRaw = new byte[16];
        for (int i = 0; i < 16; i++) {
            envRaw[i] = (byte) (20 + i);
        }
        String redisB64 = Base64.getEncoder().encodeToString(redisRaw);
        String envB64 = Base64.getEncoder().encodeToString(envRaw);
        StringRedisTemplate redis =
                Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops =
                Mockito.mock(ValueOperations.class);
        Mockito.when(redis.opsForValue()).thenReturn(ops);
        Mockito.when(ops.get(ArgumentMatchers.anyString())).thenReturn(redisB64);

        Path keyFile = temp.resolve("security").resolve("conflict.key");
        byte[] decoded = RememberMeCipherKeySupport.resolveCipherKey(envB64, keyFile, redis, "default");
        assertArrayEquals(redisRaw, decoded);
        assertEquals(redisB64, Files.readString(keyFile, StandardCharsets.UTF_8).trim());
    }
}
