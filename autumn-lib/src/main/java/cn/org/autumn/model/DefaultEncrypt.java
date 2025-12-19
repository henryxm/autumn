package cn.org.autumn.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DefaultEncrypt implements Encrypt {
    private String encrypt;
    /**
     * 客户端UUID标识
     * 客户端生成并存储，用于关联密钥对
     */
    private String uuid;
}