package cn.org.autumn.crypto;

import cn.org.autumn.config.FieldEncryptProperties;
import cn.org.autumn.site.EncryptConfigFactory;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Assertions;

/**
 * docker-compose 短前缀 crypto.field.* 与 AUTUMN_CRYPTO_FIELD_* / AUTUMN_FIELD_ENCRYPT_* 别名解析。
 */
public class FieldEncryptConfigSourceAliasTest {

    private static final String KEY = FieldCrypto.generateRandomKeyBase64();
    private static final String HASH = FieldCrypto.generateRandomKeyBase64();

    @Test
    public void shortPrefixCryptoFieldEnabledAndKey() {
        FieldEncryptProperties properties = new FieldEncryptProperties();
        MockEnvironment env = new MockEnvironment();
        env.setProperty("crypto.field.enabled", "true");
        env.setProperty("crypto.field.key", KEY);
        env.setProperty("crypto.field.hash-key", HASH);

        FieldEncryptConfigSource.Resolved resolved = resolve(properties, env);
        assertTrue(resolved.isConfigWriteEnabled());
        assertEquals(KEY, resolved.getKeyBase64());
        assertEquals(HASH, resolved.getHashKeyBase64());
    }

    @Test
    public void autumnCryptoFieldEnvViaProperties() {
        FieldEncryptProperties properties = new FieldEncryptProperties();
        properties.setEnabled(true);
        properties.setKey(KEY);
        properties.setHashKey(HASH);

        FieldEncryptConfigSource.Resolved resolved = resolve(properties, new MockEnvironment());
        assertTrue(resolved.isConfigWriteEnabled());
        assertEquals(KEY, resolved.getKeyBase64());
        assertEquals(HASH, resolved.getHashKeyBase64());
    }

    @Test
    public void legacyAutumnFieldEncryptEnvNames() {
        FieldEncryptProperties properties = new FieldEncryptProperties();
        MockEnvironment env = new MockEnvironment();
        env.setProperty("AUTUMN_FIELD_ENCRYPT_ENABLED", "true");
        env.setProperty("AUTUMN_FIELD_ENCRYPT_KEY", KEY);
        env.setProperty("AUTUMN_FIELD_HASH_KEY", HASH);

        FieldEncryptConfigSource.Resolved resolved = resolve(properties, env);
        assertTrue(resolved.isConfigWriteEnabled());
        assertEquals(KEY, resolved.getKeyBase64());
        assertEquals(HASH, resolved.getHashKeyBase64());
    }

    @Test
    public void defaultDisabledWithoutAliases() {
        FieldEncryptConfigSource.Resolved resolved = resolve(new FieldEncryptProperties(), new MockEnvironment());
        assertFalse(resolved.isConfigWriteEnabled());
    }

    @Test
    public void shortPrefixOverridesPropertiesEnabled() {
        FieldEncryptProperties properties = new FieldEncryptProperties();
        properties.setEnabled(true);
        MockEnvironment env = new MockEnvironment();
        env.setProperty("crypto.field.enabled", "false");

        FieldEncryptConfigSource.Resolved resolved = resolve(properties, env);
        assertFalse(resolved.isConfigWriteEnabled());
    }

    @Test
    public void invalidKeyBase64Ignored() {
        FieldEncryptProperties properties = new FieldEncryptProperties();
        MockEnvironment env = new MockEnvironment();
        env.setProperty("crypto.field.key", "not-valid-base64!!!");

        FieldEncryptConfigSource source = source(properties, env);
        FieldEncryptConfigSource.Resolved resolved = source.resolveFromProperties();
        assertEquals("not-valid-base64!!!", resolved.getKeyBase64());
        Assertions.assertNull(source.validKeyBase64(resolved.getKeyBase64()));
    }

    private static FieldEncryptConfigSource.Resolved resolve(FieldEncryptProperties properties, MockEnvironment env) {
        return source(properties, env).resolveFromProperties();
    }

    private static FieldEncryptConfigSource source(FieldEncryptProperties properties, MockEnvironment env) {
        EncryptConfigFactory factory = new EncryptConfigFactory();
        ReflectionTestUtils.setField(factory, "handlers", Collections.emptyList());
        FieldEncryptConfigSource source = new FieldEncryptConfigSource();
        ReflectionTestUtils.setField(source, "properties", properties);
        ReflectionTestUtils.setField(source, "environment", env);
        ReflectionTestUtils.setField(source, "encryptConfigFactory", factory);
        return source;
    }
}
