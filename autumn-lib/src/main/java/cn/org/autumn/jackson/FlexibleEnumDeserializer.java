package cn.org.autumn.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;

import java.io.IOException;

/**
 * 包装 Jackson 默认枚举反序列化：除字符串、数字外，兼容 JSON 对象形式（见 {@link FlexibleEnumJsonSupport}）。
 */
public class FlexibleEnumDeserializer extends JsonDeserializer<Enum<?>> implements ResolvableDeserializer {

    private final JsonDeserializer<?> delegate;

    public FlexibleEnumDeserializer(JsonDeserializer<?> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void resolve(com.fasterxml.jackson.databind.DeserializationContext ctxt)
            throws JsonMappingException {
        if (delegate instanceof ResolvableDeserializer) {
            ((ResolvableDeserializer) delegate).resolve(ctxt);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Enum<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken t = p.currentToken();
        if (t == JsonToken.VALUE_STRING) {
            String s = p.getText();
            if (s == null || s.trim().isEmpty()) {
                return null;
            }
        }
        if (t == JsonToken.START_OBJECT) {
            JsonNode node = ctxt.readTree(p);
            JsonNode scalar = FlexibleEnumJsonSupport.toDelegateScalar(node);
            if (scalar == null || scalar.isNull()) {
                return null;
            }
            if (scalar.isTextual() && scalar.asText().trim().isEmpty()) {
                return null;
            }
            TreeTraversingParser p2 = new TreeTraversingParser(scalar, p.getCodec());
            p2.nextToken();
            return (Enum<?>) delegate.deserialize(p2, ctxt);
        }
        return (Enum<?>) delegate.deserialize(p, ctxt);
    }

    @Override
    public Enum<?> getNullValue(DeserializationContext ctxt) throws JsonMappingException {
        if (delegate.getNullValue(ctxt) == null) {
            return null;
        }
        return (Enum<?>) delegate.getNullValue(ctxt);
    }
}
