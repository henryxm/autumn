package cn.org.autumn.model;

import java.io.Serializable;

public interface Encrypt extends Serializable {
    default String getCiphertext() {
        return "";
    }

    default String getAlgorithm() {
        return "AES";
    }

    /**
     * 客户端UUID标识
     * 客户端生成并存储，用于关联密钥对
     */
    default String getSession() {
        return "";
    }
}