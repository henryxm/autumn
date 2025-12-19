package cn.org.autumn.model;

import java.io.Serializable;

public interface Decrypt extends Serializable {
    String getDecrypt();

    /**
     * 客户端UUID标识
     * 客户端生成并存储，用于关联密钥对
     */
    String getUuid();
}
