package cn.org.autumn.utils;

/**
 * Redis所有Keys
 */
public class RedisKeys {

    public static String getSysConfigKey(String key) {
        return "system:config:" + key;
    }

    public static String getShiroSessionKey(String key) {
        return "system:sessionid:" + key;
    }
}
