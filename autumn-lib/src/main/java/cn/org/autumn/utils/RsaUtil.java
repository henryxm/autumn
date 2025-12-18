package cn.org.autumn.utils;

import cn.org.autumn.model.KeyPair;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA加密解密工具类
 * 用于前端密码加密，后端密码解密
 */
@Slf4j
public class RsaUtil {

    /**
     * RSA密钥长度，默认1024位
     */
    private static final int KEY_SIZE = 1024;

    /**
     * 密钥算法
     */
    private static final String KEY_ALGORITHM = "RSA";

    /**
     * 填充方式
     */
    private static final String TRANSFORMATION = "RSA/ECB/PKCS1Padding";

    /**
     * 生成RSA密钥对
     *
     * @return 包含公钥和私钥的Map，key分别为"publicKey"和"privateKey"
     */
    public static KeyPair generate() {
        try {
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            keyPairGen.initialize(KEY_SIZE, new SecureRandom());
            java.security.KeyPair keyPair = keyPairGen.generateKeyPair();
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
            // Base64编码后的公钥和私钥
            return new KeyPair(Base64.getEncoder().encodeToString(publicKey.getEncoded()), Base64.getEncoder().encodeToString(privateKey.getEncoded()));
        } catch (Exception e) {
            log.error("生成密钥: {}", e.getMessage());
            throw new RuntimeException("生成RSA密钥对失败", e);
        }
    }

    /**
     * 使用公钥加密
     *
     * @param data      待加密数据
     * @param publicKey Base64编码的公钥
     * @return Base64编码的加密数据
     */
    public static String encrypt(String data, String publicKey) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(publicKey);
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            PublicKey pubKey = keyFactory.generatePublic(x509KeySpec);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);
            // RSA加密有长度限制，需要分段加密
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            int inputLen = dataBytes.length;
            int maxBlockSize = KEY_SIZE / 8 - 11; // 最大加密块长度
            int offSet = 0;
            byte[] cache;
            int i = 0;
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            while (inputLen - offSet > 0) {
                if (inputLen - offSet > maxBlockSize) {
                    cache = cipher.doFinal(dataBytes, offSet, maxBlockSize);
                } else {
                    cache = cipher.doFinal(dataBytes, offSet, inputLen - offSet);
                }
                out.write(cache, 0, cache.length);
                i++;
                offSet = i * maxBlockSize;
            }
            byte[] encryptedData = out.toByteArray();
            out.close();

            return Base64.getEncoder().encodeToString(encryptedData);
        } catch (Exception e) {
            log.error("加密失败: {}", e.getMessage());
            throw new RuntimeException("RSA公钥加密失败", e);
        }
    }

    /**
     * 使用私钥解密
     *
     * @param data       Base64编码的加密数据
     * @param privateKey Base64编码的私钥
     * @return 解密后的原始数据
     */
    public static String decrypt(String data, String privateKey) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(privateKey);
            PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            PrivateKey priKey = keyFactory.generatePrivate(pkcs8KeySpec);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, priKey);
            // RSA解密需要分段解密
            byte[] encryptedBytes = Base64.getDecoder().decode(data);
            int inputLen = encryptedBytes.length;
            int maxBlockSize = KEY_SIZE / 8; // 最大解密块长度
            int offSet = 0;
            byte[] cache;
            int i = 0;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            while (inputLen - offSet > 0) {
                if (inputLen - offSet > maxBlockSize) {
                    cache = cipher.doFinal(encryptedBytes, offSet, maxBlockSize);
                } else {
                    cache = cipher.doFinal(encryptedBytes, offSet, inputLen - offSet);
                }
                out.write(cache, 0, cache.length);
                i++;
                offSet = i * maxBlockSize;
            }
            out.close();
            return out.toString("UTF-8");
        } catch (Exception e) {
            log.error("解密失败: {}", e.getMessage());
            throw new RuntimeException("RSA私钥解密失败", e);
        }
    }

    /**
     * 验证密钥对是否匹配
     *
     * @param publicKey  公钥
     * @param privateKey 私钥
     * @return 是否匹配
     */
    public static boolean verify(String publicKey, String privateKey) {
        try {
            String verify = "verify";
            String encrypted = encrypt(verify, publicKey);
            String decrypted = decrypt(encrypted, privateKey);
            return verify.equals(decrypted);
        } catch (Exception e) {
            log.error("验证密钥对失败: {}", e.getMessage());
            return false;
        }
    }

    public static void test(String[] args) {
        KeyPair pair = generate();
        boolean v = verify(pair.getPublicKey(), pair.getPrivateKey());
        log.info("测试结果:{}", v);
    }
}

