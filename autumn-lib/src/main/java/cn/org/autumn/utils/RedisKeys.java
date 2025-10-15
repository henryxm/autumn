package cn.org.autumn.utils;

import org.apache.commons.lang.StringUtils;

/**
 * Redis所有Keys
 */
public class RedisKeys {

    public static String getConfigPrefix(String namespace) {
        return StringUtils.isBlank(namespace) ? "system:config:" : namespace.trim() + ":system:config:";
    }

    public static String getSessionPrefix(String namespace) {
        return StringUtils.isBlank(namespace) ? "system:session:" : namespace.trim() + ":system:session:";
    }

    public static String getSysConfigKey(String namespace, String key) {
        return getConfigPrefix(namespace) + key;
    }

    public static String getShiroSessionKey(String namespace, String key) {
        return getSessionPrefix(namespace) + key;
    }
}
