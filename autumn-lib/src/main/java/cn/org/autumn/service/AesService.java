package cn.org.autumn.service;

import cn.org.autumn.config.CacheConfig;
import cn.org.autumn.config.EncryptConfigHandler;
import cn.org.autumn.exception.CodeException;
import cn.org.autumn.model.AesKey;
import cn.org.autumn.model.Encrypt;
import cn.org.autumn.model.Error;
import cn.org.autumn.site.EncryptConfigFactory;
import cn.org.autumn.utils.AES;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
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

    @Autowired
    Gson gson;

    /**
     * AES密钥缓存配置
     */
    private static CacheConfig aesKeyCacheConfig;

    public CacheConfig getAesKeyCacheConfig() {
        if (null == aesKeyCacheConfig) {
            EncryptConfigHandler.AesConfig config = encryptConfigFactory.getAesConfig();
            // 缓存过期时间 = 密钥有效期 + 服务端冗余保留时间
            // 确保在密钥过期后，服务端仍能解密正在传输的加密数据
            aesKeyCacheConfig = CacheConfig.builder()
                    .cacheName("AesServiceCache")
                    .keyType(String.class)
                    .valueType(AesKey.class)
                    .expireTime(config.getKeyValidMinutes() + config.getServerBufferMinutes())
                    .timeUnit(TimeUnit.MINUTES)
                    .build();
        }
        return aesKeyCacheConfig;
    }

    /**
     * 生成AES密钥和向量
     *
     * @param session 客户端UUID
     * @return AES密钥信息
     */
    public AesKey generate(String session) {
        try {
            if (StringUtils.isBlank(session)) {
                throw new RuntimeException(new CodeException(Error.RSA_SESSION_REQUIRED));
            }
            EncryptConfigHandler.AesConfig config = encryptConfigFactory.getAesConfig();
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
            String vectorBase64 = "";
            if (config.getIvSize() > 0) {
                // 生成AES向量
                byte[] iv = new byte[config.getIvSize()];
                secureRandom.nextBytes(iv);
                vectorBase64 = Base64.getEncoder().encodeToString(iv);
            }
            // 计算过期时间
            long expireTime = System.currentTimeMillis() + (config.getKeyValidMinutes() * 60 * 1000L);
            // 创建AES密钥对象
            AesKey aesKey = AesKey.builder().session(session).key(keyBase64).vector(vectorBase64).expireTime(expireTime).build();
            if (log.isDebugEnabled()) {
                log.debug("生成AES密钥，Session: {}, 过期时间: {}", session, expireTime);
            }
            return aesKey;
        } catch (Exception e) {
            log.error("生成AES密钥失败，Session: {}", session, e);
            throw new RuntimeException(new CodeException(Error.AES_KEY_GENERATE_FAILED));
        }
    }

    /**
     * 获取AES密钥（从缓存中获取或生成新的）
     * 仅在客户端主动请求获取密钥时调用，如果密钥即将过期则生成新的
     *
     * @param session 客户端UUID
     * @return AES密钥信息
     * @throws CodeException 密钥获取失败时抛出异常
     */
    public AesKey getAesKey(String session) throws CodeException {
        try {
            if (StringUtils.isBlank(session)) {
                throw new CodeException(Error.RSA_SESSION_REQUIRED);
            }
            EncryptConfigHandler.AesConfig config = encryptConfigFactory.getAesConfig();
            // 使用compute方法：如果缓存不存在则生成，存在则返回
            AesKey aesKey = cacheService.compute(session, () -> generate(session), getAesKeyCacheConfig());
            // 检查密钥是否有效：为null、已过期、格式无效或即将过期时，删除缓存并重新调用compute
            // 利用短路求值：如果aesKey为null，后面的条件不会执行
            if (aesKey == null || aesKey.isExpired() || StringUtils.isBlank(aesKey.getKey()) || aesKey.isExpiringSoon(config.getClientBufferMinutes())) {
                if (null != aesKey && log.isDebugEnabled())
                    log.debug("删除重建:{}, KEY:{}, 向量:{}, 过期:{}, 临期:{}, 过期时间:{}", aesKey.getSession(), aesKey.getKey(), aesKey.getVector(), aesKey.isExpired(), aesKey.isExpiringSoon(), null != aesKey.getExpireTime() ? new Date(aesKey.getExpireTime()) : "");
                cacheService.remove(getAesKeyCacheConfig().getCacheName(), session);
                aesKey = cacheService.compute(session, () -> generate(session), getAesKeyCacheConfig());
            }
            if (log.isDebugEnabled())
                log.debug("获取密钥:{}, KEY:{}, 向量:{}, 过期:{}, 临期:{}, 过期时间:{}", aesKey.getSession(), aesKey.getKey(), aesKey.getVector(), aesKey.isExpired(), aesKey.isExpiringSoon(), null != aesKey.getExpireTime() ? new Date(aesKey.getExpireTime()) : "");
            return aesKey;
        } catch (RuntimeException e) {
            if (e.getCause() instanceof CodeException) {
                throw (CodeException) e.getCause();
            }
            log.error("获取密钥失败:{}, 错误:{}", session, e.getMessage());
            throw new CodeException(Error.AES_KEY_NOT_FOUND);
        } catch (Exception e) {
            log.error("获取密钥失败:{}, 异常:{}", session, e.getMessage());
            throw new CodeException(Error.AES_KEY_NOT_FOUND);
        }
    }

    /**
     * 使用AES加密数据
     * 注意：此方法不会触发密钥更新，直接使用现有密钥（即使即将过期）
     * 因为客户端可能还在使用旧的密钥，不应该在加解密过程中动态更新
     *
     * @param data    待加密数据
     * @param session 客户端UUID
     * @return 加密后的数据（Base64编码）
     * @throws CodeException 加密失败时抛出异常
     */
    public String encrypt(String data, String session) throws CodeException {
        if (StringUtils.isBlank(data)) {
            return "";
        }
        if (StringUtils.isBlank(session)) {
            throw new CodeException(Error.RSA_SESSION_REQUIRED);
        }
        // 直接从缓存获取密钥，不触发更新（客户端可能还在使用旧密钥）
        AesKey aesKey = cacheService.get(getAesKeyCacheConfig().getCacheName(), session);
        if (aesKey == null) {
            throw new CodeException(Error.AES_KEY_NOT_FOUND);
        }
        if (StringUtils.isBlank(aesKey.getKey())) {
            log.error("AES密钥格式错误，密钥为空，Session: {}", session);
            throw new CodeException(Error.AES_KEY_FORMAT_ERROR);
        }
        try {
            // AES工具类使用Base64编码的密钥和向量
            // 密钥和向量已经是Base64编码，直接使用
            String encrypted = AES.encrypt(data, aesKey.getKey(), aesKey.getVector());
            if (StringUtils.isBlank(encrypted)) {
                log.error("加密为空:{}, 秘钥:{}, 向量:{}, 内容:{}", session, aesKey.getKey(), aesKey.getVector(), data);
                throw new CodeException(Error.AES_ENCRYPT_FAILED);
            }
            return encrypted;
        } catch (CodeException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("AES密钥或向量格式错误，加密失败，Session: {}, 错误: {}", session, e.getMessage());
            throw new CodeException(Error.AES_KEY_FORMAT_ERROR);
        } catch (Exception e) {
            log.error("AES加密失败，Session: {}", session, e);
            throw new CodeException(Error.AES_ENCRYPT_FAILED);
        }
    }

    public String decrypt(Encrypt encrypt) throws CodeException {
        if (null == encrypt)
            return "";
        return decrypt(encrypt.getCiphertext(), encrypt.getSession());
    }

    /**
     * 使用AES解密数据
     * 注意：此方法不会触发密钥更新，直接使用现有密钥（即使即将过期或已过期）
     * 因为客户端可能还在使用旧的密钥，不应该在加解密过程中动态更新
     * 支持旧密钥的平滑切换：即使密钥已过期，只要在服务端冗余保留时间内，仍可解密
     *
     * @param data    加密数据（Base64编码）
     * @param session 客户端UUID
     * @return 解密后的数据
     * @throws CodeException 解密失败时抛出异常
     */
    public String decrypt(String data, String session) throws CodeException {
        if (StringUtils.isBlank(data)) {
            return "";
        }
        if (StringUtils.isBlank(session)) {
            throw new CodeException(Error.RSA_SESSION_REQUIRED);
        }
        // 直接从缓存获取密钥，不触发更新（客户端可能还在使用旧密钥）
        AesKey aesKey = cacheService.get(getAesKeyCacheConfig().getCacheName(), session);
        if (aesKey == null) {
            throw new CodeException(Error.AES_KEY_NOT_FOUND);
        }
        if (StringUtils.isBlank(aesKey.getKey())) {
            log.error("AES密钥格式错误，密钥为空，Session: {}", session);
            throw new CodeException(Error.AES_KEY_FORMAT_ERROR);
        }
        // 检查密钥是否已过期（但仍在服务端冗余保留时间内）
        if (aesKey.isExpired()) {
            log.warn("密钥过期，Session: {}, 过期时间: {}", session, aesKey.getExpireTime());
        }
        try {
            // AES工具类使用Base64编码的密钥和向量
            // 密钥和向量已经是Base64编码，直接使用
            String decrypted = AES.decrypt(data, aesKey.getKey(), aesKey.getVector());
            if (StringUtils.isBlank(decrypted)) {
                log.error("解密为空:{}, 秘钥:{}, 向量:{}, 内容:{}", session, aesKey.getKey(), aesKey.getVector(), data);
                throw new CodeException(Error.AES_DECRYPTED_DATA_EMPTY);
            }
            return decrypted;
        } catch (CodeException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("AES密钥或向量格式错误，解密失败，Session: {}, 错误: {}", session, e.getMessage());
            throw new CodeException(Error.AES_KEY_FORMAT_ERROR);
        } catch (Exception e) {
            log.error("AES解密失败，Session: {}", session, e);
            throw new CodeException(Error.AES_DECRYPT_FAILED);
        }
    }

    /**
     * 检查AES密钥是否存在且有效
     *
     * @param session 客户端UUID
     * @return true-存在且有效，false-不存在或已过期
     */
    public boolean hasValidAesKey(String session) {
        if (StringUtils.isBlank(session)) {
            return false;
        }
        AesKey aesKey = cacheService.get(getAesKeyCacheConfig().getCacheName(), session);
        return aesKey != null && !aesKey.isExpired();
    }

    /**
     * 移除AES密钥
     *
     * @param session 客户端UUID
     */
    public void removeAesKey(String session) {
        if (StringUtils.isBlank(session)) {
            return;
        }
        cacheService.remove(getAesKeyCacheConfig().getCacheName(), session);
    }
}