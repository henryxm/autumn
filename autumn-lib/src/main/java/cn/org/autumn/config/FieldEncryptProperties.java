package cn.org.autumn.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 字段级存储加密配置，前缀 {@code autumn.crypto.field.*}。
 * <p>
 * {@link #key}：AES 加密主密钥；{@link #hashKey}：searchable 盲索引 HMAC（与 {@code @FieldEncrypt#vector()} 无关）。
 * 详见 {@code docs/AI_FIELD_ENCRYPT.md} §0。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "autumn.crypto.field")
public class FieldEncryptProperties {

    private boolean enabled = false;

    /**
     * AES-256 主密钥，Base64 编码（32 字节）；可用环境变量 {@code AUTUMN_FIELD_ENCRYPT_KEY} 注入。
     */
    private String key = "";

    /**
     * 盲索引 HMAC 密钥，Base64；默认同 {@link #key}。
     */
    private String hashKey = "";

    /**
     * 密文版本前缀。
     */
    private String prefix = "ENC$v1$";
}
