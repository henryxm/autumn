package cn.org.autumn.utils;

import org.apache.commons.lang3.StringUtils;

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

    /**
     * 强制下线标记 key：该 userUuid 在 TTL 内不允许通过 RememberMe 自动登录，需重新输入密码
     */
    public static String getForceLogoutKey(String namespace, String userUuid) {
        return getForceLogoutPrefix(namespace) + (userUuid != null ? userUuid : "");
    }

    /** 强制下线 key 的前缀，用于 keys(prefix+"*") 批量查询 */
    public static String getForceLogoutPrefix(String namespace) {
        return StringUtils.isBlank(namespace) ? "system:logout:" : namespace.trim() + ":system:logout:";
    }
}
