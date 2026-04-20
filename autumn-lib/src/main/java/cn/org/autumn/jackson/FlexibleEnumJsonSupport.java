package cn.org.autumn.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.util.Iterator;
import java.util.Map;

/**
 * 从 JSON 对象中提取可交给默认枚举反序列化的标量（与客户端对象包装写法兼容）。
 */
final class FlexibleEnumJsonSupport {

    private static final String[] PREFERRED_KEYS = {"name", "value", "code", "type", "kind", "ordinal"};

    private FlexibleEnumJsonSupport() {
    }

    /**
     * 构造与 JSON 字符串/数字等形式等价的节点，供 {@link com.fasterxml.jackson.databind.node.TreeTraversingParser} 交给默认枚举 Deserializer。
     */
    static JsonNode toDelegateScalar(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            String txt = node.asText();
            if (txt.trim().isEmpty()) {
                return null;
            }
            return TextNode.valueOf(txt);
        }
        if (node.isIntegralNumber()) {
            return IntNode.valueOf(node.intValue());
        }
        if (node.isObject() && node.size() == 1 && node.has("ordinal")) {
            JsonNode ord = node.get("ordinal");
            if (ord != null && ord.isIntegralNumber()) {
                return IntNode.valueOf(ord.intValue());
            }
        }
        String text = extractEnumText(node);
        if (text == null) {
            return null;
        }
        if (text.trim().isEmpty()) {
            return null;
        }
        return TextNode.valueOf(text);
    }

    /**
     * @param node 通常为 Object 节点；若已为文本则直接返回
     */
    static String extractEnumText(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isIntegralNumber()) {
            return String.valueOf(node.longValue());
        }
        if (!node.isObject()) {
            return null;
        }
        for (String k : PREFERRED_KEYS) {
            if ("ordinal".equals(k)) {
                continue;
            }
            JsonNode v = node.get(k);
            if (v != null && !v.isNull()) {
                if (v.isTextual()) {
                    return v.asText();
                }
                if (v.isIntegralNumber()) {
                    return String.valueOf(v.longValue());
                }
            }
        }
        Iterator<Map.Entry<String, JsonNode>> it = node.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            JsonNode v = e.getValue();
            if (v != null && v.isTextual()) {
                return v.asText();
            }
        }
        return null;
    }
}
