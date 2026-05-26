package cn.org.autumn.integration.support;

import com.alibaba.fastjson.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 通过开放 API 创建测试机器人。
 */
public final class RobotTestFixtures {

    private RobotTestFixtures() {
    }

    public static RobotTestContext createRobot(RobotApiClient robotApi, String userToken, String namePrefix) {
        String name = namePrefix + "-" + UUID.randomUUID().toString().substring(0, 8);
        JSONObject create = robotApi.post("/create", userToken, RobotTestBodies.createRobot(name));
        IntegrationJson.assertSuccess(create);
        JSONObject data = IntegrationJson.data(create);
        String robotUuid = data.getJSONObject("robot").getString("uuid");
        String robotToken = data.getString("token");
        if (robotToken == null || !robotToken.startsWith("rbt_"))
            throw new AssertionError("创建机器人未返回 rbt_ 令牌");
        return new RobotTestContext(robotUuid, robotToken);
    }

    public static Map<String, Object> configUserBody(String userUuid) {
        Map<String, Object> body = new HashMap<>();
        body.put("uuid", userUuid);
        return body;
    }

    public static Map<String, Object> configSaveBody(String userUuid, Integer maxRobots, Integer maxTokens, Integer maxHooks) {
        Map<String, Object> body = new HashMap<>();
        body.put("uuid", userUuid);
        if (maxRobots != null)
            body.put("maxRobots", maxRobots);
        if (maxTokens != null)
            body.put("maxTokens", maxTokens);
        if (maxHooks != null)
            body.put("maxHooks", maxHooks);
        return body;
    }
}
