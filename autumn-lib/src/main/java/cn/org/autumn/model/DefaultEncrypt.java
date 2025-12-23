package cn.org.autumn.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DefaultEncrypt implements Encrypt {
    /**
     * 别加密数据的密文
     */
    private String ciphertext;
    /**
     * 使用的加密算法
     */
    private String algorithm;
    /**
     * 客户端UUID标识
     * 客户端生成并存储，用于关联密钥对
     */
    private String session;
}