package cn.org.autumn.crypto;

import cn.org.autumn.config.FieldEncryptProperties;
import cn.org.autumn.site.EncryptConfigFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

/** 字段加密单元测试装配辅助。 */
final class FieldEncryptTestSupport {

    private FieldEncryptTestSupport() {
    }

    static FieldEncryptService newService(FieldEncryptProperties properties) {
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
}
