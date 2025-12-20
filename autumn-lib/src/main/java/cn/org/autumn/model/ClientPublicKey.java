package cn.org.autumn.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 客户端公钥信息
 * 用于存储客户端生成的RSA公钥
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientPublicKey implements Serializable {
    /**
     * 客户端标识（Session ID或客户端ID）
     */
    private String uuid;
    
    /**
     * 客户端公钥（Base64编码）
     */
    private String publicKey;
    
    /**
     * 公钥过期时间戳（毫秒）
     * 客户端应定期更新公钥
     */
    private Long expireTime;
    
    /**
     * 公钥创建时间戳（毫秒）
     */
    private Long createTime;

    public ClientPublicKey(String uuid, String publicKey) {
        this.uuid = uuid;
        this.publicKey = publicKey;
        this.createTime = System.currentTimeMillis();
    }

    /**
     * 检查公钥是否已过期
     *
     * @return true表示已过期，false表示未过期
     */
    public boolean isExpired() {
        return expireTime != null && System.currentTimeMillis() > expireTime;
    }

    /**
     * 检查公钥是否即将过期
     *
     * @param bufferMinutes 缓冲时间（分钟）
     * @return true表示即将过期，false表示未过期
     */
    public boolean isExpiringSoon(int bufferMinutes) {
        if (expireTime == null) {
            return false;
        }
        long bufferMillis = bufferMinutes * 60 * 1000L;
        return System.currentTimeMillis() > (expireTime - bufferMillis);
    }
}
