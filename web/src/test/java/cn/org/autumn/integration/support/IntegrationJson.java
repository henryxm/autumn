package cn.org.autumn.integration.support;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * 解析开放 API {@code Response} JSON。
 */
public final class IntegrationJson {

    private IntegrationJson() {
    }

    public static JSONObject parse(String body) {
        if (body == null || body.isEmpty())
            return new JSONObject();
        return JSON.parseObject(body);
    }

    public static int code(JSONObject response) {
        return response == null ? -1 : response.getIntValue("code");
    }

    public static String msg(JSONObject response) {
        return response == null ? "" : response.getString("msg");
    }

    public static JSONObject data(JSONObject response) {
        return response == null ? null : response.getJSONObject("data");
    }

    public static void assertSuccess(JSONObject response) {
        assertCode(response, 0);
    }

    public static void assertCode(JSONObject response, int expectedCode) {
        assertEquals(expectedCode, code(response),
                () -> "期望 code=" + expectedCode + "，实际 code=" + code(response) + ", msg=" + msg(response)
                        + ", body=" + (response == null ? "null" : response.toJSONString()));
    }

    /** 业务失败：非 0 且非鉴权 -10000 */
    public static void assertBusinessFailure(JSONObject response) {
        assertNotEquals(0, code(response),
                () -> "期望业务失败 code≠0，实际: " + format(response));
        assertNotEquals(-10000, code(response),
                () -> "期望业务失败而非鉴权 -10000，实际: " + format(response));
    }

    public static void assertAuthFailure(JSONObject response) {
        assertCode(response, -10000);
    }

    private static String format(JSONObject response) {
        return "code=" + code(response) + ", msg=" + msg(response);
    }
}
