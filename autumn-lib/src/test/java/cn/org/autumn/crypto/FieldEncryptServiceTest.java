package cn.org.autumn.crypto;

import cn.org.autumn.annotation.FieldEncrypt;
import cn.org.autumn.config.FieldEncryptProperties;
import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.data.ColumnInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;

public class FieldEncryptServiceTest {

    private static final byte[] KEY = new byte[32];

    static {
        Arrays.fill(KEY, (byte) 9);
    }

    private FieldEncryptService service;

    @Before
    public void setUp() {
        FieldEncryptProperties properties = new FieldEncryptProperties();
        properties.setEnabled(true);
        properties.setKey(Base64.getEncoder().encodeToString(KEY));
        service = FieldEncryptTestSupport.newService(properties);
        service.registerEntity(SampleEntity.class);
    }

    @Test
    public void onWriteOnReadRoundTrip() {
        SampleEntity entity = new SampleEntity();
        entity.mobile = "13800138000";
        service.onWrite(entity);
        Assert.assertTrue(entity.mobile.startsWith("ENC$v1$"));
        service.onRead(entity);
        Assert.assertEquals("13800138000", entity.mobile);
    }

    @Test
    public void hashQueryValueForSearchableField() {
        String hash = service.hashQueryValue(SampleEntity.class, "mobile", "13800138000");
        Assert.assertNotNull(hash);
        Assert.assertEquals(64, hash.length());
    }

    @Test
    public void applyBeforeWriteSetsHash() throws IllegalAccessException {
        SampleEntity entity = new SampleEntity();
        entity.mobile = "13800138000";
        service.applyBeforeWrite(entity);
        Assert.assertTrue(entity.mobile.startsWith("ENC$v1$"));
        Assert.assertNotNull(entity.mobileHash);
        Assert.assertEquals(64, entity.mobileHash.length());
    }

    @Test
    public void applyAfterReadRestoresPlain() {
        SampleEntity entity = new SampleEntity();
        entity.mobile = "13800138000";
        service.applyBeforeWrite(entity);
        service.applyAfterRead(entity);
        Assert.assertEquals("13800138000", entity.mobile);
    }

    @Test
    public void columnInfoExpandsLength() {
        try {
            Field field = SampleEntity.class.getDeclaredField("mobile");
            ColumnInfo info = new ColumnInfo(field);
            Assert.assertTrue(info.getLength() >= 1024);
            Assert.assertTrue(info.getGenAnnotation().contains("@FieldEncrypt"));
        } catch (NoSuchFieldException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void decryptWhenWriteDisabled() {
        FieldEncryptProperties properties = new FieldEncryptProperties();
        properties.setEnabled(false);
        properties.setKey(Base64.getEncoder().encodeToString(KEY));
        FieldEncryptService offService = FieldEncryptTestSupport.newService(properties);
        String cipher = FieldCrypto.encrypt("secret", KEY, "ENC$v1$", "");
        Assert.assertEquals("secret", offService.decryptValue(cipher));
        Assert.assertFalse(offService.isWriteEncryptEnabled());
        Assert.assertTrue(offService.isReadDecryptEnabled());
        SampleEntity entity = new SampleEntity();
        entity.mobile = cipher;
        offService.registerEntity(SampleEntity.class);
        offService.applyAfterRead(entity);
        Assert.assertEquals("secret", entity.mobile);
        offService.applyBeforeWrite(entity);
        Assert.assertEquals("secret", entity.mobile);
    }

    @Test
    public void runtimeOverrideWriteSwitch() throws IllegalAccessException {
        FieldEncryptProperties properties = new FieldEncryptProperties();
        properties.setEnabled(false);
        properties.setKey(Base64.getEncoder().encodeToString(KEY));
        FieldEncryptService svc = FieldEncryptTestSupport.newService(properties);
        svc.registerEntity(SampleEntity.class);
        Assert.assertFalse(svc.isConfigWriteEncryptEnabled());
        Assert.assertFalse(svc.isWriteEncryptEnabled());
        svc.setRuntimeWriteEncryptOverride(true);
        Assert.assertTrue(svc.isWriteEncryptEnabled());
        SampleEntity entity = new SampleEntity();
        entity.mobile = "13800138000";
        svc.applyBeforeWrite(entity);
        Assert.assertTrue(entity.mobile.startsWith("ENC$v1$"));
        svc.setRuntimeWriteEncryptOverride(false);
        Assert.assertFalse(svc.isWriteEncryptEnabled());
        entity.mobile = "13800138000";
        svc.applyBeforeWrite(entity);
        Assert.assertEquals("13800138000", entity.mobile);
        String cipher = FieldCrypto.encrypt("x", KEY, "ENC$v1$", "");
        Assert.assertTrue(svc.needsMigration("plain"));
        Assert.assertFalse(svc.needsMigration(cipher));
        entity.mobile = "plain";
        svc.applyEncryptBeforePersist(entity);
        Assert.assertTrue(entity.mobile.startsWith("ENC$v1$"));
    }

    @Test
    public void testDecryptCipher() {
        String cipher = FieldCrypto.encrypt("secret-value", KEY, "ENC$v1$", "");
        Map<String, Object> ok = service.testDecrypt(cipher);
        Assert.assertEquals(Boolean.TRUE, ok.get("success"));
        Assert.assertEquals("secret-value", ok.get("plain"));
        Map<String, Object> bad = service.testDecrypt("not-a-cipher");
        Assert.assertTrue(bad.containsKey("error"));
    }

    @Test
    public void applyExternalKeysOverridesEnv() {
        FieldEncryptProperties properties = new FieldEncryptProperties();
        properties.setEnabled(false);
        properties.setKey(Base64.getEncoder().encodeToString(KEY));
        FieldEncryptService svc = FieldEncryptTestSupport.newService(properties);
        byte[] otherKey = new byte[32];
        Arrays.fill(otherKey, (byte) 2);
        String otherB64 = Base64.getEncoder().encodeToString(otherKey);
        svc.applyExternalKeys(otherB64, otherB64, FieldEncryptService.SOURCE_REDIS);
        Assert.assertEquals(FieldEncryptService.SOURCE_REDIS, svc.getKeySource());
        String cipher = FieldCrypto.encrypt("cluster", otherKey, "ENC$v1$", "");
        Assert.assertEquals("cluster", svc.decryptValue(cipher));
    }

    @Test
    public void reloadKeysFromEnvironmentRestoresEnvKeys() {
        FieldEncryptProperties properties = new FieldEncryptProperties();
        properties.setEnabled(false);
        properties.setKey(Base64.getEncoder().encodeToString(KEY));
        FieldEncryptService svc = FieldEncryptTestSupport.newService(properties);
        byte[] otherKey = new byte[32];
        Arrays.fill(otherKey, (byte) 4);
        svc.applyExternalKeys(Base64.getEncoder().encodeToString(otherKey), Base64.getEncoder().encodeToString(otherKey), FieldEncryptService.SOURCE_REDIS);
        Assert.assertEquals(FieldEncryptService.SOURCE_REDIS, svc.getKeySource());
        svc.reloadKeysFromEnvironment();
        Assert.assertEquals(FieldEncryptService.SOURCE_ENV, svc.getKeySource());
        String cipher = FieldCrypto.encrypt("env-key", KEY, "ENC$v1$", "");
        Assert.assertEquals("env-key", svc.decryptValue(cipher));
    }

    public static class SampleEntity {
        @Column(comment = "手机号", length = 255)
        @FieldEncrypt(searchable = true)
        public String mobile;

        @Column(comment = "手机号哈希", length = 64)
        public String mobileHash;
    }
}
