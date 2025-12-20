package cn.org.autumn.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

/**
 * 公钥数据包装类
 * 用于封装解析后的公钥字节数组和格式信息
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KeyData {
    /**
     * 公钥字节数组（Base64解码后）
     */
    private final byte[] keyBytes;

    /**
     * 是否为PKCS#1格式
     * true: PKCS#1格式（-----BEGIN RSA PUBLIC KEY-----）
     * false: PKCS#8格式（X509，-----BEGIN PUBLIC KEY-----）
     */
    private final boolean isPKCS1;

    /**
     * 构造函数
     *
     * @param keyBytes 公钥字节数组
     * @param isPKCS1  是否为PKCS#1格式
     */
    public KeyData(byte[] keyBytes, boolean isPKCS1) {
        this.keyBytes = keyBytes;
        this.isPKCS1 = isPKCS1;
    }
}
