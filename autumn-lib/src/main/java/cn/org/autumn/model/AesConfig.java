package cn.org.autumn.model;

import cn.org.autumn.annotation.ConfigField;
import cn.org.autumn.annotation.ConfigParam;
import cn.org.autumn.config.EncryptConfigHandler;
import cn.org.autumn.config.InputType;

import java.io.Serializable;

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
    private int keyValidMinutes = 60;

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
}

