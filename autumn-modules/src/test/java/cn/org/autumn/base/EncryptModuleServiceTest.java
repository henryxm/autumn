package cn.org.autumn.base;

import cn.org.autumn.annotation.FieldEncrypt;
import cn.org.autumn.config.FieldEncryptProperties;
import cn.org.autumn.crypto.FieldEncryptConfigSource;
import cn.org.autumn.crypto.FieldEncryptService;
import cn.org.autumn.site.EncryptConfigFactory;
import cn.org.autumn.table.annotation.Column;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;

public class EncryptModuleServiceTest {

    private static final byte[] KEY = new byte[32];

    static {
        Arrays.fill(KEY, (byte) 5);
    }

    private FieldEncryptService fieldEncryptService;
    private TestEncryptModuleService service;

    @Before
    public void setUp() {
        FieldEncryptProperties properties = new FieldEncryptProperties();
        properties.setEnabled(true);
        properties.setKey(Base64.getEncoder().encodeToString(KEY));
        fieldEncryptService = newFieldEncryptService(properties);
        fieldEncryptService.registerEntity(EncryptSampleEntity.class);
        service = new TestEncryptModuleService();
        ReflectionTestUtils.setField(service, "encrypt", fieldEncryptService);
    }

    @Test
    public void onReadDecryptsCipher() {
        EncryptSampleEntity entity = new EncryptSampleEntity();
        entity.secret = "plain-secret";
        fieldEncryptService.onWrite(entity);
        Assert.assertTrue(entity.secret.startsWith("ENC$v1$"));
        service.afterRead(entity);
        Assert.assertEquals("plain-secret", entity.secret);
    }

    @Test
    public void insertEncryptsBeforeSuper() {
        EncryptSampleEntity entity = new EncryptSampleEntity();
        entity.secret = "token-hash";
        StubEncryptModuleService stub = new StubEncryptModuleService();
        ReflectionTestUtils.setField(stub, "encrypt", fieldEncryptService);
        stub.persist = entity;
        Assert.assertTrue(stub.insert(entity) > 0);
        Assert.assertTrue(stub.persist.secret.startsWith("ENC$v1$"));
    }

    private static FieldEncryptService newFieldEncryptService(FieldEncryptProperties properties) {
        EncryptConfigFactory factory = new EncryptConfigFactory();
        ReflectionTestUtils.setField(factory, "handlers", Collections.emptyList());
        FieldEncryptConfigSource configSource = new FieldEncryptConfigSource();
        ReflectionTestUtils.setField(configSource, "properties", properties);
        ReflectionTestUtils.setField(configSource, "encryptConfigFactory", factory);
        FieldEncryptService service = new FieldEncryptService();
        ReflectionTestUtils.setField(service, "configSource", configSource);
        service.init();
        return service;
    }

    static class EncryptSampleEntity {
        @Column
        @FieldEncrypt
        String secret;
    }

    static class TestEncryptModuleService extends EncryptModuleService<BaseMapper<Object>, Object> {
    }

    static class StubEncryptModuleService extends EncryptModuleService<BaseMapper<EncryptSampleEntity>, EncryptSampleEntity> {
        EncryptSampleEntity persist;

        @Override
        public int insert(EncryptSampleEntity entity) {
            encrypt.onWrite(entity);
            persist = entity;
            return 1;
        }
    }
}
