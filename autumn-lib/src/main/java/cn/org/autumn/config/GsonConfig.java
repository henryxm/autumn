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
        // 创建同时处理序列化和反序列化的适配器
        DateTypeAdapter dateAdapter = new DateTypeAdapter(dateFormat);
        return new GsonBuilder().setDateFormat(DATE_TIME_PATTERN).registerTypeAdapter(Date.class, dateAdapter).create();
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
}
