package cn.org.autumn.base;

import cn.org.autumn.annotation.FieldEncrypt;
import cn.org.autumn.config.FieldEncryptProperties;
import cn.org.autumn.crypto.FieldEncryptConfigSource;
import cn.org.autumn.crypto.FieldEncryptService;
import cn.org.autumn.site.EncryptConfigFactory;
import cn.org.autumn.table.annotation.Column;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

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
    public void insertRestoresPlainAfterPersist() {
        EncryptSampleEntity entity = new EncryptSampleEntity();
        entity.secret = "token-hash";
        StubEncryptModuleService stub = new StubEncryptModuleService();
        ReflectionTestUtils.setField(stub, "encrypt", fieldEncryptService);
        Assert.assertTrue(stub.insert(entity));
        Assert.assertEquals("token-hash", entity.secret);
        Assert.assertTrue(stub.persist.secret.startsWith("ENC$v1$"));
    }

    @Test
    public void restoreAfterWriteIsIdempotent() {
        EncryptSampleEntity entity = new EncryptSampleEntity();
        entity.secret = "plain-secret";
        fieldEncryptService.onWrite(entity);
        fieldEncryptService.restoreAfterWrite(entity);
        Assert.assertEquals("plain-secret", entity.secret);
        fieldEncryptService.restoreAfterWrite(entity);
        Assert.assertEquals("plain-secret", entity.secret);
    }

    @Test
    public void overridesAllServiceImplSelectMethods() {
        Set<String> expected = new HashSet<>();
        for (Method method : ServiceImpl.class.getMethods()) {
            if (method.getName().startsWith("select") && !"selectCount".equals(method.getName())) {
                expected.add(method.getName());
            }
        }
        Set<String> actual = new HashSet<>();
        for (Method method : EncryptModuleService.class.getDeclaredMethods()) {
            if (method.getName().startsWith("select") && !"selectCount".equals(method.getName())) {
                actual.add(method.getName());
            }
        }
        Assert.assertTrue("EncryptModuleService 须覆盖 ServiceImpl 全部 select*（除 selectCount）", actual.containsAll(expected));
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
        public boolean insert(EncryptSampleEntity entity) {
            encrypt.onWrite(entity);
            try {
                persist = entity;
                return true;
            } finally {
                restoreAfterWrite(entity);
            }
        }
    }
}
