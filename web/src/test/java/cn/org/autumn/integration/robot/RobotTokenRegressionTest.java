package cn.org.autumn.integration.robot;

import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.org.autumn.integration.base.IntegrationTest;
import cn.org.autumn.integration.support.IntegrationJson;
import cn.org.autumn.integration.support.RobotTestBodies;
import cn.org.autumn.integration.support.RobotTestContext;
import cn.org.autumn.integration.support.RobotTestFixtures;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Test;

/**
 * 令牌轮换与 usedRows 配额回归。
 */
public class RobotTokenRegressionTest extends IntegrationTest {

    @Test
    public void token_rotate_increasesUsedRows_andNewTokenWorks() {
        RobotTestContext ctx = RobotTestFixtures.createRobot(robotApi, userToken, "it-rotate");

        JSONObject listBefore = robotApi.post("/token/list", userToken, RobotTestBodies.uuid(ctx.getRobotUuid()));
        IntegrationJson.assertSuccess(listBefore);
        assertTrue(IntegrationJson.data(listBefore).getIntValue("usedRows") >= 1);

        JSONObject rotate = robotApi.post("/token/rotate", userToken, RobotTestBodies.uuid(ctx.getRobotUuid()));
        IntegrationJson.assertSuccess(rotate);
        String newToken = IntegrationJson.data(rotate).getString("token");
        assertTrue(newToken.startsWith("rbt_"));

        JSONObject listAfter = robotApi.post("/token/list", userToken, RobotTestBodies.uuid(ctx.getRobotUuid()));
        IntegrationJson.assertSuccess(listAfter);
        int usedAfter = IntegrationJson.data(listAfter).getIntValue("usedRows");
        assertTrue(usedAfter >= 2);

        JSONObject push = robotApi.post("/message/push", newToken,
                RobotTestBodies.messagePush("demo.ping", null, "idem-rot-" + System.nanoTime()));
        IntegrationJson.assertSuccess(push);
    }

    @Test
    public void token_rotate_atQuota_failsUntilRevokedSlotFreed() {
        JSONObject saveQuota = robotApi.post("/config/save", userToken,
                RobotTestFixtures.configSaveBody(adminUuid, null, 2, null));
        IntegrationJson.assertSuccess(saveQuota);

        RobotTestContext ctx = RobotTestFixtures.createRobot(robotApi, userToken, "it-rotate-cap");

        JSONObject createSecond = robotApi.post("/token/create", userToken, RobotTestBodies.uuid(ctx.getRobotUuid()));
        IntegrationJson.assertSuccess(createSecond);

        JSONObject listFull = robotApi.post("/token/list", userToken, RobotTestBodies.uuid(ctx.getRobotUuid()));
        IntegrationJson.assertSuccess(listFull);
        JSONObject listData = IntegrationJson.data(listFull);
        assertTrue(listData.getIntValue("usedRows") >= 2);
        assertTrue(listData.getIntValue("maxRows") >= 2);

        JSONObject rotateBlocked = robotApi.post("/token/rotate", userToken, RobotTestBodies.uuid(ctx.getRobotUuid()));
        IntegrationJson.assertBusinessFailure(rotateBlocked);
        assertTrue(IntegrationJson.msg(rotateBlocked).contains("上限"));

        JSONArray tokens = listData.getJSONArray("tokens");
        String tokenRowUuid = tokens.getJSONObject(0).getString("uuid");
        JSONObject revoke = robotApi.post("/token/revoke", userToken, RobotTestBodies.uuid(tokenRowUuid));
        IntegrationJson.assertSuccess(revoke);

        JSONObject rotateOk = robotApi.post("/token/rotate", userToken, RobotTestBodies.uuid(ctx.getRobotUuid()));
        IntegrationJson.assertSuccess(rotateOk);
        assertTrue(IntegrationJson.data(rotateOk).getString("token").startsWith("rbt_"));
    }
}
