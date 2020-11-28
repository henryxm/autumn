package cn.org.autumn.utils;

import java.util.UUID;

public class Uuid {
    public static String uuid() {
        return UUID.randomUUID().toString().replace("-", "").toLowerCase();
    }
}
