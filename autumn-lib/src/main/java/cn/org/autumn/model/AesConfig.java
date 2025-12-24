package cn.org.autumn.model;

import cn.org.autumn.annotation.ConfigField;
import cn.org.autumn.annotation.ConfigParam;
import cn.org.autumn.config.EncryptConfigHandler;
import cn.org.autumn.config.InputType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * AES加密配置
 *
 * @author Autumn
 */
@ConfigParam(paramKey = "AES_CONFIG", category = AesConfig.config, name = "AES加密配置", description = "配置AES密钥的有效期、密钥长度等参数")
public class AesConfig implements EncryptConfigHandler.AesConfig, Serializable {
    private static final long serialVersionUID = 1L;

    public static final String config = "aes_config";

    /**
     * AES密钥有效期（分钟），默认1小时
     */
    @ConfigField(category = InputType.NumberType, name = "AES密钥有效期（分钟）", description = "AES密钥的有效期，默认60分钟（1小时）")
    private int keyValidMinutes = 24 * 60;

    /**
     * 服务端冗余保留时间（分钟），默认10分钟
     */
    @ConfigField(category = InputType.NumberType, name = "服务端冗余保留时间（分钟）", description = "密钥过期后，服务端仍保留此时间，用于处理正在传输的加密数据，默认10分钟")
    private int serverBufferMinutes = 60;

    /**
     * 客户端建议提前刷新时间（分钟），默认5分钟
     */
    @ConfigField(category = InputType.NumberType, name = "客户端提前刷新时间（分钟）", description = "客户端应在此时间之前重新获取新的密钥，默认5分钟")
    private int clientBufferMinutes = 60;

    /**
     * AES密钥长度（位），默认256位
     */
    @ConfigField(category = InputType.NumberType, name = "AES密钥长度（位）", description = "AES密钥的长度，默认256位，可选值：128、192、256")
    private int keySize = 256;

    /**
     * AES向量长度（字节），固定16字节（128位）
     */
    @ConfigField(category = InputType.NumberType, name = "AES向量长度（字节）", description = "AES向量的长度，固定16字节（128位）")
    private int ivSize = 16;

    public int getKeyValidMinutes() {
        return keyValidMinutes;
    }

    public void setKeyValidMinutes(int keyValidMinutes) {
        this.keyValidMinutes = keyValidMinutes;
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

    public int getKeySize() {
        return keySize;
    }

    public void setKeySize(int keySize) {
        this.keySize = keySize;
    }

    public int getIvSize() {
        return ivSize;
    }

    public void setIvSize(int ivSize) {
        this.ivSize = ivSize;
    }

    /**
     * 校验并修正配置参数
     * 如果值不合理，则使用默认值
     *
     * @return 修正信息列表，如果为空则表示没有进行修正
     */
    public List<String> validateAndFix() {
        List<String> fixes = new ArrayList<>();
        // AES密钥长度必须是128、192、256位
        if (keySize != 128 && keySize != 192 && keySize != 256) {
            int oldValue = keySize;
            keySize = 256; // 默认值
            fixes.add(String.format("AES密钥长度不合理: %d位，已修正为默认值: %d位", oldValue, keySize));
        }
        // AES向量长度固定为16字节（128位）
        if (ivSize != 16 && ivSize != 0) {
            int oldValue = ivSize;
            ivSize = 16; // 固定值
            fixes.add(String.format("AES向量长度不合理: %d字节，已修正为固定值: %d字节", oldValue, ivSize));
        }
        // 密钥有效期必须大于0
        if (keyValidMinutes <= 0) {
            int oldValue = keyValidMinutes;
            keyValidMinutes = 24 * 60; // 默认24小时
            fixes.add(String.format("AES密钥有效期不合理: %d分钟，已修正为默认值: %d分钟", oldValue, keyValidMinutes));
        }
        // 服务端冗余保留时间必须大于等于0
        if (serverBufferMinutes < 0) {
            int oldValue = serverBufferMinutes;
            serverBufferMinutes = 60; // 默认60分钟
            fixes.add(String.format("AES服务端冗余保留时间不合理: %d分钟，已修正为默认值: %d分钟", oldValue, serverBufferMinutes));
        }
        // 客户端提前刷新时间必须大于等于0
        if (clientBufferMinutes < 0) {
            int oldValue = clientBufferMinutes;
            clientBufferMinutes = 60; // 默认60分钟
            fixes.add(String.format("AES客户端提前刷新时间不合理: %d分钟，已修正为默认值: %d分钟", oldValue, clientBufferMinutes));
        }
        return fixes;
    }
}

