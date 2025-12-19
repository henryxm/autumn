package cn.org.autumn.service;

import cn.org.autumn.config.CacheConfig;
import cn.org.autumn.exception.CodeException;
import cn.org.autumn.model.EncryptSession;
import cn.org.autumn.model.KeyPair;
import cn.org.autumn.utils.RsaUtil;
import cn.org.autumn.utils.Uuid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RsaService {

    @Autowired
    CacheService cacheService;

    /**
     * 密钥对有效期（分钟），默认24小时
     */
    private static final int KEY_PAIR_VALID_MINUTES = 24 * 60;

    /**
     * 服务端冗余保留时间（分钟），默认10分钟
     * 密钥对过期后，服务端仍保留此时间，用于处理正在传输的加密数据
     */
    private static final int SERVER_BUFFER_MINUTES = 10;

    /**
     * 客户端建议提前刷新时间（分钟），默认5分钟
     * 客户端应在此时间之前重新获取新的密钥对
     */
    private static final int CLIENT_BUFFER_MINUTES = 5;

    /**
     * 缓存配置
     * 缓存过期时间 = 密钥对有效期 + 服务端冗余保留时间
     * 确保在密钥对过期后，服务端仍能解密正在传输的数据
     */
    private static final CacheConfig config = CacheConfig.builder()
            .cacheName("RsaServiceCache")
            .keyType(String.class)
            .valueType(KeyPair.class)
            .expireTime(KEY_PAIR_VALID_MINUTES + SERVER_BUFFER_MINUTES)
            .timeUnit(TimeUnit.MINUTES)
            .build();

    /**
     * 生成新的密钥对
     *
     * @param session Session ID
     * @return 包含过期时间的密钥对
     */
    public KeyPair generate(String session) {
        KeyPair pair = RsaUtil.generate();
        pair.setSession(session);
        // 设置过期时间：当前时间 + 密钥对有效期
        long expireTime = System.currentTimeMillis() + (KEY_PAIR_VALID_MINUTES * 60 * 1000L);
        pair.setExpireTime(expireTime);
        if (log.isDebugEnabled()) {
            log.debug("生成新的密钥对，Session: {}, 过期时间: {}", session, expireTime);
        }
        return pair;
    }

    /**
     * 获取密钥对（如果不存在则生成新的）
     * 返回的密钥对包含过期时间，客户端应在此时间之前重新获取
     *
     * @param session HTTP Session
     * @return 包含过期时间的密钥对
     */
    public KeyPair getKeyPair(HttpSession session) {
        String id = null != session ? session.getId() : Uuid.uuid();
        KeyPair pair = cacheService.compute(id, () -> generate(id), config);
        // 如果密钥对即将过期，生成新的密钥对
        if (pair.isExpiringSoon(CLIENT_BUFFER_MINUTES)) {
            if (log.isDebugEnabled()) {
                log.debug("密钥对即将过期，生成新的密钥对，Session: {}", id);
            }
            pair = generate(id);
            // 更新缓存
            cacheService.put(config.getCacheName(), id, pair);
        }
        if (null != session) {
            // 将私钥存储到session中，用于后续解密
            session.setAttribute("rsa_private_key", pair.getPrivateKey());
            // 设置Session过期时间 = 密钥对有效期 + 服务端冗余保留时间
            session.setMaxInactiveInterval((KEY_PAIR_VALID_MINUTES + SERVER_BUFFER_MINUTES) * 60);
        }
        return pair;
    }

    /**
     * 获取客户端建议的刷新时间（毫秒时间戳）
     * 客户端应在此时间之前重新获取新的密钥对
     *
     * @param expireTime 密钥对过期时间
     * @return 建议刷新时间
     */
    public long getSuggestedRefreshTime(Long expireTime) {
        if (expireTime == null) {
            return System.currentTimeMillis() + (KEY_PAIR_VALID_MINUTES - CLIENT_BUFFER_MINUTES) * 60 * 1000L;
        }
        return expireTime - (CLIENT_BUFFER_MINUTES * 60 * 1000L);
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
    public String decrypt(EncryptSession value, HttpSession session) throws CodeException {
        if (StringUtils.isNotBlank(value.getEncrypt())) {
            String privateKey = null;
            KeyPair keyPair = null;
            // 1. 优先从Session中获取私钥
            if (null != session) {
                privateKey = (String) session.getAttribute("rsa_private_key");
            }
            // 2. 如果Session中没有，从缓存中获取
            if (null == privateKey && StringUtils.isNotBlank(value.getSession())) {
                keyPair = cacheService.get(config.getCacheName(), value.getSession());
                if (null != keyPair) {
                    privateKey = keyPair.getPrivateKey();
                    // 检查密钥对是否已过期（但仍在服务端冗余保留时间内）
                    if (keyPair.isExpired()) {
                        log.warn("使用已过期的密钥对进行解密，Session: {}, 过期时间: {}", value.getSession(), keyPair.getExpireTime());
                    }
                    // 3. 服务器无状态，可能重启，从缓存获取后重新写入Session
                    if (null != session && null != privateKey) {
                        session.setAttribute("rsa_private_key", privateKey);
                        // 计算Session过期时间：密钥对过期时间 + 服务端冗余保留时间
                        if (keyPair.getExpireTime() != null) {
                            long sessionExpireTime = keyPair.getExpireTime() + (SERVER_BUFFER_MINUTES * 60 * 1000L);
                            long currentTime = System.currentTimeMillis();
                            // 计算剩余秒数（Session过期时间 - 当前时间）
                            int remainingSeconds = (int) ((sessionExpireTime - currentTime) / 1000);
                            if (remainingSeconds > 0) {
                                session.setMaxInactiveInterval(remainingSeconds);
                                if (log.isDebugEnabled()) {
                                    log.debug("从缓存恢复私钥到Session，Session: {}, 剩余有效时间: {}秒", value.getSession(), remainingSeconds);
                                }
                            } else {
                                log.warn("密钥对已完全过期，无法设置Session过期时间，Session: {}", value.getSession());
                            }
                        }
                    }
                }
            }
            // 4. 如果仍然没有找到私钥，抛出异常
            if (StringUtils.isBlank(privateKey)) {
                throw new CodeException("私钥不存在，可能密钥对已过期或Session无效", 10001);
            }
            // 5. 执行解密
            try {
                return RsaUtil.decrypt(value.getEncrypt(), privateKey);
            } catch (Exception e) {
                log.error("解密失败，Session: {}, 错误: {}", value.getSession(), e.getMessage());
                throw new CodeException("解密失败: " + e.getMessage(), 10002);
            }
        }
        return "";
    }
}
