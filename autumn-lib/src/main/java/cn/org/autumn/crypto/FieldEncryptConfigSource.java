package cn.org.autumn.crypto;

import cn.org.autumn.config.EncryptConfigHandler;
import cn.org.autumn.config.FieldEncryptProperties;
import cn.org.autumn.site.EncryptConfigFactory;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 合并 yml、环境变量与 {@link EncryptConfigHandler} 扩展后的字段加密配置（单一解析入口）。
 * <p>
 * docker-compose / 环境变量支持多种写法（显式短前缀优先于默认 properties）：
 * <ul>
 *   <li>{@code crypto.field.enabled|key|hash-key}（短前缀，与 bighub 一致）</li>
 *   <li>{@code autumn.crypto.field.*} / {@code AUTUMN_CRYPTO_FIELD_*}（Spring 松散绑定）</li>
 *   <li>{@code AUTUMN_FIELD_ENCRYPT_*} / {@code AUTUMN_FIELD_HASH_KEY}（文档历史名）</li>
 * </ul>
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
    private Environment environment;

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
        resolved.configWriteEnabled = resolveWriteEnabled();
        resolved.keyBase64 = resolveKeyBase64();
        String hash = resolveHashKeyBase64();
        resolved.hashKeyBase64 = StringUtils.isNotBlank(hash) ? hash : resolved.keyBase64;
        String prefix = firstNonBlank(
                StringUtils.trimToNull(properties.getPrefix()),
                env("crypto.field.prefix"),
                env("autumn.crypto.field.prefix")
        );
        resolved.prefix = StringUtils.isNotBlank(prefix) ? prefix : DEFAULT_PREFIX;
        return resolved;
    }

    /**
     * 写入开关优先级：显式 {@code crypto.field.enabled} → {@code autumn.crypto.field.enabled} →
     * {@code AUTUMN_FIELD_ENCRYPT_ENABLED} → {@link FieldEncryptProperties#isEnabled()}。
     */
    private boolean resolveWriteEnabled() {
        Boolean shortPrefix = envBoolean("crypto.field.enabled");
        if (shortPrefix != null) {
            return shortPrefix;
        }
        Boolean autumn = envBoolean("autumn.crypto.field.enabled");
        if (autumn != null) {
            return autumn;
        }
        Boolean legacy = envBoolean("AUTUMN_FIELD_ENCRYPT_ENABLED");
        if (legacy != null) {
            return legacy;
        }
        return properties.isEnabled();
    }

    private String resolveKeyBase64() {
        return firstNonBlank(
                env("crypto.field.key"),
                StringUtils.trimToNull(properties.getKey()),
                env("autumn.crypto.field.key"),
                env("AUTUMN_CRYPTO_FIELD_KEY"),
                env("AUTUMN_FIELD_ENCRYPT_KEY")
        );
    }

    private String resolveHashKeyBase64() {
        return firstNonBlank(
                env("crypto.field.hash-key"),
                env("crypto.field.hashKey"),
                StringUtils.trimToNull(properties.getHashKey()),
                env("autumn.crypto.field.hash-key"),
                env("AUTUMN_CRYPTO_FIELD_HASH_KEY"),
                env("AUTUMN_FIELD_HASH_KEY")
        );
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
        byte[] decoded = decodeBase64Quietly(candidate);
        if (decoded == null || decoded.length != AES_KEY_BYTES) {
            return null;
        }
        return candidate.trim();
    }

    public String validHashBase64(String candidate) {
        byte[] decoded = decodeBase64Quietly(candidate);
        if (decoded == null || decoded.length < MIN_HASH_KEY_BYTES) {
            return null;
        }
        return candidate.trim();
    }

    private static byte[] decodeBase64Quietly(String candidate) {
        return FieldCrypto.decodeKeyBase64(candidate);
    }

    private String env(String key) {
        if (environment == null || StringUtils.isBlank(key)) {
            return null;
        }
        return StringUtils.trimToNull(environment.getProperty(key));
    }

    private Boolean envBoolean(String key) {
        String raw = env(key);
        if (raw == null) {
            return null;
        }
        if ("true".equalsIgnoreCase(raw) || "1".equals(raw) || "yes".equalsIgnoreCase(raw)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(raw) || "0".equals(raw) || "no".equalsIgnoreCase(raw)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (StringUtils.isNotBlank(v)) {
                return v.trim();
            }
        }
        return null;
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
