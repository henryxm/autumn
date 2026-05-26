package cn.org.autumn.integration.support;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 机器人开放 API 集成测试请求体构造。
 */
public final class RobotTestBodies {

    private RobotTestBodies() {
    }

    public static Map<String, Object> createRobot(String name) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("description", "integration test");
        body.put("tokenExpireDays", 30);
        return body;
    }

    public static Map<String, Object> uuid(String uuid) {
        Map<String, Object> body = new HashMap<>();
        body.put("uuid", uuid);
        return body;
    }

    public static Map<String, Object> hookCreate(String robotUuid, String callbackUrl) {
        Map<String, Object> body = new HashMap<>();
        body.put("robot", robotUuid);
        body.put("name", "it-hook");
        body.put("callbackUrl", callbackUrl);
        body.put("secret", "it-secret-" + UUID.randomUUID());
        body.put("events", "demo.ping,*");
        return body;
    }

    public static Map<String, Object> messagePush(String type, Object payload, String messageId) {
        Map<String, Object> body = new HashMap<>();
        body.put("type", type);
        if (payload != null)
            body.put("data", payload);
        if (messageId != null)
            body.put("messageId", messageId);
        return body;
    }
}
