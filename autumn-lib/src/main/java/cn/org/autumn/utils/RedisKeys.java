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

    /** 强制下线 key 的前缀，批量查询请用 SCAN(prefix+"*")，禁止 KEYS */
    public static String getForceLogoutPrefix(String namespace) {
        return StringUtils.isBlank(namespace) ? "system:logout:" : namespace.trim() + ":system:logout:";
    }

    /** 字段存储加密 Redis 键前缀（集群模式开关与密钥） */
    public static String getFieldEncryptPrefix(String namespace) {
        String ns = StringUtils.isBlank(namespace) ? "" : namespace.trim() + ":";
        return ns + "autumn:field-encrypt:";
    }

    public static String getFieldEncryptKey(String namespace, String suffix) {
        return getFieldEncryptPrefix(namespace) + suffix;
    }

    public static String getFieldEncryptRefreshChannel(String namespace) {
        return getFieldEncryptPrefix(namespace) + "refresh";
    }
}
