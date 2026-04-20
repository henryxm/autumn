package cn.org.autumn.gson;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.Map;

/**
 * 与 Jackson {@code FlexibleEnumJacksonModule} 对齐：枚举反序列化兼容空串、JSON 对象包装、序号等；
 * 供 {@link cn.org.autumn.modules.oauth.resolver.EncryptArgumentResolver} 解密后 {@code gson.fromJson} 路径使用。
 */
public class FlexibleEnumGsonTypeAdapterFactory implements TypeAdapterFactory {

    private static final String[] PREFERRED_KEYS = {"name", "value", "code", "type", "kind"};

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
        Class<?> raw = typeToken.getRawType();
        if (!raw.isEnum()) {
            return null;
        }
        Class<? extends Enum> enumClass = (Class<? extends Enum>) raw;
        return (TypeAdapter<T>) new TypeAdapter<Enum>() {

            @Override
            public void write(JsonWriter out, Enum value) throws IOException {
                if (value == null) {
                    out.nullValue();
                } else {
                    out.value(value.name());
                }
            }

            @Override
            public Enum read(JsonReader in) throws IOException {
                JsonToken peek = in.peek();
                if (peek == JsonToken.NULL) {
                    in.nextNull();
                    return null;
                }
                JsonElement element = JsonParser.parseReader(in);
                return readEnum(enumClass, element);
            }
        };
    }

    /**
     * 逻辑与 Jackson 侧 {@code FlexibleEnumDeserializer} / {@code FlexibleEnumJsonSupport} 保持一致，避免加密解密链路行为分叉。
     */
    static Enum<?> readEnum(Class<? extends Enum> ec, JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonPrimitive()) {
            JsonPrimitive p = element.getAsJsonPrimitive();
            if (p.isString()) {
                String s = p.getAsString();
                if (s == null || s.trim().isEmpty()) {
                    return null;
                }
                return Enum.valueOf(ec, s.trim());
            }
            if (p.isNumber()) {
                int ord = p.getAsInt();
                Enum<?>[] constants = ec.getEnumConstants();
                if (constants != null && ord >= 0 && ord < constants.length) {
                    return constants[ord];
                }
                throw new JsonParseException("ordinal out of range for " + ec.getSimpleName());
            }
        }
        if (element.isJsonObject()) {
            JsonObject o = element.getAsJsonObject();
            if (o.size() == 1 && o.has("ordinal")) {
                JsonElement ordEl = o.get("ordinal");
                if (ordEl.isJsonPrimitive() && ordEl.getAsJsonPrimitive().isNumber()) {
                    int ord = ordEl.getAsJsonPrimitive().getAsInt();
                    Enum<?>[] constants = ec.getEnumConstants();
                    if (constants != null && ord >= 0 && ord < constants.length) {
                        return constants[ord];
                    }
                    throw new JsonParseException("ordinal out of range for " + ec.getSimpleName());
                }
            }
            String text = extractText(o);
            if (text == null || text.trim().isEmpty()) {
                return null;
            }
            return Enum.valueOf(ec, text.trim());
        }
        throw new JsonParseException("cannot deserialize enum from " + element.getClass());
    }

    private static String extractText(JsonObject o) {
        for (String k : PREFERRED_KEYS) {
            if (!o.has(k)) {
                continue;
            }
            JsonElement v = o.get(k);
            if (v.isJsonNull()) {
                continue;
            }
            if (v.isJsonPrimitive()) {
                JsonPrimitive prim = v.getAsJsonPrimitive();
                if (prim.isString()) {
                    return prim.getAsString();
                }
                if (prim.isNumber()) {
                    return String.valueOf(prim.getAsLong());
                }
            }
        }
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            JsonElement v = e.getValue();
            if (v.isJsonPrimitive() && v.getAsJsonPrimitive().isString()) {
                return v.getAsJsonPrimitive().getAsString();
            }
        }
        return null;
    }
}
