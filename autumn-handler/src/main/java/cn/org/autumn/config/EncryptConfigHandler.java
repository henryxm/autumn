package cn.org.autumn.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * 加密配置处理器接口
 * 用于自定义RSA和AES的配置参数
 * 
 * @author Autumn
 */
@Component
@ConditionalOnMissingBean(EncryptConfigHandler.class)
public interface EncryptConfigHandler {

    /**
     * 获取RSA配置
     * 
     * @return RSA配置对象，如果返回null则使用默认配置
     */
    default EncryptConfigHandler.RsaConfig getRsaConfig() {
        return null;
    }

    /**
     * 获取AES配置
     * 
     * @return AES配置对象，如果返回null则使用默认配置
     */
    default EncryptConfigHandler.AesConfig getAesConfig() {
        return null;
    }

    /**
     * RSA配置接口
     */
    interface RsaConfig {
        /**
         * 密钥对有效期（分钟），默认24小时
         */
        default int getKeyPairValidMinutes() {
            return 24 * 60;
        }

        /**
         * 服务端冗余保留时间（分钟），默认10分钟
         * 密钥对过期后，服务端仍保留此时间，用于处理正在传输的加密数据
         */
        default int getServerBufferMinutes() {
            return 10;
        }

        /**
         * 客户端建议提前刷新时间（分钟），默认5分钟
         * 客户端应在此时间之前重新获取新的密钥对
         */
        default int getClientBufferMinutes() {
            return 10;
        }

        /**
         * 客户端公钥有效期（分钟），默认7天
         */
        default int getClientPublicKeyValidMinutes() {
            return 7 * 24 * 60;
        }

        /**
         * RSA密钥长度（位），默认1024位
         */
        default int getKeySize() {
            return 1024;
        }
    }

    /**
     * AES配置接口
     */
    interface AesConfig {
        /**
         * AES密钥有效期（分钟），默认1小时
         */
        default int getKeyValidMinutes() {
            return 60;
        }

        /**
         * 服务端冗余保留时间（分钟），默认10分钟
         * 密钥过期后，服务端仍保留此时间，用于处理正在传输的加密数据
         */
        default int getServerBufferMinutes() {
            return 10;
        }

        /**
         * 客户端建议提前刷新时间（分钟），默认5分钟
         * 客户端应在此时间之前重新获取新的密钥
         */
        default int getClientBufferMinutes() {
            return 10;
        }

        /**
         * AES密钥长度（位），默认256位
         */
        default int getKeySize() {
            return 256;
        }

        /**
         * AES向量长度（字节），固定16字节（128位）
         */
        default int getIvSize() {
            return 16;
        }
    }
}

