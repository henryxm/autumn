package cn.org.autumn.service;

import cn.org.autumn.config.CacheConfig;
import cn.org.autumn.config.EncryptConfigHandler;
import cn.org.autumn.exception.CodeException;
import cn.org.autumn.model.AesKey;
import cn.org.autumn.model.Encrypt;
import cn.org.autumn.model.Error;
import cn.org.autumn.site.EncryptConfigFactory;
import cn.org.autumn.utils.AES;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * AES加密服务
 * 提供AES密钥生成、加密、解密功能
 *
 * @author Autumn
 */
@Slf4j
@Service
public class AesService {

    @Autowired
    private CacheService cacheService;

    @Autowired
    EncryptConfigFactory encryptConfigFactory;

    /**
     * AES密钥缓存配置
     */
    private static CacheConfig aesKeyCacheConfig;

    /**
     * 获取AES配置
     */
    private EncryptConfigHandler.AesConfig getAesConfig() {
        return encryptConfigFactory.getAesConfig();
    }


    public void initCacheConfig() {
        EncryptConfigHandler.AesConfig config = getAesConfig();
        // 缓存过期时间 = 密钥有效期 + 服务端冗余保留时间
        // 确保在密钥过期后，服务端仍能解密正在传输的加密数据
        aesKeyCacheConfig = CacheConfig.builder()
                .cacheName("AesServiceCache")
                .keyType(String.class)
                .valueType(AesKey.class)
                .expireTime(config.getKeyValidMinutes() + config.getServerBufferMinutes())
                .timeUnit(TimeUnit.MINUTES)
                .build();
        if (log.isDebugEnabled()) {
            log.debug("AES缓存配置初始化完成: expireTime={}", aesKeyCacheConfig.getExpireTime());
        }
    }

    public CacheConfig getAesKeyCacheConfig() {
        if (null == aesKeyCacheConfig)
            initCacheConfig();
        return aesKeyCacheConfig;
    }

    /**
     * 生成AES密钥和向量
     *
     * @param uuid 客户端UUID
     * @return AES密钥信息
     * @throws CodeException 密钥生成失败时抛出异常
     */
    public AesKey generate(String uuid) throws CodeException {
        if (StringUtils.isBlank(uuid)) {
            throw new CodeException(Error.RSA_UUID_REQUIRED);
        }
        try {
            EncryptConfigHandler.AesConfig config = getAesConfig();
            // 生成AES密钥
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            SecureRandom secureRandom = new SecureRandom();
            keyGenerator.init(config.getKeySize(), secureRandom);
            SecretKey secretKey = keyGenerator.generateKey();
            byte[] keyBytes = secretKey.getEncoded();
            // 验证密钥长度
            int expectedKeyLength = config.getKeySize() / 8;
            if (keyBytes.length != expectedKeyLength) {
                log.error("AES密钥长度错误，期望: {}字节，实际: {}字节", expectedKeyLength, keyBytes.length);
                throw new CodeException(Error.AES_KEY_LENGTH_ERROR);
            }
            String keyBase64 = Base64.getEncoder().encodeToString(keyBytes);
            // 生成AES向量
            byte[] iv = new byte[config.getIvSize()];
            secureRandom.nextBytes(iv);
            String vectorBase64 = Base64.getEncoder().encodeToString(iv);
            // 计算过期时间
            long expireTime = System.currentTimeMillis() + (config.getKeyValidMinutes() * 60 * 1000L);
            // 创建AES密钥对象
            AesKey aesKey = AesKey.builder().uuid(uuid).key(keyBase64).vector(vectorBase64).expireTime(expireTime).build();
            // 缓存AES密钥
            cacheService.put(getAesKeyCacheConfig().getCacheName(), uuid, aesKey);
            if (log.isDebugEnabled()) {
                log.debug("生成AES密钥，UUID: {}, 过期时间: {}", uuid, expireTime);
            }
            return aesKey;
        } catch (CodeException e) {
            throw e;
        } catch (Exception e) {
            log.error("生成AES密钥失败，UUID: {}", uuid, e);
            throw new CodeException(Error.AES_KEY_GENERATE_FAILED);
        }
    }

    /**
     * 获取AES密钥（从缓存中获取或生成新的）
     *
     * @param uuid 客户端UUID
     * @return AES密钥信息
     * @throws CodeException 密钥获取失败时抛出异常
     */
    public AesKey getAesKey(String uuid) throws CodeException {
        if (StringUtils.isBlank(uuid)) {
            throw new CodeException(Error.RSA_UUID_REQUIRED);
        }
        try {
            EncryptConfigHandler.AesConfig config = getAesConfig();
            // 从缓存中获取
            AesKey aesKey = cacheService.get(getAesKeyCacheConfig().getCacheName(), uuid);
            if (aesKey != null && !aesKey.isExpired()) {
                // 检查密钥格式
                if (StringUtils.isBlank(aesKey.getKey()) || StringUtils.isBlank(aesKey.getVector())) {
                    if (log.isDebugEnabled())
                        log.warn("AES密钥格式错误，UUID: {}, 密钥或向量为空", uuid);
                    // 删除无效密钥，重新生成
                    cacheService.remove(getAesKeyCacheConfig().getCacheName(), uuid);
                    return generate(uuid);
                }
                // 如果密钥即将过期，生成新的密钥
                if (aesKey.isExpiringSoon(config.getClientBufferMinutes())) {
                    if (log.isDebugEnabled()) {
                        log.debug("AES密钥即将过期，生成新的密钥，UUID: {}", uuid);
                    }
                    aesKey = generate(uuid);
                }
                return aesKey;
            }
            // 缓存中不存在或已过期，生成新的
            return generate(uuid);
        } catch (Exception e) {
            log.error("获取AES密钥失败，UUID: {}", uuid, e);
            throw new CodeException(Error.AES_KEY_NOT_FOUND);
        }
    }

