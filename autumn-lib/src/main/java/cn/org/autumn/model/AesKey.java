package cn.org.autumn.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * AES密钥信息
 * 包含密钥、向量和过期时间
 *
 * @author Autumn
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AesKey implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 客户端UUID标识
     */
    private String uuid;

    /**
     * AES密钥（Base64编码）
     */
    private String key;

    /**
     * AES向量（Base64编码）
     */
    private String vector;

    /**
     * 过期时间（时间戳，毫秒）
     */
    private Long expireTime;

    /**
     * 创建时间（时间戳，毫秒）
     */
    private Long createTime;

    /**
     * 检查密钥是否已过期
     *
     * @return true-已过期，false-未过期
     */
    public boolean isExpired() {
        if (expireTime == null) {
            return false;
        }
        return System.currentTimeMillis() > expireTime;
    }

    /**
     * 检查密钥是否即将过期
     * 在过期前5分钟认为即将过期
     *
     * @return true-即将过期，false-未即将过期
     */
    public boolean isExpiringSoon() {
        if (expireTime == null) {
            return false;
        }
        long bufferTime = 5 * 60 * 1000; // 5分钟
        return System.currentTimeMillis() > (expireTime - bufferTime);
    }
}
