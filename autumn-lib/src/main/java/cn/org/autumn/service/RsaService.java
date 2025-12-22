package cn.org.autumn.service;

import cn.org.autumn.config.CacheConfig;
import cn.org.autumn.config.EncryptConfigHandler;
import cn.org.autumn.exception.CodeException;
import cn.org.autumn.model.ClientPublicKey;
import cn.org.autumn.model.Encrypt;
import cn.org.autumn.model.Error;
import cn.org.autumn.model.KeyPair;
import cn.org.autumn.site.EncryptConfigFactory;
import cn.org.autumn.utils.RsaUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RsaService {

    @Autowired
    CacheService cacheService;

    @Autowired
    EncryptConfigFactory encryptConfigFactory;

    /**
     * 服务端密钥对缓存配置
     * 缓存过期时间 = 密钥对有效期 + 服务端冗余保留时间
     * 确保在密钥对过期后，服务端仍能解密正在传输的数据
     */
    private static CacheConfig serverPrivateKeyConfig;

    /**
     * 客户端公钥缓存配置
     */
    private static CacheConfig clientPublicKeyConfig;

    /**
     * 获取RSA配置
     */
    private EncryptConfigHandler.RsaConfig getRsaConfig() {
        return encryptConfigFactory.getRsaConfig();
    }

    public CacheConfig getServerPrivateKeyConfig() {
        if (null == serverPrivateKeyConfig) {
            EncryptConfigHandler.RsaConfig config = getRsaConfig();
            serverPrivateKeyConfig = CacheConfig.builder()
                    .cacheName("RsaServiceCache")
                    .keyType(String.class)
                    .valueType(KeyPair.class)
                    .expireTime(config.getKeyPairValidMinutes() + config.getServerBufferMinutes())
                    .timeUnit(TimeUnit.MINUTES)
                    .build();
        }
        return serverPrivateKeyConfig;
    }

    public CacheConfig getClientPublicKeyConfig() {
        if (null == clientPublicKeyConfig) {
            EncryptConfigHandler.RsaConfig config = getRsaConfig();
            clientPublicKeyConfig = CacheConfig.builder()
                    .cacheName("ClientPublicKeyCache")
                    .keyType(String.class)
                    .valueType(ClientPublicKey.class)
                    .expireTime(config.getClientPublicKeyValidMinutes())
                    .timeUnit(TimeUnit.MINUTES)
                    .build();
        }
        return clientPublicKeyConfig;
    }

    /**
     * 生成新的密钥对
     *
     * @param uuid 客户端UUID标识
     * @return 包含过期时间的密钥对
     * @throws CodeException 密钥生成失败时抛出异常
     */
    public KeyPair generate(String uuid) throws CodeException {
        if (StringUtils.isBlank(uuid)) {
            throw new CodeException(Error.RSA_UUID_REQUIRED);
        }
        try {
            EncryptConfigHandler.RsaConfig config = getRsaConfig();
            KeyPair pair = RsaUtil.generate(config.getKeySize());
            pair.setUuid(uuid);
            // 设置过期时间：当前时间 + 密钥对有效期
            long expireTime = System.currentTimeMillis() + (config.getKeyPairValidMinutes() * 60 * 1000L);
            pair.setExpireTime(expireTime);
            if (log.isDebugEnabled()) {
                log.debug("生成新的密钥对，UUID: {}, 密钥长度: {}位, 过期时间: {}", uuid, config.getKeySize(), expireTime);
            }
            return pair;
        } catch (Exception e) {
            log.error("生成RSA密钥对失败，UUID: {}", uuid, e);
            throw new CodeException(Error.RSA_KEY_GENERATE_FAILED);
        }
    }

    /**
     * 获取密钥对（如果不存在则生成新的）
     * 返回的密钥对包含过期时间，客户端应在此时间之前重新获取
     *
     * @param uuid 客户端UUID标识
     * @return 包含过期时间的密钥对
     * @throws CodeException 密钥获取或生成失败时抛出异常
     */
    public KeyPair getKeyPair(String uuid) throws CodeException {
        if (StringUtils.isBlank(uuid)) {
            throw new CodeException(Error.RSA_UUID_REQUIRED);
        }
        try {
            EncryptConfigHandler.RsaConfig config = getRsaConfig();
            KeyPair pair = cacheService.compute(uuid, () -> {
                try {
                    return generate(uuid);
                } catch (CodeException e) {
                    throw new RuntimeException(e);
                }
            }, getServerPrivateKeyConfig());
            // 如果密钥对即将过期，生成新的密钥对
            if (pair != null && pair.isExpiringSoon(config.getClientBufferMinutes())) {
                if (log.isDebugEnabled()) {
                    log.debug("密钥对即将过期，生成新的密钥对，UUID: {}", uuid);
                }
                pair = generate(uuid);
                // 更新缓存
                cacheService.put(getServerPrivateKeyConfig().getCacheName(), uuid, pair);
            }
            if (pair == null) {
                throw new CodeException(Error.RSA_KEY_PAIR_NOT_FOUND);
            }
            return pair;
        } catch (RuntimeException e) {
            if (e.getCause() instanceof CodeException) {
                throw (CodeException) e.getCause();
            }
            log.error("获取密钥对失败，UUID: {}", uuid, e);
            throw new CodeException(Error.RSA_KEY_PAIR_NOT_FOUND);
        }
    }

    /**
     * 解密数据
     * 支持旧密钥对的平滑切换：即使密钥对已过期，只要在服务端冗余保留时间内，仍可解密
     *
     * @param value   加密会话数据
     * @param session HTTP Session
     * @return 解密后的数据
     * @throws CodeException 解密失败时抛出异常
     */
    /**
     * 解密数据（使用UUID）
     * 支持旧密钥对的平滑切换：即使密钥对已过期，只要在服务端冗余保留时间内，仍可解密
     *
     * @param value 加密数据（包含uuid）
     * @return 解密后的数据
     * @throws CodeException 解密失败时抛出异常
     */
    public String decrypt(Encrypt value) throws CodeException {
        if (StringUtils.isBlank(value.getEncrypt())) {
            return "";
        }
        if (StringUtils.isBlank(value.getUuid())) {
            throw new CodeException(Error.RSA_UUID_REQUIRED);
        }
        // 从缓存中获取密钥对
        KeyPair keyPair = cacheService.get(getServerPrivateKeyConfig().getCacheName(), value.getUuid());
        if (keyPair == null) {
            throw new CodeException(Error.RSA_PRIVATE_KEY_NOT_FOUND);
        }
        String privateKey = keyPair.getPrivateKey();
        if (StringUtils.isBlank(privateKey)) {
            throw new CodeException(Error.RSA_PRIVATE_KEY_NOT_FOUND);
        }
        // 检查密钥对是否已过期（但仍在服务端冗余保留时间内）
        if (keyPair.isExpired()) {
            log.warn("使用已过期的密钥对进行解密，UUID: {}, 过期时间: {}", value.getUuid(), keyPair.getExpireTime());
        }
        // 执行解密
        try {
            String decrypted = RsaUtil.decrypt(value.getEncrypt(), privateKey);
            if (StringUtils.isBlank(decrypted)) {
                log.warn("解密结果为空，UUID: {}", value.getUuid());
            }
            return decrypted;
        } catch (IllegalArgumentException e) {
            log.error("RSA密钥格式错误，解密失败，UUID: {}, 错误: {}", value.getUuid(), e.getMessage());
            throw new CodeException(Error.RSA_KEY_FORMAT_ERROR);
        } catch (Exception e) {
            log.error("RSA解密失败，UUID: {}, 错误: {}", value.getUuid(), e.getMessage(), e);
            throw new CodeException(Error.RSA_DECRYPT_FAILED);
        }
    }

    /**
     * 保存客户端公钥（使用UUID作为客户端标识）
     *
     * @param uuid       客户端UUID标识
     * @param publicKey  客户端公钥
     * @param expireTime 客户端指定的过期时间（毫秒时间戳），如果为null则使用后端默认过期时间
     * @return 保存的客户端公钥信息
     * @throws CodeException 保存失败时抛出异常
     */
    public ClientPublicKey savePublicKey(String uuid, String publicKey, Long expireTime) throws CodeException {
        if (StringUtils.isBlank(uuid)) {
            throw new CodeException(Error.RSA_UUID_REQUIRED);
        }
        if (StringUtils.isBlank(publicKey)) {
            throw new CodeException(Error.RSA_KEY_FORMAT_ERROR);
        }
        // 验证公钥格式：尝试使用公钥加密一个测试字符串来验证格式
        try {
            String testData = "test";
            RsaUtil.encrypt(testData, publicKey);
        } catch (IllegalArgumentException e) {
            log.error("客户端公钥格式错误，UUID: {}, 错误: {}", uuid, e.getMessage());
            throw new CodeException(Error.RSA_KEY_FORMAT_ERROR);
        } catch (RuntimeException e) {
            // RsaUtil.encrypt可能抛出RuntimeException包装的InvalidKeyException
            Throwable cause = e.getCause();
            if (cause instanceof java.security.InvalidKeyException) {
                log.error("客户端公钥解析失败，UUID: {}, 错误: {}", uuid, cause.getMessage());
                throw new CodeException(Error.RSA_PUBLIC_KEY_PARSE_ERROR);
            }
            log.error("客户端公钥验证失败，UUID: {}, 错误: {}", uuid, e.getMessage());
            throw new CodeException(Error.RSA_KEY_FORMAT_ERROR);
        } catch (Exception e) {
            log.error("客户端公钥验证失败，UUID: {}, 错误: {}", uuid, e.getMessage());
            throw new CodeException(Error.RSA_KEY_FORMAT_ERROR);
        }
        // 创建客户端公钥对象
        ClientPublicKey clientPublicKey = new ClientPublicKey(uuid, publicKey);
        // 处理过期时间
        EncryptConfigHandler.RsaConfig config = getRsaConfig();
        long finalExpireTime;
        long currentTime = System.currentTimeMillis();
        long maxExpireTime = currentTime + (config.getClientPublicKeyValidMinutes() * 60 * 1000L); // 最大过期时间（后端默认）
        if (expireTime != null) {
            // 客户端提供了过期时间，进行验证
            if (expireTime <= currentTime) {
                throw new IllegalArgumentException("过期时间不能小于或等于当前时间");
            }
            if (expireTime > maxExpireTime) {
                log.warn("客户端指定的过期时间超过最大有效期，使用最大有效期。UUID: {}, 客户端指定: {}, 最大有效期: {}", uuid, expireTime, maxExpireTime);
                finalExpireTime = maxExpireTime;
            } else {
                finalExpireTime = expireTime;
                if (log.isDebugEnabled()) {
                    log.debug("使用客户端指定的过期时间，UUID: {}, 过期时间: {}", uuid, finalExpireTime);
                }
            }
        } else {
            // 客户端未提供过期时间，使用后端默认过期时间
            finalExpireTime = maxExpireTime;
            if (log.isDebugEnabled()) {
                log.debug("使用后端默认过期时间，UUID: {}, 过期时间: {}", uuid, finalExpireTime);
            }
        }
        clientPublicKey.setExpireTime(finalExpireTime);
        // 保存到缓存
        cacheService.put(getClientPublicKeyConfig().getCacheName(), uuid, clientPublicKey);
        if (log.isDebugEnabled()) {
            log.debug("保存客户端公钥，UUID: {}, 过期时间: {}", uuid, finalExpireTime);
        }
        return clientPublicKey;
    }

    /**
     * 获取客户端公钥
     *
     * @param uuid 客户端标识
     * @return 客户端公钥信息，如果不存在或已过期返回null
     */
    public ClientPublicKey getClientPublicKey(String uuid) {
        if (StringUtils.isBlank(uuid)) {
            return null;
        }
        ClientPublicKey clientPublicKey = cacheService.get(getClientPublicKeyConfig().getCacheName(), uuid);
        if (clientPublicKey != null && clientPublicKey.isExpired()) {
            log.warn("客户端公钥已过期，ClientId: {}, 过期时间: {}", uuid, clientPublicKey.getExpireTime());
            // 可以选择删除过期公钥或返回null
            return null;
        }
        return clientPublicKey;
    }

    /**
     * 使用客户端公钥加密数据（使用UUID）
     * 服务端使用客户端的公钥加密数据，返回给客户端，客户端使用自己的私钥解密
     *
     * @param data 待加密的数据
     * @param uuid 客户端UUID标识
     * @return Base64编码的加密数据
     * @throws CodeException 加密失败时抛出异常
     */
    public String encrypt(String data, String uuid) throws CodeException {
        if (StringUtils.isBlank(data)) {
            throw new IllegalArgumentException("待加密数据不能为空");
        }
        if (StringUtils.isBlank(uuid)) {
            throw new IllegalArgumentException("UUID不能为空");
        }
        // 获取客户端公钥
        ClientPublicKey clientPublicKey = getClientPublicKey(uuid);
        if (clientPublicKey == null) {
            throw new CodeException(Error.RSA_CLIENT_PUBLIC_KEY_NOT_FOUND);
        }
        // 检查公钥是否即将过期
        EncryptConfigHandler.RsaConfig config = getRsaConfig();
        if (clientPublicKey.isExpiringSoon(config.getClientBufferMinutes())) {
            log.warn("客户端公钥即将过期，建议客户端更新公钥，UUID: {}, 过期时间: {}", uuid, clientPublicKey.getExpireTime());
        }
        try {
            // 使用客户端公钥加密
            String encrypted = RsaUtil.encrypt(data, clientPublicKey.getPublicKey());
            if (StringUtils.isBlank(encrypted)) {
                log.warn("加密结果为空，UUID: {}", uuid);
                throw new CodeException(Error.RSA_ENCRYPT_FAILED);
            }
            return encrypted;
        } catch (IllegalArgumentException e) {
            log.error("RSA密钥格式错误，加密失败，UUID: {}, 错误: {}", uuid, e.getMessage());
            throw new CodeException(Error.RSA_KEY_FORMAT_ERROR);
        } catch (Exception e) {
            log.error("使用客户端公钥加密失败，UUID: {}, 错误: {}", uuid, e.getMessage(), e);
            throw new CodeException(Error.RSA_ENCRYPT_FAILED);
        }
    }

    /**
     * 检查客户端公钥是否存在且有效
     *
     * @param uuid 客户端标识
     * @return true表示存在且有效，false表示不存在或已过期
     */
    public boolean hasValidClientPublicKey(String uuid) {
        ClientPublicKey clientPublicKey = getClientPublicKey(uuid);
        return clientPublicKey != null && !clientPublicKey.isExpired();
    }
}
