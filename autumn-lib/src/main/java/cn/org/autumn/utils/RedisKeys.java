package cn.org.autumn.utils;

/**
 * Redis所有Keys
 */
public class RedisKeys {

    public static String getSysConfigKey(String namespace, String key) {
        if (null == namespace)
            namespace = "";
        else
            namespace = namespace.trim() + ":";
        return "system:config:" + namespace + key;
    }

    public static String getShiroSessionKey(String key) {
        return "system:sessionid:" + key;
    }
}
