package cn.org.autumn.config;

import com.google.gson.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Gson配置类
 * 配置Gson的日期时间格式化，与Spring框架保持一致
 * Spring默认格式：yyyy-MM-dd HH:mm:ss，时区：GMT+8
 * <p>
 * 增强功能：
 * 1. Date类型：支持空字符串、日期字符串、时间戳
 * 2. Boolean类型：支持0/1转换为false/true，字符串"true"/"false"转换
 * 3. Number类型：支持字符串转数字，空字符串转0
 * 4. String类型：null转空字符串
 */
@Configuration
public class GsonConfig {

    /**
     * Spring框架默认的日期时间格式
     */
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * Spring框架默认的时区
     */
    private static final String TIME_ZONE = "GMT+8";

    @Bean
    public Gson gson() {
        // 创建SimpleDateFormat并设置时区
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_TIME_PATTERN);
        dateFormat.setTimeZone(TimeZone.getTimeZone(TIME_ZONE));
        // 创建类型适配器
        DateTypeAdapter dateAdapter = new DateTypeAdapter(dateFormat);
        BooleanTypeAdapter booleanAdapter = new BooleanTypeAdapter();
        return new GsonBuilder()
                .setDateFormat(DATE_TIME_PATTERN)
                .registerTypeAdapter(Date.class, dateAdapter)
                .registerTypeAdapter(Boolean.class, booleanAdapter)
                .registerTypeAdapter(boolean.class, booleanAdapter)
                .serializeNulls() // 序列化null值
                .create();
    }

    /**
     * Date类型自定义适配器
     * 同时处理序列化和反序列化，支持空字符串转换为null
     */
    private static class DateTypeAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {
        private final SimpleDateFormat dateFormat;

        public DateTypeAdapter(SimpleDateFormat dateFormat) {
            this.dateFormat = dateFormat;
        }

        /**
         * 序列化：将Date转换为字符串
         */
        @Override
        public JsonPrimitive serialize(Date date, Type typeOfSrc, JsonSerializationContext context) {
            if (date == null) {
                return null;
            }
            synchronized (dateFormat) {
                return new JsonPrimitive(dateFormat.format(date));
            }
        }

        /**
         * 反序列化：将JSON元素转换为Date
         * 支持空字符串、null、日期字符串和时间戳
         */
        @Override
        public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json == null || json.isJsonNull()) {
                return null;
            }
            // 处理字符串类型
            if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
                String dateStr = json.getAsString();
                // 空字符串返回null
                if (dateStr == null || dateStr.trim().isEmpty()) {
                    return null;
                }
                try {
                    // 尝试使用配置的日期格式解析
                    synchronized (dateFormat) {
                        return dateFormat.parse(dateStr);
                    }
                } catch (ParseException e) {
                    // 如果解析失败，尝试使用时间戳（毫秒）
                    try {
                        long timestamp = Long.parseLong(dateStr);
                        return new Date(timestamp);
                    } catch (NumberFormatException ex) {
                        // 如果都失败，抛出异常
                        throw new JsonParseException("无法解析日期字符串: " + dateStr, e);
                    }
                }
            }
            // 处理数字类型（时间戳）
            if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
                long timestamp = json.getAsLong();
                return new Date(timestamp);
            }
            // 其他类型无法解析
            throw new JsonParseException("无法将 " + json.getClass().getSimpleName() + " 转换为 Date");
        }
    }

    /**
     * Boolean类型自定义适配器
     * 支持0/1转换为false/true，字符串"true"/"false"转换
     */
    private static class BooleanTypeAdapter implements JsonSerializer<Boolean>, JsonDeserializer<Boolean> {

        /**
         * 序列化：将Boolean转换为JSON布尔值
         */
        @Override
        public JsonElement serialize(Boolean src, Type typeOfSrc, JsonSerializationContext context) {
            if (src == null) {
                return JsonNull.INSTANCE;
            }
            return new JsonPrimitive(src);
        }

        /**
         * 反序列化：将JSON元素转换为Boolean
         * 支持：
         * 1. 数字0/1转换为false/true
         * 2. 字符串"true"/"false"、"1"/"0"、"yes"/"no"转换
         * 3. 布尔值直接返回
         * 4. null返回null
         */
        @Override
        public Boolean deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json == null || json.isJsonNull()) {
                return null;
            }
            // 处理布尔类型
            if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isBoolean()) {
                return json.getAsBoolean();
            }
            // 处理数字类型：0=false, 1=true, 其他数字抛出异常
            if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
                long num = json.getAsLong();
                if (num == 0) {
                    return false;
                } else if (num == 1) {
                    return true;
                } else {
                    throw new JsonParseException("无法将数字 " + num + " 转换为 Boolean，只支持0(false)和1(true)");
                }
            }
            // 处理字符串类型
            if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
                String str = json.getAsString().trim().toLowerCase();
                if (str.isEmpty()) {
                    return null;
                }
                // 支持多种字符串格式
                if ("true".equals(str) || "1".equals(str) || "yes".equals(str) || "y".equals(str) || "on".equals(str)) {
                    return true;
                } else if ("false".equals(str) || "0".equals(str) || "no".equals(str) || "n".equals(str) || "off".equals(str)) {
                    return false;
                } else {
                    throw new JsonParseException("无法将字符串 \"" + str + "\" 转换为 Boolean");
                }
            }
            // 其他类型无法解析
            throw new JsonParseException("无法将 " + json.getClass().getSimpleName() + " 转换为 Boolean");
        }
    }
}
