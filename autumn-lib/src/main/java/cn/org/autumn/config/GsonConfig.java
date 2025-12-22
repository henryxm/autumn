package cn.org.autumn.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Type;
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
        return new GsonBuilder().setDateFormat(DATE_TIME_PATTERN).registerTypeAdapter(Date.class, new DateSerializer(dateFormat)).create();
    }

    /**
     * Date类型自定义序列化器
     * 确保使用指定的时区进行序列化
     */
    private static class DateSerializer implements JsonSerializer<Date> {
        private final SimpleDateFormat dateFormat;

        public DateSerializer(SimpleDateFormat dateFormat) {
            this.dateFormat = dateFormat;
        }

        @Override
        public JsonPrimitive serialize(Date date, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(dateFormat.format(date));
        }
    }
}
