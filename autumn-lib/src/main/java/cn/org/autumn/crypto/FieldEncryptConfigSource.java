package cn.org.autumn.crypto;

import cn.org.autumn.config.EncryptConfigHandler;
import cn.org.autumn.config.FieldEncryptProperties;
import cn.org.autumn.site.EncryptConfigFactory;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * 合并 yml、环境变量与 {@link EncryptConfigHandler} 扩展后的字段加密配置（单一解析入口）。
 * <p>
 * {@link EncryptConfigFactory} 使用 {@link Lazy} 注入，避免与 {@code sqlSessionFactory} 初始化形成环。
 */
@Component
public class FieldEncryptConfigSource {

    private static final int AES_KEY_BYTES = 32;
    private static final int MIN_HASH_KEY_BYTES = 16;
    private static final String DEFAULT_PREFIX = "ENC$v1$";

    @Autowired
    private FieldEncryptProperties properties;

    @Autowired
    @Lazy
    private EncryptConfigFactory encryptConfigFactory;

    /**
     * 读取环境侧完整配置（不含 Redis / sys_config 运行时覆盖）。
     */
    public Resolved resolveFromEnvironment() {
        Resolved resolved = resolveFromProperties();
        mergeHandlerConfig(resolved, encryptConfigFactory.getFieldEncryptConfig());
        return resolved;
    }

    /** 仅 yml / 环境变量，不触发 {@link EncryptConfigFactory}。 */
    public Resolved resolveFromProperties() {
        Resolved resolved = new Resolved();
        resolved.configWriteEnabled = properties.isEnabled();
        resolved.keyBase64 = properties.getKey();
        resolved.hashKeyBase64 = StringUtils.isNotBlank(properties.getHashKey()) ? properties.getHashKey() : properties.getKey();
        resolved.prefix = StringUtils.isNotBlank(properties.getPrefix()) ? properties.getPrefix() : DEFAULT_PREFIX;
        return resolved;
    }

    public String validKeyBase64() {
        return validKeyBase64(resolveFromProperties().keyBase64);
    }

    public String validHashKeyBase64(String fallbackKeyBase64) {
        Resolved resolved = resolveFromProperties();
        String hash = StringUtils.isNotBlank(resolved.hashKeyBase64) ? resolved.hashKeyBase64 : fallbackKeyBase64;
        return validHashBase64(hash);
    }

    public String validKeyBase64(String candidate) {
        if (StringUtils.isBlank(candidate)) {
            return null;
        }
        byte[] decoded = FieldCrypto.decodeKeyBase64(candidate);
        if (decoded == null || decoded.length != AES_KEY_BYTES) {
            return null;
        }
        return candidate.trim();
    }

    public String validHashBase64(String candidate) {
        if (StringUtils.isBlank(candidate)) {
            return null;
        }
        byte[] decoded = FieldCrypto.decodeKeyBase64(candidate);
        if (decoded == null || decoded.length < MIN_HASH_KEY_BYTES) {
            return null;
        }
        return candidate.trim();
    }

    private static void mergeHandlerConfig(Resolved resolved, EncryptConfigHandler.FieldEncryptConfig custom) {
        if (custom == null) {
            return;
        }
        if (custom.isEnabled()) {
            resolved.configWriteEnabled = true;
        }
        if (StringUtils.isNotBlank(custom.getKeyBase64())) {
            resolved.keyBase64 = custom.getKeyBase64();
        }
        if (StringUtils.isNotBlank(custom.getHashKeyBase64())) {
            resolved.hashKeyBase64 = custom.getHashKeyBase64();
        }
        if (StringUtils.isNotBlank(custom.getPrefix())) {
            resolved.prefix = custom.getPrefix();
        }
        if (StringUtils.isBlank(resolved.hashKeyBase64)) {
            resolved.hashKeyBase64 = resolved.keyBase64;
        }
    }

    @Getter
    public static class Resolved {
        private boolean configWriteEnabled;
        private String keyBase64;
        private String hashKeyBase64;
        private String prefix;
    }
}
