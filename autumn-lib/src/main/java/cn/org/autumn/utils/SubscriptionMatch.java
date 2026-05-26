package cn.org.autumn.utils;

import org.apache.commons.lang.StringUtils;

/**
 * 事件 / 消息类型订阅匹配（CSV 或 {@code *}）。
 */
public final class SubscriptionMatch {

    private SubscriptionMatch() {
    }

    public static boolean matches(String subscriptions, String name) {
        if (StringUtils.isBlank(subscriptions) || "*".equals(subscriptions.trim()))
            return true;
        if (StringUtils.isBlank(name))
            return false;
        String[] parts = subscriptions.split(",");
        for (String part : parts) {
            if (name.equals(StringUtils.trim(part)))
                return true;
        }
        return false;
    }
}
