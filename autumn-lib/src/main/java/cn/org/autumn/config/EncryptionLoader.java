package cn.org.autumn.config;

import cn.org.autumn.model.AesKey;
import cn.org.autumn.model.RsaKey;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * 加密密钥加载器接口
 * 用于从数据库加载加密密钥信息
 * 当缓存和Redis中都未获取到密钥时，可以通过此接口从数据库加载
 *
 * @author Autumn
 */
@Component
@ConditionalOnMissingBean(EncryptionLoader.class)
public interface EncryptionLoader {

    /**
     * 从数据库加载AES密钥
     *
     * @param session 客户端会话ID
     * @return AES密钥对象，如果不存在则返回null
     */
    default AesKey loadAes(String session) {
        return null;
    }

    /**
     * 从数据库加载RSA密钥对
     *
     * @param session 客户端会话ID
     * @return RSA密钥对对象，如果不存在则返回null
     */
    default RsaKey loadRsa(String session) {
        return null;
    }

    /**
     * 从数据库加载客户端公钥
     *
     * @param session 客户端会话ID
     * @return 客户端公钥字符串，如果不存在则返回null
     */
    default String loadClient(String session) {
        return null;
    }
}
