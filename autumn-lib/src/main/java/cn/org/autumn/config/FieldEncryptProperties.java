package cn.org.autumn.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 字段级存储加密配置，前缀 {@code autumn.crypto.field.*}。
 * <p>
 * docker-compose / 环境变量可用：
 * <ul>
 *   <li>{@code AUTUMN_CRYPTO_FIELD_ENABLED} / {@code AUTUMN_CRYPTO_FIELD_KEY} / {@code AUTUMN_CRYPTO_FIELD_HASH_KEY}</li>
 *   <li>短前缀 {@code crypto.field.enabled} / {@code crypto.field.key} / {@code crypto.field.hash-key}（由
 *       {@code FieldEncryptConfigSource} 别名解析）</li>
 *   <li>历史名 {@code AUTUMN_FIELD_ENCRYPT_ENABLED} / {@code AUTUMN_FIELD_ENCRYPT_KEY} / {@code AUTUMN_FIELD_HASH_KEY}</li>
 * </ul>
 * {@link #key}：AES 主密钥；{@link #hashKey}：searchable 盲索引 HMAC。详见 {@code docs/AI_FIELD_ENCRYPT.md}。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "autumn.crypto.field")
public class FieldEncryptProperties {

    private boolean enabled = false;

    /**
     * AES-256 主密钥，Base64 编码（32 字节）。
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
