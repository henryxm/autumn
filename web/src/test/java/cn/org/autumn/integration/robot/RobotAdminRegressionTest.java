package cn.org.autumn.integration.robot;

import cn.org.autumn.integration.base.IntegrationTest;
import cn.org.autumn.integration.support.IntegrationJson;
import cn.org.autumn.integration.support.RobotTestBodies;
import cn.org.autumn.integration.support.RobotTestContext;
import cn.org.autumn.integration.support.RobotTestFixtures;
import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 管理员配置与 Hook 启停回归。
 */
public class RobotAdminRegressionTest extends IntegrationTest {

    private static final String HOOK_CALLBACK = "https://example.com/autumn-it/admin-hook";

    @Test
    public void config_save_updatesEffectiveQuota() {
        JSONObject save = robotApi.post("/config/save", userToken,
                RobotTestFixtures.configSaveBody(adminUuid, 50, 5, 5));
        IntegrationJson.assertSuccess(save);
        JSONObject saved = IntegrationJson.data(save);
        assertEquals(50, saved.getIntValue("maxRobots"));
        assertEquals(5, saved.getIntValue("maxTokens"));

        JSONObject get = robotApi.post("/config/get", userToken, RobotTestFixtures.configUserBody(adminUuid));
        IntegrationJson.assertSuccess(get);
        JSONObject effective = IntegrationJson.data(get);
        assertTrue(effective.getIntValue("effectiveMaxRobots") >= 50
                || effective.getIntValue("effectiveMaxRobots") == -1);
    }

    @Test
    public void hook_disable_and_enable() {
        RobotTestContext ctx = RobotTestFixtures.createRobot(robotApi, userToken, "it-hook-admin");
        JSONObject hookCreate = robotApi.post("/hook/create", userToken,
                RobotTestBodies.hookCreate(ctx.getRobotUuid(), HOOK_CALLBACK));
        IntegrationJson.assertSuccess(hookCreate);
        String hookUuid = IntegrationJson.data(hookCreate).getString("uuid");

        JSONObject disable = robotApi.post("/hook/disable", userToken, RobotTestBodies.uuid(hookUuid));
        IntegrationJson.assertSuccess(disable);

        JSONObject enable = robotApi.post("/hook/enable", userToken, RobotTestBodies.uuid(hookUuid));
        IntegrationJson.assertSuccess(enable);
    }
}
