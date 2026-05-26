package cn.org.autumn.integration.robot;

import cn.org.autumn.integration.base.IntegrationTest;
import cn.org.autumn.integration.support.IntegrationJson;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 机器人管理开放 API 集成测试（用户令牌）。
 */
public class RobotApiIntegrationTest extends IntegrationTest {

    private static final String HOOK_CALLBACK = "https://example.com/autumn-it/robot-hook";

    @Test
    public void create_list_disable_enable_delete_flow() {
        String name = "it-robot-" + UUID.randomUUID().toString().substring(0, 8);
        JSONObject create = robotApi.post("/create", userToken, createBody(name));
        IntegrationJson.assertSuccess(create);
        JSONObject createData = IntegrationJson.data(create);
        assertNotNull(createData);
        assertTrue(createData.getString("token").startsWith("rbt_"));
        String robotUuid = createData.getJSONObject("robot").getString("uuid");
        assertNotNull(robotUuid);

        JSONObject list = robotApi.post("/list", userToken, null);
        IntegrationJson.assertSuccess(list);
        JSONArray robots = IntegrationJson.data(list).getJSONArray("list");
        assertTrue(containsUuid(robots, robotUuid));

        JSONObject hook = robotApi.post("/hook/create", userToken, hookCreateBody(robotUuid));
        IntegrationJson.assertSuccess(hook);
        assertEquals(robotUuid, IntegrationJson.data(hook).getString("robot"));

        JSONObject tokenList = robotApi.post("/token/list", userToken, uuidBody(robotUuid));
        IntegrationJson.assertSuccess(tokenList);
        assertTrue(IntegrationJson.data(tokenList).getIntValue("usedRows") >= 1);

        JSONObject disable = robotApi.post("/disable", userToken, uuidBody(robotUuid));
        IntegrationJson.assertSuccess(disable);

        JSONObject enable = robotApi.post("/enable", userToken, uuidBody(robotUuid));
        IntegrationJson.assertSuccess(enable);

        JSONObject delete = robotApi.post("/delete", userToken, uuidBody(robotUuid));
        IntegrationJson.assertSuccess(delete);
    }

    @Test
    public void config_get_returnsQuota() {
        JSONObject resp = robotApi.post("/config/get", userToken, null);
        IntegrationJson.assertSuccess(resp);
        JSONObject data = IntegrationJson.data(resp);
        assertNotNull(data.getString("uuid"));
        assertTrue(data.getIntValue("effectiveMaxRobots") > 0 || data.getIntValue("effectiveMaxRobots") == -1);
    }

    @Test
    public void create_rejectsBlankName() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "");
        JSONObject resp = robotApi.post("/create", userToken, body);
        IntegrationJson.assertBusinessFailure(resp);
    }

    private static Map<String, Object> createBody(String name) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("description", "integration test");
        body.put("tokenExpireDays", 30);
        return body;
    }

    private static Map<String, Object> hookCreateBody(String robotUuid) {
        Map<String, Object> body = new HashMap<>();
        body.put("robot", robotUuid);
        body.put("name", "it-hook");
        body.put("callbackUrl", HOOK_CALLBACK);
        body.put("secret", "it-secret-" + UUID.randomUUID());
        body.put("events", "demo.ping,*");
        return body;
    }

    private static Map<String, Object> uuidBody(String uuid) {
        Map<String, Object> body = new HashMap<>();
        body.put("uuid", uuid);
        return body;
    }

    private static boolean containsUuid(JSONArray robots, String uuid) {
        if (robots == null)
            return false;
        for (int i = 0; i < robots.size(); i++) {
            JSONObject item = robots.getJSONObject(i);
            if (uuid.equals(item.getString("uuid")))
                return true;
        }
        return false;
    }
}
