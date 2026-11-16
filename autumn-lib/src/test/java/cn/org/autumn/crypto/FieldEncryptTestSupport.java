package cn.org.autumn.crypto;

import cn.org.autumn.config.FieldEncryptProperties;
import cn.org.autumn.site.EncryptConfigFactory;
import java.util.Collections;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

/** 字段加密单元测试装配辅助。 */
final class FieldEncryptTestSupport {

    private FieldEncryptTestSupport() {
    }

    static FieldEncryptService newService(FieldEncryptProperties properties) {
        return newService(properties, new MockEnvironment());
    }

    static FieldEncryptService newService(FieldEncryptProperties properties, MockEnvironment environment) {
        EncryptConfigFactory factory = new EncryptConfigFactory();
        ReflectionTestUtils.setField(factory, "handlers", Collections.emptyList());
        FieldEncryptConfigSource configSource = new FieldEncryptConfigSource();
        ReflectionTestUtils.setField(configSource, "properties", properties);
        ReflectionTestUtils.setField(configSource, "environment", environment);
        ReflectionTestUtils.setField(configSource, "encryptConfigFactory", factory);
        FieldEncryptService service = new FieldEncryptService();
        ReflectionTestUtils.setField(service, "configSource", configSource);
        service.init();
        return service;
    }
}
