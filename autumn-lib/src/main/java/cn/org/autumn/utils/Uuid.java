package cn.org.autumn.utils;

import org.apache.commons.lang.StringUtils;

import java.util.UUID;

/**
 * 无连字符小写 32 位 UUID，常用作<strong>业务主键</strong>；长整型可选 {@link SnowflakeId}。自增 {@code id} 仅用于后台生成 CRUD。
 */
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
