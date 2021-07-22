package cn.org.autumn.config;

import cn.org.autumn.annotation.EnvAware;
import cn.org.autumn.site.InitFactory;
import org.apache.commons.lang.StringUtils;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.math.BigDecimal;

public interface EnvHandler extends InitFactory.Before {

    default void set(Field field, Object value) {
    }

    @Override
    @Order(0)
    default void before() {
        Field[] fields = getClass().getDeclaredFields();
        for (Field field : fields) {
            EnvAware envAware = field.getDeclaredAnnotation(EnvAware.class);
            String key = field.getName();
            if (null != envAware && StringUtils.isNotBlank(envAware.value()))
                key = envAware.value();
            String value = Config.getEnv(key);
            if (StringUtils.isNotBlank(value)) {
                field.setAccessible(true);
                try {
                    Type type = field.getGenericType();
                    if (type.equals(boolean.class) || type.equals(Boolean.class)) {
                        boolean v = false;
                        if ("1".equals(value) || "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value))
                            v = true;
                        field.set(this, v);
                    } else if (type.equals(int.class) || type.equals(Integer.class)) {
                        field.set(this, Integer.parseInt(value));
                    } else if (type.equals(long.class) || type.equals(Long.class)) {
                        field.set(this, Long.parseLong(value));
                    } else if (type.equals(float.class) || type.equals(Float.class)) {
                        field.set(this, Float.parseFloat(value));
                    } else if (type.equals(double.class) || type.equals(Double.class)) {
                        field.set(this, Double.parseDouble(value));
                    } else if (type.equals(BigDecimal.class)) {
                        field.set(this, BigDecimal.valueOf(Long.parseLong(value)));
                    } else if (type.equals(String.class))
                        field.set(this, value);
                    else
                        set(field, value);
                } catch (Exception ignored) {
                }
            }
        }
    }
}