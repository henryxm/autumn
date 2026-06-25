package cn.org.autumn.modules.sys.config;

import cn.org.autumn.config.ClearHandler;
import cn.org.autumn.config.EncryptConfigHandler;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.sys.service.SysConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 默认加密配置处理器
 * 从SysConfigService读取配置
 *
 * @author Autumn
 */
@Slf4j
@Component
public class DefaultEncryptConfigHandler implements EncryptConfigHandler, ClearHandler, LoopJob.OneHour {

    @Autowired
    private SysConfigService sysConfigService;

    private cn.org.autumn.model.RsaConfig rsaConfig;

    private cn.org.autumn.model.AesConfig aesConfig;

    @Override
    public EncryptConfigHandler.RsaConfig getRsaConfig() {
        if (rsaConfig == null) {
            synchronized (this) {
                if (rsaConfig == null) {
                    try {
                        rsaConfig = sysConfigService.getConfigObjectValidate("RSA_CONFIG", cn.org.autumn.model.RsaConfig.class);
                        if (log.isDebugEnabled()) {
                            log.debug("Loaded RSA config: keyPairValidMinutes={}, serverBufferMinutes={}, clientBufferMinutes={}, clientPublicKeyValidMinutes={}, keySize={}", rsaConfig.getKeyValidMinutes(), rsaConfig.getServerBufferMinutes(), rsaConfig.getClientBufferMinutes(), rsaConfig.getClientPublicKeyValidMinutes(), rsaConfig.getKeySize());
                        }
                    } catch (Exception e) {
                        log.warn("Failed to load RSA config, using defaults: {}", e.getMessage());
                        rsaConfig = new cn.org.autumn.model.RsaConfig();
                    }
                }
            }
        }
        return rsaConfig;
    }

    @Override
    public EncryptConfigHandler.AesConfig getAesConfig() {
        if (aesConfig == null) {
            synchronized (this) {
                if (aesConfig == null) {
                    try {
                        aesConfig = sysConfigService.getConfigObjectValidate("AES_CONFIG", cn.org.autumn.model.AesConfig.class);
                        if (log.isDebugEnabled()) {
                            log.debug("Loaded AES config: keyValidMinutes={}, keySize={}, ivSize={}", aesConfig.getKeyValidMinutes(), aesConfig.getKeySize(), aesConfig.getIvSize());
                        }
                    } catch (Exception e) {
                        log.warn("Failed to load AES config, using defaults: {}", e.getMessage());
                        aesConfig = new cn.org.autumn.model.AesConfig();
                    }
                }
            }
        }
        return aesConfig;
    }

    @Override
    public void onOneHour() {
        clear();
    }

    @Override
    public void clear() {
        aesConfig = null;
        rsaConfig = null;
    }
}

