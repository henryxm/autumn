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

    /**
     * 获取RSA配置
     * 优先从配置处理器获取，如果没有则使用默认配置
     *
     * @return RSA配置对象
     */
    public EncryptConfigHandler.RsaConfig getRsaConfig() {
        if (handlers != null && !handlers.isEmpty()) {
            for (EncryptConfigHandler handler : handlers) {
                EncryptConfigHandler.RsaConfig config = handler.getRsaConfig();
                if (config != null) {
                    return config;
                }
            }
        }
        return new EncryptConfigHandler.RsaConfig() {
        };
    }

    /**
     * 获取AES配置
     * 优先从配置处理器获取，如果没有则使用默认配置
     *
     * @return AES配置对象
     */
    public EncryptConfigHandler.AesConfig getAesConfig() {
        for (EncryptConfigHandler handler : handlers) {
            EncryptConfigHandler.AesConfig config = handler.getAesConfig();
            if (config != null) {
                return config;
            }
        }
        return new EncryptConfigHandler.AesConfig() {
        };
    }
}

