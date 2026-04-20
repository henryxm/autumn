package cn.org.autumn.jackson;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * 为所有 Java 枚举注册 {@link FlexibleEnumDeserializer}，以兼容客户端将枚举写成 JSON 对象的常见写法。
 */
public class FlexibleEnumJacksonModule extends SimpleModule {

    public FlexibleEnumJacksonModule() {
        super("autumn-flexible-enums");
        setDeserializerModifier(new BeanDeserializerModifier() {
            @Override
            public JsonDeserializer<?> modifyEnumDeserializer(DeserializationConfig config, JavaType type,
                    BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
                Class<?> raw = type.getRawClass();
                if (raw == null || !raw.isEnum()) {
                    return deserializer;
                }
                return new FlexibleEnumDeserializer(deserializer);
            }
        });
    }
}
