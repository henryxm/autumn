package cn.org.autumn.crypto;

import org.apache.commons.lang.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 字段存储 AES-256-GCM 加解密与 HMAC 盲索引。
 */
public final class FieldCrypto {

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final String HMAC_SHA256 = "HmacSHA256";

    private FieldCrypto() {
    }

    public static String encrypt(String plaintext, byte[] key, String prefix, String fixedVectorBase64) {
        if (StringUtils.isBlank(plaintext)) {
            return "";
        }
        if (StringUtils.isNotBlank(prefix) && plaintext.startsWith(prefix)) {
            return plaintext;
        }
        try {
            byte[] iv = resolveIv(fixedVectorBase64);
            Cipher cipher = Cipher.getInstance(AES_GCM);
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return prefix + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("字段加密失败", e);
        }
    }

    public static String decrypt(String ciphertext, byte[] key, String prefix) {
        if (StringUtils.isBlank(ciphertext)) {
            return "";
        }
        if (StringUtils.isBlank(prefix) || !ciphertext.startsWith(prefix)) {
            return ciphertext;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(ciphertext.substring(prefix.length()));
            if (combined.length <= GCM_IV_LENGTH) {
                return ciphertext;
            }
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance(AES_GCM);
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] plain = cipher.doFinal(encrypted);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("字段解密失败", e);
        }
    }

    public static String hash(String plaintext, byte[] hashKey) {
        if (StringUtils.isBlank(plaintext)) {
            return "";
        }
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(hashKey, HMAC_SHA256));
            byte[] digest = mac.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("字段盲索引计算失败", e);
        }
    }

    public static byte[] decodeKeyBase64(String keyBase64) {
        if (StringUtils.isBlank(keyBase64)) {
            return null;
        }
        return Base64.getDecoder().decode(keyBase64.trim());
    }

    /** 生成 AES-256 主密钥（32 字节）的 Base64 字符串，仅供开发配置使用。 */
    public static String generateRandomKeyBase64() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    /** 生成 GCM IV（12 字节）的 Base64 字符串，仅供开发或 @FieldEncrypt(vector) 参考。 */
    public static String generateRandomVectorBase64() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return Base64.getEncoder().encodeToString(iv);
    }

    private static byte[] resolveIv(String fixedVectorBase64) {
        if (StringUtils.isNotBlank(fixedVectorBase64)) {
            byte[] iv = Base64.getDecoder().decode(fixedVectorBase64.trim());
            if (iv.length != GCM_IV_LENGTH) {
                throw new IllegalArgumentException("固定 IV 须为 Base64 编码的 12 字节");
            }
            return iv;
        }
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
