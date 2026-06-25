package cn.org.autumn.integration.robot;

import static org.junit.jupiter.api.Assertions.*;

import cn.org.autumn.integration.base.IntegrationTest;
import cn.org.autumn.integration.support.IntegrationJson;
import cn.org.autumn.integration.support.RobotTestBodies;
import com.alibaba.fastjson.JSONObject;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 入站 message/push 扩展回归：停用机器人、类型/载荷校验。
 */
public class RobotInboundRegressionTest extends IntegrationTest {

    private String robotUuid;
    private String robotToken;

    @BeforeEach
    public void prepareRobot() {
        String name = "it-inbound-reg-" + UUID.randomUUID().toString().substring(0, 8);
        JSONObject create = robotApi.post("/create", userToken, RobotTestBodies.createRobot(name));
        IntegrationJson.assertSuccess(create);
        JSONObject data = IntegrationJson.data(create);
        robotUuid = data.getJSONObject("robot").getString("uuid");
        robotToken = data.getString("token");
    }

    @Test
    public void push_rejectsWhenRobotDisabled() {
        JSONObject disable = robotApi.post("/disable", userToken, RobotTestBodies.uuid(robotUuid));
        IntegrationJson.assertSuccess(disable);

        // disable 会作废全部 rbt_ 令牌（见 docs/AI_ROBOT.md），原令牌应鉴权失败
        JSONObject resp = robotApi.post("/message/push", robotToken,
                RobotTestBodies.messagePush("demo.ping", new HashMap<>(), "idem-dis-" + UUID.randomUUID()));
        IntegrationJson.assertAuthFailure(resp);
    }

    @Test
    public void push_rejectsInvalidType() {
        JSONObject resp = robotApi.post("/message/push", robotToken,
                RobotTestBodies.messagePush("INVALID_TYPE", new HashMap<>(), "idem-type-" + UUID.randomUUID()));
        IntegrationJson.assertBusinessFailure(resp);
        assertTrue(IntegrationJson.msg(resp).contains("类型") || IntegrationJson.msg(resp).contains("格式"));
    }

    @Test
    public void push_rejectsInvalidJsonPayload() {
        Map<String, Object> body = new HashMap<>();
        body.put("type", "demo.ping");
        body.put("data", "{invalid-json");
        body.put("messageId", "idem-json-" + UUID.randomUUID());
        JSONObject resp = robotApi.post("/message/push", robotToken, body);
        IntegrationJson.assertBusinessFailure(resp);
        assertTrue(IntegrationJson.msg(resp).contains("JSON"));
    }

    @Test
    public void push_withoutToken_returnsLoginError() {
        JSONObject resp = robotApi.post("/message/push", null,
                RobotTestBodies.messagePush("demo.ping", new HashMap<>(), null));
        IntegrationJson.assertAuthFailure(resp);
    }
}
