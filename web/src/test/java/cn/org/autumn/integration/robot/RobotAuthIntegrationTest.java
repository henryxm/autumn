package cn.org.autumn.integration.robot;

import cn.org.autumn.integration.base.IntegrationTest;
import cn.org.autumn.integration.support.IntegrationJson;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 机器人 API 鉴权边界：用户令牌 vs {@code rbt_} 令牌不可混用。
 */
public class RobotAuthIntegrationTest extends IntegrationTest {

    @Test
    public void manageApi_rejectsRobotToken() {
        String robotToken = createRobotTokenOnce();
        JSONObject resp = robotApi.post("/list", robotToken, null);
        IntegrationJson.assertAuthFailure(resp);
        assertTrue(IntegrationJson.msg(resp).contains("用户令牌"));
    }

    @Test
    public void messagePush_rejectsUserToken() {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "demo.ping");
        data.put("data", new HashMap<String, Object>());
        JSONObject resp = robotApi.post("/message/push", userToken, data);
        IntegrationJson.assertAuthFailure(resp);
        assertTrue(IntegrationJson.msg(resp).contains("机器人"));
    }

    @Test
    public void manageApi_withoutToken_returnsLoginError() {
        JSONObject resp = robotApi.post("/list", null, null);
        IntegrationJson.assertAuthFailure(resp);
    }

    private String createRobotTokenOnce() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "it-auth-" + UUID.randomUUID().toString().substring(0, 8));
        JSONObject create = robotApi.post("/create", userToken, body);
        IntegrationJson.assertSuccess(create);
        return IntegrationJson.data(create).getString("token");
    }
}
