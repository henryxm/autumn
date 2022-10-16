package cn.org.autumn.utils;

import org.apache.commons.lang.StringUtils;

import java.util.UUID;

public class Uuid {
    public static String uuid() {
        return UUID.randomUUID().toString().replace("-", "").toLowerCase();
    }

    public static String format(String uuid) {
        if (StringUtils.isBlank(uuid) || uuid.length() != 32)
            return uuid;
        String tmp = uuid.substring(0, 8) + "-" + uuid.substring(8, 12) + "-" + uuid.substring(12, 16) + "-" + uuid.substring(16, 20) + "-" + uuid.substring(20, 32);
        return tmp.toUpperCase();
    }
}