    /**
     * 使用AES加密数据
     *
     * @param data 待加密数据
     * @param uuid 客户端UUID
     * @return 加密后的数据（Base64编码）
     * @throws CodeException 加密失败时抛出异常
     */
    public String encrypt(String data, String uuid) throws CodeException {
        if (StringUtils.isBlank(data)) {
            throw new CodeException(Error.AES_ENCRYPTED_DATA_EMPTY);
        }
        if (StringUtils.isBlank(uuid)) {
            throw new CodeException(Error.RSA_UUID_REQUIRED);
        }
        AesKey aesKey = getAesKey(uuid);
        if (aesKey == null) {
            throw new CodeException(Error.AES_KEY_NOT_FOUND);
        }
        if (StringUtils.isBlank(aesKey.getKey())) {
            log.error("AES密钥格式错误，密钥为空，UUID: {}", uuid);
            throw new CodeException(Error.AES_KEY_FORMAT_ERROR);
        }
        if (StringUtils.isBlank(aesKey.getVector())) {
            log.error("AES向量格式错误，向量为空，UUID: {}", uuid);
            throw new CodeException(Error.AES_VECTOR_FORMAT_ERROR);
        }
        try {
            // AES工具类使用Base64编码的密钥和向量
            // 密钥和向量已经是Base64编码，直接使用
            String encrypted = AES.encrypt(data, aesKey.getKey(), aesKey.getVector());
            if (StringUtils.isBlank(encrypted)) {
                log.error("加密为空:{}, 秘钥:{}, 向量:{}, 内容:{}", uuid, aesKey.getKey(), aesKey.getVector(), data);
                throw new CodeException(Error.AES_ENCRYPT_FAILED);
            }
            return encrypted;
        } catch (CodeException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("AES密钥或向量格式错误，加密失败，UUID: {}, 错误: {}", uuid, e.getMessage());
            throw new CodeException(Error.AES_KEY_FORMAT_ERROR);
        } catch (Exception e) {
            log.error("AES加密失败，UUID: {}", uuid, e);
            throw new CodeException(Error.AES_ENCRYPT_FAILED);
        }
    }

    public String decrypt(Encrypt encrypt) throws CodeException {
        return decrypt(encrypt.getCiphertext(), encrypt.getUuid());
    }

    /**
     * 使用AES解密数据
     *
     * @param data 加密数据（Base64编码）
     * @param uuid 客户端UUID
     * @return 解密后的数据
     * @throws CodeException 解密失败时抛出异常
     */
    public String decrypt(String data, String uuid) throws CodeException {
        if (StringUtils.isBlank(data)) {
            throw new CodeException(Error.AES_ENCRYPTED_DATA_EMPTY);
        }
        if (StringUtils.isBlank(uuid)) {
            throw new CodeException(Error.RSA_UUID_REQUIRED);
        }
        AesKey aesKey = getAesKey(uuid);
        if (aesKey == null) {
            throw new CodeException(Error.AES_KEY_NOT_FOUND);
        }
        if (StringUtils.isBlank(aesKey.getKey())) {
            log.error("AES密钥格式错误，密钥为空，UUID: {}", uuid);
            throw new CodeException(Error.AES_KEY_FORMAT_ERROR);
        }
        if (StringUtils.isBlank(aesKey.getVector())) {
            log.error("AES向量格式错误，向量为空，UUID: {}", uuid);
            throw new CodeException(Error.AES_VECTOR_FORMAT_ERROR);
        }
        // 检查密钥是否已过期（但仍在服务端冗余保留时间内）
        if (aesKey.isExpired()) {
            log.warn("使用已过期的AES密钥进行解密，UUID: {}, 过期时间: {}", uuid, aesKey.getExpireTime());
        }
        try {
            // AES工具类使用Base64编码的密钥和向量
            // 密钥和向量已经是Base64编码，直接使用
            String decrypted = AES.decrypt(data, aesKey.getKey(), aesKey.getVector());
            if (StringUtils.isBlank(decrypted)) {
                log.error("解密为空:{}, 秘钥:{}, 向量:{}, 内容:{}", uuid, aesKey.getKey(), aesKey.getVector(), data);
                throw new CodeException(Error.AES_DECRYPTED_DATA_EMPTY);
            }
            return decrypted;
        } catch (CodeException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("AES密钥或向量格式错误，解密失败，UUID: {}, 错误: {}", uuid, e.getMessage());
            throw new CodeException(Error.AES_KEY_FORMAT_ERROR);
        } catch (Exception e) {
            log.error("AES解密失败，UUID: {}", uuid, e);
            throw new CodeException(Error.AES_DECRYPT_FAILED);
        }
    }

    /**
     * 检查AES密钥是否存在且有效
     *
     * @param uuid 客户端UUID
     * @return true-存在且有效，false-不存在或已过期
     */
    public boolean hasValidAesKey(String uuid) {
        if (StringUtils.isBlank(uuid)) {
            return false;
        }
        AesKey aesKey = cacheService.get(getAesKeyCacheConfig().getCacheName(), uuid);
        return aesKey != null && !aesKey.isExpired();
    }

    /**
     * 移除AES密钥
     *
     * @param uuid 客户端UUID
     */
    public void removeAesKey(String uuid) {
        if (StringUtils.isBlank(uuid)) {
            return;
        }
        cacheService.remove(getAesKeyCacheConfig().getCacheName(), uuid);
    }
}