package cn.org.autumn.model;

import cn.org.autumn.annotation.ConfigField;
import cn.org.autumn.annotation.ConfigParam;
import cn.org.autumn.config.EncryptConfigHandler;
import cn.org.autumn.config.InputType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * RSA加密配置
 * 
 * @author Autumn
 */
@ConfigParam(paramKey = "RSA_CONFIG", category = RsaConfig.config, name = "RSA加密配置", description = "配置RSA密钥对的有效期、密钥长度等参数")
public class RsaConfig implements EncryptConfigHandler.RsaConfig, Serializable {
    private static final long serialVersionUID = 1L;

    public static final String config = "rsa_config";

    /**
     * 密钥对有效期（分钟），默认24小时
     */
    @ConfigField(category = InputType.NumberType, name = "密钥对有效期（分钟）", description = "RSA密钥对的有效期，默认1440分钟（24小时）")
    private int keyPairValidMinutes = 24 * 60;

    /**
     * 服务端冗余保留时间（分钟），默认10分钟
     */
    @ConfigField(category = InputType.NumberType, name = "服务端冗余保留时间（分钟）", description = "密钥对过期后，服务端仍保留此时间，用于处理正在传输的加密数据，默认10分钟")
    private int serverBufferMinutes = 10;

    /**
     * 客户端建议提前刷新时间（分钟），默认5分钟
     */
    @ConfigField(category = InputType.NumberType, name = "客户端提前刷新时间（分钟）", description = "客户端应在此时间之前重新获取新的密钥对，默认10分钟")
    private int clientBufferMinutes = 10;

    /**
     * 客户端公钥有效期（分钟），默认7天
     */
    @ConfigField(category = InputType.NumberType, name = "客户端公钥有效期（分钟）", description = "客户端公钥的有效期，默认10080分钟（7天）")
    private int clientPublicKeyValidMinutes = 7 * 24 * 60;

    /**
     * RSA密钥长度（位），默认1024位
     */
    @ConfigField(category = InputType.NumberType, name = "RSA密钥长度（位）", description = "RSA密钥对的长度，默认1024位，可选值：1024、2048、4096")
    private int keySize = 1024;

    public int getKeyPairValidMinutes() {
        return keyPairValidMinutes;
    }

    public void setKeyPairValidMinutes(int keyPairValidMinutes) {
        this.keyPairValidMinutes = keyPairValidMinutes;
    }

    public int getServerBufferMinutes() {
        return serverBufferMinutes;
    }

    public void setServerBufferMinutes(int serverBufferMinutes) {
        this.serverBufferMinutes = serverBufferMinutes;
    }

    public int getClientBufferMinutes() {
        return clientBufferMinutes;
    }

    public void setClientBufferMinutes(int clientBufferMinutes) {
        this.clientBufferMinutes = clientBufferMinutes;
    }

    public int getClientPublicKeyValidMinutes() {
        return clientPublicKeyValidMinutes;
    }

    public void setClientPublicKeyValidMinutes(int clientPublicKeyValidMinutes) {
        this.clientPublicKeyValidMinutes = clientPublicKeyValidMinutes;
    }

    public int getKeySize() {
        return keySize;
    }

    public void setKeySize(int keySize) {
        this.keySize = keySize;
    }

    /**
     * 校验并修正配置参数
     * 如果值不合理，则使用默认值
     *
     * @return 修正信息列表，如果为空则表示没有进行修正
     */
    public List<String> validateAndFix() {
        List<String> fixes = new ArrayList<>();
        // RSA密钥长度必须是1024、2048、4096等有效值，且至少为1024
        if (keySize < 1024 || (keySize % 1024 != 0) || keySize > 8192) {
            int oldValue = keySize;
            keySize = 1024; // 默认值
            fixes.add(String.format("RSA密钥长度不合理: %d位，已修正为默认值: %d位", oldValue, keySize));
        }
        // 密钥对有效期必须大于0
        if (keyPairValidMinutes <= 0) {
            int oldValue = keyPairValidMinutes;
            keyPairValidMinutes = 24 * 60; // 默认24小时
            fixes.add(String.format("RSA密钥对有效期不合理: %d分钟，已修正为默认值: %d分钟", oldValue, keyPairValidMinutes));
        }
        // 服务端冗余保留时间必须大于等于0
        if (serverBufferMinutes < 0) {
            int oldValue = serverBufferMinutes;
            serverBufferMinutes = 10; // 默认10分钟
            fixes.add(String.format("RSA服务端冗余保留时间不合理: %d分钟，已修正为默认值: %d分钟", oldValue, serverBufferMinutes));
        }
        // 客户端提前刷新时间必须大于等于0
        if (clientBufferMinutes < 0) {
            int oldValue = clientBufferMinutes;
            clientBufferMinutes = 10; // 默认10分钟
            fixes.add(String.format("RSA客户端提前刷新时间不合理: %d分钟，已修正为默认值: %d分钟", oldValue, clientBufferMinutes));
        }
        // 客户端公钥有效期必须大于0
        if (clientPublicKeyValidMinutes <= 0) {
            int oldValue = clientPublicKeyValidMinutes;
            clientPublicKeyValidMinutes = 7 * 24 * 60; // 默认7天
            fixes.add(String.format("RSA客户端公钥有效期不合理: %d分钟，已修正为默认值: %d分钟", oldValue, clientPublicKeyValidMinutes));
        }
        return fixes;
    }
}

