package cn.org.autumn.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
public class Crypto {
    /**
     * 解码Base64字符串，如果解码失败则返回原始字符串的字节数组
     * 自动检测是否为Base64编码
     * 对于密钥：支持16、24、32字节（128、192、256位）
     *
     * @param input 输入字符串（可能是Base64编码或普通字符串）
     * @return 解码后的字节数组
     */
    private static byte[] decodeBase64IfNeeded(String input) {
        if (StringUtils.isBlank(input)) {
            return new byte[0];
        }
        try {
            // 尝试Base64解码
            byte[] decoded = Base64.getDecoder().decode(input);
            // 如果解码成功且长度合理（AES密钥通常是16、24或32字节），返回解码结果
            if (decoded.length == 16 || decoded.length == 24 || decoded.length == 32) {
                return decoded;
            }
            // 如果解码后的长度不符合预期，可能是误判，使用原始字符串
            return input.getBytes(StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            // Base64解码失败，使用原始字符串
            return input.getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * 解码Base64向量字符串，确保返回16字节
     *
     * @param input Base64编码的向量字符串
     * @return 16字节的向量字节数组，如果解码失败或长度不正确返回null
     */
    private static byte[] decodeBase64Vector(String input) {
        if (StringUtils.isBlank(input)) {
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(input);
            if (decoded.length == 16) {
                return decoded;
            }
            log.error("向量长度错误: 必须是16字节，当前长度: {}", decoded.length);
            return null;
        } catch (IllegalArgumentException e) {
            // Base64解码失败，尝试使用原始字符串
            byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
            if (bytes.length == 16) {
                return bytes;
            }
            log.error("向量长度错误: 必须是16字节，当前长度: {}", bytes.length);
            return null;
        }
    }

    public static String encrypt(String content, String algorithm, String password, String vector) {
        try {
            if (StringUtils.isBlank(content))
                return "";
            if (StringUtils.isBlank(algorithm))
                algorithm = "AES";
            // 解码Base64编码的密钥和向量
            byte[] keyBytes = decodeBase64IfNeeded(password);
            byte[] ivBytes = StringUtils.isNotBlank(vector) ? decodeBase64Vector(vector) : null;
            // 验证向量长度（如果提供了向量但长度不正确，返回null）
            if (ivBytes == null && StringUtils.isNotBlank(vector)) {
                return null;
            }
            SecretKeySpec key = new SecretKeySpec(keyBytes, algorithm);
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            // 根据是否有向量选择加密模式
            Cipher cipher;
            if (ivBytes != null) {
                // 有向量：使用CBC模式
                cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                IvParameterSpec iv = new IvParameterSpec(ivBytes);
                cipher.init(Cipher.ENCRYPT_MODE, key, iv);
            } else {
                // 无向量：使用ECB模式（兼容旧代码）
                cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                cipher.init(Cipher.ENCRYPT_MODE, key);
            }
            byte[] result = cipher.doFinal(bytes);
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            if (log.isDebugEnabled())
                log.debug("加密失败:{}", e.getMessage(), e);
        }
        return "";
    }

    public static String decrypt(String content, String algorithm, String password, String vector) {
        try {
            if (StringUtils.isBlank(content))
                return "";
            byte[] bytes = Base64.getDecoder().decode(content);
            if (StringUtils.isBlank(algorithm))
                algorithm = "AES";
            // 解码Base64编码的密钥和向量
            byte[] keyBytes = decodeBase64IfNeeded(password);
            byte[] ivBytes = StringUtils.isNotBlank(vector) ? decodeBase64Vector(vector) : null;
            // 验证向量长度（如果提供了向量但长度不正确，返回空字符串）
            if (ivBytes == null && StringUtils.isNotBlank(vector)) {
                return "";
            }
            SecretKeySpec key = new SecretKeySpec(keyBytes, algorithm);
            // 根据是否有向量选择解密模式
            Cipher cipher;
            if (ivBytes != null) {
                // 有向量：使用CBC模式
                cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                IvParameterSpec iv = new IvParameterSpec(ivBytes);
                cipher.init(Cipher.DECRYPT_MODE, key, iv);
            } else {
                // 无向量：使用ECB模式（兼容旧代码）
                cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, key);
            }
            byte[] result = cipher.doFinal(bytes);
            return new String(result, StandardCharsets.UTF_8);
        } catch (Exception e) {
            if (log.isDebugEnabled())
                log.debug("解密失败:{}", e.getMessage(), e);
        }
        return "";
    }
}
