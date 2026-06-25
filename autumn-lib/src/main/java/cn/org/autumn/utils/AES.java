package cn.org.autumn.utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AES {
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
