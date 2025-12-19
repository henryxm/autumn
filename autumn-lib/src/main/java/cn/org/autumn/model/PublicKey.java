package cn.org.autumn.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PublicKey implements Serializable {
    String publicKey;
    /**
     * 客户端UUID标识
     * 客户端生成并存储，用于关联密钥对
     */
    String uuid;
    /**
     * 密钥对过期时间戳（毫秒）
     * 客户端应在此时间之前重新获取新的密钥对
     * 建议提前5分钟刷新，避免在传输过程中密钥过期
     */
    Long expireTime;
}
