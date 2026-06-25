package cn.org.autumn.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;

/**
 * 将请求体中的 JSON 节点（对象、数组、标量或 JSON 字符串）统一反序列化为 JSON 文本，避免 {@code Object} 类型映射不一致。
 */
public class JsonPayloadDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        if (node == null || node.isNull())
            return null;
        if (node.isTextual())
            return node.asText();
        return node.toString();
    }
}
