package cn.org.autumn.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RsaKey implements Serializable {
    /**
     * 客户端UUID标识
     * 客户端生成并存储，用于关联密钥对
     */
    String session;
    /**
     * 公钥，后台生成，发送给客户端
     */
    String publicKey;
    /**
     * 私钥，后台生成，保存在后台，用于解密
     */
    String privateKey;
    /**
     * 密钥对过期时间戳（毫秒）
     * 客户端应在此时间之前重新获取新的密钥对
     */
    Long expireTime;

    public RsaKey(String publicKey, String privateKey) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    public RsaKey(String publicKey, String privateKey, String session) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.session = session;
    }

    public RsaKey(String session, String publicKey, Long expireTime) {
        this.session = session;
        this.publicKey = publicKey;
        this.expireTime = expireTime;
    }

    /**
     * 转换为PublicKey对象（用于返回给客户端）
     *
     * @return PublicKey对象
     */
    public RsaKey copy() {
        return new RsaKey(session, publicKey, expireTime);
    }

    /**
     * 检查密钥对是否已过期
     *
     * @return true表示已过期，false表示未过期
     */
    public boolean isExpired() {
        return expireTime != null && System.currentTimeMillis() > expireTime;
    }

    public boolean isExpiringSoon() {
        return isExpiringSoon(10);
    }

    /**
     * 检查密钥对是否即将过期（用于客户端提前刷新）
     * 默认提前5分钟刷新
     *
     * @param bufferMinutes 缓冲时间（分钟），默认5分钟
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