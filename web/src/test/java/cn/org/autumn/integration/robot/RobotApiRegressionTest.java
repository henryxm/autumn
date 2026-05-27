package cn.org.autumn.integration.robot;

import cn.org.autumn.integration.base.IntegrationTest;
import cn.org.autumn.integration.support.IntegrationJson;
import cn.org.autumn.integration.support.RobotTestBodies;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 管理 API 扩展回归：Hook/令牌细项、销毁、负向校验。
 */
public class RobotApiRegressionTest extends IntegrationTest {

    private static final String HOOK_CALLBACK = "https://example.com/autumn-it/hook";

    @Test
    public void hook_list_update_delete_flow() {
        String robotUuid = createRobotUuid();
        JSONObject hookCreate = robotApi.post("/hook/create", userToken, RobotTestBodies.hookCreate(robotUuid, HOOK_CALLBACK));
        IntegrationJson.assertSuccess(hookCreate);
        String hookUuid = IntegrationJson.data(hookCreate).getString("uuid");

        JSONObject hookList = robotApi.post("/hook/list", userToken, RobotTestBodies.uuid(robotUuid));
        IntegrationJson.assertSuccess(hookList);
        assertTrue(containsHookUuid(IntegrationJson.data(hookList).getJSONArray("hooks"), hookUuid));

        JSONObject hookUpdate = robotApi.post("/hook/update", userToken, hookUpdateBody(hookUuid));
        IntegrationJson.assertSuccess(hookUpdate);
        assertEquals("it-hook-updated", IntegrationJson.data(hookUpdate).getString("name"));

        JSONObject hookDelete = robotApi.post("/hook/delete", userToken, RobotTestBodies.uuid(hookUuid));
        IntegrationJson.assertSuccess(hookDelete);
    }

    @Test
    public void token_create_and_revoke() {
        String robotUuid = createRobotUuid();
        JSONObject createToken = robotApi.post("/token/create", userToken, RobotTestBodies.uuid(robotUuid));
        IntegrationJson.assertSuccess(createToken);
        assertTrue(IntegrationJson.data(createToken).getString("token").startsWith("rbt_"));

        JSONObject tokenList = robotApi.post("/token/list", userToken, RobotTestBodies.uuid(robotUuid));
        IntegrationJson.assertSuccess(tokenList);
        JSONArray tokens = IntegrationJson.data(tokenList).getJSONArray("tokens");
        assertTrue(tokens != null && tokens.size() >= 2);
        String tokenRowUuid = tokens.getJSONObject(0).getString("uuid");

        JSONObject revoke = robotApi.post("/token/revoke", userToken, RobotTestBodies.uuid(tokenRowUuid));
        IntegrationJson.assertSuccess(revoke);
    }

    @Test
    public void destroy_robot_irreversible() {
        String robotUuid = createRobotUuid();
        JSONObject del = robotApi.post("/delete", userToken, RobotTestBodies.uuid(robotUuid));
        IntegrationJson.assertSuccess(del);

        JSONObject destroy = robotApi.post("/destroy", userToken, RobotTestBodies.uuid(robotUuid));
        IntegrationJson.assertSuccess(destroy);

        JSONObject enable = robotApi.post("/enable", userToken, RobotTestBodies.uuid(robotUuid));
        IntegrationJson.assertBusinessFailure(enable);
    }

    @Test
    public void destroy_requires_soft_delete_first() {
        String robotUuid = createRobotUuid();
        JSONObject destroy = robotApi.post("/destroy", userToken, RobotTestBodies.uuid(robotUuid));
        IntegrationJson.assertBusinessFailure(destroy);
    }

    @Test
    public void hook_create_rejectsLocalCallback() {
        String robotUuid = createRobotUuid();
        java.util.Map<String, Object> body = RobotTestBodies.hookCreate(robotUuid, "http://127.0.0.1/hook");
        JSONObject resp = robotApi.post("/hook/create", userToken, body);
        IntegrationJson.assertBusinessFailure(resp);
        assertTrue(IntegrationJson.msg(resp).contains("内网") || IntegrationJson.msg(resp).contains("本机"));
    }

    private String createRobotUuid() {
        String name = "it-reg-" + UUID.randomUUID().toString().substring(0, 8);
        JSONObject create = robotApi.post("/create", userToken, RobotTestBodies.createRobot(name));
        IntegrationJson.assertSuccess(create);
        return IntegrationJson.data(create).getJSONObject("robot").getString("uuid");
    }

    private static java.util.Map<String, Object> hookUpdateBody(String hookUuid) {
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("uuid", hookUuid);
        body.put("name", "it-hook-updated");
        return body;
    }

    private static boolean containsHookUuid(JSONArray hooks, String hookUuid) {
        if (hooks == null)
            return false;
        for (int i = 0; i < hooks.size(); i++) {
            if (hookUuid.equals(hooks.getJSONObject(i).getString("uuid")))
                return true;
        }
        return false;
    }
}
