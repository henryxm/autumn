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
    private String session;

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
     * 检查密钥是否已过期
     *
     * @return true-已过期，false-未过期
     */
    public boolean expired() {
        if (expireTime == null) {
            return false;
        }
        return System.currentTimeMillis() > expireTime;
    }

    /**
     * 检查密钥是否即将过期（用于客户端提前刷新）
     * 默认提前5分钟刷新
     *
     * @return true-即将过期，false-未即将过期
     */
    public boolean expiring() {
        return expiring(10);
    }

    /**
     * 检查密钥是否即将过期（用于客户端提前刷新）
     *
     * @param bufferMinutes 缓冲时间（分钟）
     * @return true-即将过期，false-未即将过期
     */
    public boolean expiring(int bufferMinutes) {
        if (expireTime == null) {
            return false;
        }
        long bufferMillis = bufferMinutes * 60 * 1000L;
        return System.currentTimeMillis() > (expireTime - bufferMillis);
    }
}
