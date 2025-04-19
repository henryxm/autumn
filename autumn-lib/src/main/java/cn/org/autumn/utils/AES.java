package cn.org.autumn.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AES {

    public static final Logger log = LoggerFactory.getLogger(AES.class);

    public static String encrypt(String content, String password) {
        return Crypto.encrypt(content, null, password, null);
    }

    public static String decrypt(String content, String password) {
        return Crypto.decrypt(content, null, password, null);
    }

    public static String encrypt(String content, String password, String vector) {
        return Crypto.encrypt(content, null, password, vector);
    }

    public static String decrypt(String content, String password, String vector) {
        return Crypto.decrypt(content, null, password, vector);
    }
}
