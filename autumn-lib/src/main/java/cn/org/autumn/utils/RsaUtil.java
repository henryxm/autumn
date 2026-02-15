package cn.org.autumn.utils;

import cn.org.autumn.model.KeyData;
import cn.org.autumn.model.RsaKey;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
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
    private static final int DEFAULT_KEY_SIZE = 1024;

    /**
     * 密钥算法
     */
    private static final String KEY_ALGORITHM = "RSA";

    /**
     * 填充方式
     */
    private static final String TRANSFORMATION = "RSA/ECB/PKCS1Padding";

    /**
     * 生成RSA密钥对（使用默认密钥长度1024位）
     *
     * @return 包含公钥和私钥的KeyPair对象
     */
    public static RsaKey generate() {
        return generate(DEFAULT_KEY_SIZE);
    }

    /**
     * 生成RSA密钥对（使用指定的密钥长度）
     *
     * @param keySize 密钥长度（位），支持1024、2048、4096等
     * @return 包含公钥和私钥的KeyPair对象
     */
    public static RsaKey generate(int keySize) {
        try {
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            keyPairGen.initialize(keySize, new SecureRandom());
            java.security.KeyPair keyPair = keyPairGen.generateKeyPair();
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
            // Base64编码后的公钥和私钥
            return new RsaKey(Base64.getEncoder().encodeToString(publicKey.getEncoded()), Base64.getEncoder().encodeToString(privateKey.getEncoded()));
        } catch (Exception e) {
            log.error("生成密钥: {}", e.getMessage());
            throw new RuntimeException("生成RSA密钥对失败", e);
        }
    }

    /**
     * 处理公钥字符串，支持PEM格式和纯Base64格式
     *
     * @param publicKey 公钥字符串（PEM格式或Base64格式）
     * @return Base64解码后的公钥字节数组和是否为PKCS#1格式的标识
     */
    private static KeyData parsePublicKey(String publicKey) {
        if (publicKey == null || publicKey.trim().isEmpty()) {
            throw new IllegalArgumentException("公钥不能为空");
        }
        String key = publicKey.trim();
        boolean isPKCS1 = false;
        // 检测是否为PEM格式
        if (key.contains("-----BEGIN")) {
            // 检测是否为PKCS#1格式（-----BEGIN RSA PUBLIC KEY-----）
            if (key.contains("-----BEGIN RSA PUBLIC KEY-----")) {
                isPKCS1 = true;
            }
            // 移除 PEM 格式的头尾标识
            key = key.replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----BEGIN RSA PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replace("-----END RSA PUBLIC KEY-----", "")
                    .replaceAll("\\s", ""); // 移除所有空白字符（空格、换行等）
        }
        try {
            byte[] keyBytes = Base64.getDecoder().decode(key);
            return new KeyData(keyBytes, isPKCS1);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("公钥Base64格式错误: " + e.getMessage(), e);
        }
    }

    /**
     * 使用公钥加密
     *
     * @param data      待加密数据
     * @param publicKey Base64编码的公钥（支持PEM格式和纯Base64格式）
     * @return Base64编码的加密数据
     */
    public static String encrypt(String data, String publicKey) {
        try {
            if (StringUtils.isBlank(data))
                return "";
            KeyData keyData = parsePublicKey(publicKey);
            PublicKey pubKey;
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            // 尝试使用X509格式（PKCS#8）解析
            try {
                X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyData.getKeyBytes());
                pubKey = keyFactory.generatePublic(x509KeySpec);
                // 如果检测到是PKCS#1格式但成功解析，记录警告（可能是误判）
                if (keyData.isPKCS1()) {
                    log.warn("检测到PKCS#1格式标识，但使用X509格式解析成功，可能存在格式误判");
                }
            } catch (InvalidKeySpecException | IllegalArgumentException e) {
                // 如果X509格式失败，根据isPKCS1标识提供更准确的错误信息
                String formatHint = keyData.isPKCS1() ? "检测到PKCS#1格式（-----BEGIN RSA PUBLIC KEY-----），但Java标准库不支持此格式。" : "尝试使用PKCS#8格式（X509）解析失败。";
                String errorMsg = String.format(
                        "公钥格式错误！\n" +
                                "错误信息: %s\n" +
                                "格式检测: %s\n" +
                                "可能的原因:\n" +
                                "1. iPhone客户端生成的公钥格式不兼容（%s）\n" +
                                "2. 公钥Base64编码不正确\n" +
                                "3. 公钥数据损坏\n\n" +
                                "解决方案:\n" +
                                "- 确保iPhone客户端生成PKCS#8格式（X509）的公钥\n" +
                                "- PKCS#1格式需要转换为PKCS#8格式或使用第三方库（如BouncyCastle）\n" +
                                "- 或者使用Java后端生成的公钥进行测试\n" +
                                "- 检查公钥是否包含正确的PEM头尾标识",
                        e.getMessage(),
                        formatHint,
                        keyData.isPKCS1() ? "检测到PKCS#1格式" : "格式不匹配"
                );
                log.error("公钥解析: {}", errorMsg);
                throw new InvalidKeyException(errorMsg, e);
            }
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);
            // RSA加密有长度限制，需要分段加密
            // 从公钥中获取密钥长度
            int keySize = ((RSAPublicKey) pubKey).getModulus().bitLength();
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            int inputLen = dataBytes.length;
            int maxBlockSize = keySize / 8 - 11; // 最大加密块长度（PKCS1Padding需要11字节填充）
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
            if (StringUtils.isBlank(data))
                return "";
            byte[] keyBytes = Base64.getDecoder().decode(privateKey);
            PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            PrivateKey priKey = keyFactory.generatePrivate(pkcs8KeySpec);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, priKey);
            // RSA解密需要分段解密
            // 从私钥中获取密钥长度
            int keySize = ((RSAPrivateKey) priKey).getModulus().bitLength();
            byte[] encryptedBytes = Base64.getDecoder().decode(data);
            int inputLen = encryptedBytes.length;
            int maxBlockSize = keySize / 8; // 最大解密块长度
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
        RsaKey pair = generate();
        boolean v = verify(pair.getPublicKey(), pair.getPrivateKey());
        log.info("测试结果:{}", v);
    }
}

