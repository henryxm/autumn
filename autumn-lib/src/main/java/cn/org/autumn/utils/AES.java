package cn.org.autumn.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class AES {

    public static final Logger log = LoggerFactory.getLogger(AES.class);

    public static String encrypt(String content, String password) {
        try {
            if (content.isEmpty())
                return "";
            SecretKeySpec key = new SecretKeySpec(password.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] result = cipher.doFinal(bytes);
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            log.debug("加密失败:{}", e.getMessage());
        }
        return null;
    }

    public static String decrypt(String content, String password) {
        try {
            byte[] bytes = Base64.getDecoder().decode(content);
            SecretKeySpec key = new SecretKeySpec(password.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] result = cipher.doFinal(bytes);
            return new String(result);
        } catch (Exception e) {
            log.debug("解密失败:{}", e.getMessage());
        }
        return "";
    }
}
