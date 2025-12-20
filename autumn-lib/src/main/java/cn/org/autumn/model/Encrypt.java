package cn.org.autumn.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public interface Encrypt extends Serializable {
    String getEncrypt();

    /**
     * 客户端UUID标识
     * 客户端生成并存储，用于关联密钥对
     */
    String getUuid();
}