package cn.org.autumn.integration.robot;

import cn.org.autumn.integration.base.IntegrationTest;
import cn.org.autumn.integration.support.IntegrationJson;
import cn.org.autumn.integration.support.RobotTestBodies;
import cn.org.autumn.integration.support.RobotTestContext;
import cn.org.autumn.integration.support.RobotTestFixtures;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 机器人入站 {@code message/push} API 集成测试（{@code rbt_} 令牌）。
 */
public class RobotInboundApiIntegrationTest extends IntegrationTest {

    private RobotTestContext robot;

    @BeforeAll
    void initRobotOnce() {
        robot = RobotTestFixtures.createRobot(robotApi, userToken, "it-push");
    }

    @Test
    public void push_acceptsMessage() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", "IT-001");
        JSONObject resp = push("demo.ping", payload, "idem-" + UUID.randomUUID());
        IntegrationJson.assertSuccess(resp);
        JSONObject result = IntegrationJson.data(resp);
        assertEquals("demo.ping", result.getString("type"));
        assertTrue(result.getBooleanValue("queued"));
        assertFalse(result.getBooleanValue("duplicate"));
        assertNotNull(result.getString("messageId"));
    }

    @Test
    public void push_idempotentByMessageId() {
        String messageId = "idem-fixed-" + UUID.randomUUID();
        Map<String, Object> payload = new HashMap<>();
        payload.put("k", "v");
        JSONObject first = push("demo.ping", payload, messageId);
        IntegrationJson.assertSuccess(first);
        assertTrue(IntegrationJson.data(first).getBooleanValue("queued"));

        JSONObject second = push("demo.ping", payload, messageId);
        IntegrationJson.assertSuccess(second);
        JSONObject secondData = IntegrationJson.data(second);
        assertTrue(secondData.getBooleanValue("duplicate"));
        assertEquals(IntegrationJson.data(first).getString("messageId"), secondData.getString("messageId"));
    }

    @Test
    public void push_idempotentByHeader() {
        String messageId = "hdr-" + UUID.randomUUID();
        Map<String, Object> data = new HashMap<>();
        data.put("type", "demo.ping");
        Map<String, Object> inner = new HashMap<>();
        inner.put("x", 1);
        data.put("data", inner);
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Robot-Message-Id", messageId);
        JSONObject first = robotApi.postWithHeaders("/message/push", robot.getRobotToken(), data, headers);
        IntegrationJson.assertSuccess(first);
        JSONObject second = robotApi.postWithHeaders("/message/push", robot.getRobotToken(), data, headers);
        IntegrationJson.assertSuccess(second);
        assertTrue(IntegrationJson.data(second).getBooleanValue("duplicate"));
    }

    private JSONObject push(String type, Map<String, Object> payload, String messageId) {
        return robotApi.post("/message/push", robot.getRobotToken(),
                RobotTestBodies.messagePush(type, payload, messageId));
    }
}
