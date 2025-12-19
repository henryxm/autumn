package cn.org.autumn.model;

import lombok.Data;

import java.io.Serializable;

/**
 * AES密钥响应
 * 包含加密后的AES密钥和向量信息
 *
 * @author Autumn
 */
@Data
public class AesKeyResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * UUID
     */
    private String uuid;

    /**
     * 加密后的AES密钥（使用客户端公钥RSA加密）
     */
    private String key;

    /**
     * 加密后的AES向量（使用客户端公钥RSA加密）
     */
    private String vector;

    /**
     * AES密钥过期时间（时间戳，毫秒）
     */
    private Long expireTime;

    /**
     * 消息提示
     */
    private String message;
}
