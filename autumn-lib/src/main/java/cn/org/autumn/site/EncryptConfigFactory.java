package cn.org.autumn.site;

import cn.org.autumn.config.EncryptConfigHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 加密配置工厂类
 * 管理RSA和AES的配置参数
 * 
 * @author Autumn
 */
@Slf4j
@Component
public class EncryptConfigFactory extends Factory {

    @Autowired(required = false)
    private List<EncryptConfigHandler> handlers;

    private EncryptConfigHandler.RsaConfig rsaConfig;
    private EncryptConfigHandler.AesConfig aesConfig;

    /**
     * 获取RSA配置
     * 优先从配置处理器获取，如果没有则使用默认配置
     * 
     * @return RSA配置对象
     */
    public EncryptConfigHandler.RsaConfig getRsaConfig() {
        if (rsaConfig == null) {
            synchronized (this) {
                if (rsaConfig == null) {
                    if (handlers != null && !handlers.isEmpty()) {
                        for (EncryptConfigHandler handler : handlers) {
                            EncryptConfigHandler.RsaConfig config = handler.getRsaConfig();
                            if (config != null) {
                                rsaConfig = config;
                                if (log.isDebugEnabled()) {
                                    log.debug("使用自定义RSA配置: {}", handler.getClass().getName());
                                }
                                break;
                            }
                        }
                    }
                    // 如果没有自定义配置，使用默认配置
                    if (rsaConfig == null) {
                        rsaConfig = new EncryptConfigHandler.RsaConfig() {};
                        if (log.isDebugEnabled()) {
                            log.debug("使用默认RSA配置");
                        }
                    }
                }
            }
        }
        return rsaConfig;
    }

    /**
     * 获取AES配置
     * 优先从配置处理器获取，如果没有则使用默认配置
     * 
     * @return AES配置对象
     */
    public EncryptConfigHandler.AesConfig getAesConfig() {
        if (aesConfig == null) {
            synchronized (this) {
                if (aesConfig == null) {
                    if (handlers != null && !handlers.isEmpty()) {
                        for (EncryptConfigHandler handler : handlers) {
                            EncryptConfigHandler.AesConfig config = handler.getAesConfig();
                            if (config != null) {
                                aesConfig = config;
                                if (log.isDebugEnabled()) {
                                    log.debug("使用自定义AES配置: {}", handler.getClass().getName());
                                }
                                break;
                            }
                        }
                    }
                    // 如果没有自定义配置，使用默认配置
                    if (aesConfig == null) {
                        aesConfig = new EncryptConfigHandler.AesConfig() {};
                        if (log.isDebugEnabled()) {
                            log.debug("使用默认AES配置");
                        }
                    }
                }
            }
        }
        return aesConfig;
    }
}

