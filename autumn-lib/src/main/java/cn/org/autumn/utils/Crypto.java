package cn.org.autumn.utils;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Crypto {

    public static final Logger log = LoggerFactory.getLogger(Crypto.class);

    public static String encrypt(String content, String algorithm, String password, String vector) {
        try {
            if (content.isEmpty())
                return "";
            if (StringUtils.isBlank(algorithm))
                algorithm = "AES";
            SecretKeySpec key = new SecretKeySpec(password.getBytes(), algorithm);
            Cipher cipher = Cipher.getInstance(algorithm);
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            if (StringUtils.isNotBlank(vector)) {
                IvParameterSpec iv = new IvParameterSpec(vector.getBytes());
                cipher.init(Cipher.ENCRYPT_MODE, key, iv);
            } else {
                cipher.init(Cipher.ENCRYPT_MODE, key);
            }
            byte[] result = cipher.doFinal(bytes);
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            log.debug("加密失败:{}", e.getMessage());
        }
        return null;
    }

    public static String decrypt(String content, String algorithm, String password, String vector) {
        try {
            byte[] bytes = Base64.getDecoder().decode(content);
            if (StringUtils.isBlank(algorithm))
                algorithm = "AES";
            SecretKeySpec key = new SecretKeySpec(password.getBytes(), algorithm);
            Cipher cipher = Cipher.getInstance(algorithm);
            if (StringUtils.isNotBlank(vector)) {
                IvParameterSpec iv = new IvParameterSpec(vector.getBytes());
                cipher.init(Cipher.DECRYPT_MODE, key, iv);
            } else {
                cipher.init(Cipher.DECRYPT_MODE, key);
            }
            byte[] result = cipher.doFinal(bytes);
            return new String(result);
        } catch (Exception e) {
            log.debug("解密失败:{}", e.getMessage());
        }
        return "";
    }
}
