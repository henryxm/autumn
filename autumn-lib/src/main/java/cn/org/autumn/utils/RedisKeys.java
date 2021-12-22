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
        return namespace + "system:config:" + key;
    }

    public static String getShiroSessionKey(String namespace, String key) {
        if (null == namespace)
            namespace = "";
        else
            namespace = namespace.trim() + ":";
        return namespace + "system:sessionid:" + key;
    }
}
