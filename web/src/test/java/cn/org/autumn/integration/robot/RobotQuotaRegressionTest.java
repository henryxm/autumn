package cn.org.autumn.integration.robot;

import cn.org.autumn.integration.support.IntegrationJson;
import cn.org.autumn.integration.support.RobotTestBodies;
import cn.org.autumn.integration.support.RobotTestFixtures;
import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 配额：软删释放名额；停用仍占名额；软删后不可 enable。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RobotQuotaRegressionTest extends RobotIntegrationTest {

    @AfterAll
    void restoreQuota() {
        robotService.purgeAllRobotsForOwner(adminUuid);
        robotApi.post("/config/save", userToken, RobotTestFixtures.configSaveBody(adminUuid, -1, null, null));
    }

    @Test
    @Order(1)
    public void disabled_still_occupies_quota() {
        robotApi.post("/config/save", userToken, RobotTestFixtures.configSaveBody(adminUuid, 1, null, null));
        String robotA = createRobot("it-quota-dis");
        robotApi.post("/disable", userToken, RobotTestBodies.uuid(robotA));
        JSONObject second = robotApi.post("/create", userToken, RobotTestBodies.createRobot("it-quota-dis-b"));
        IntegrationJson.assertBusinessFailure(second);
        assertTrue(IntegrationJson.msg(second).contains("上限"));

        JSONObject del = robotApi.post("/delete", userToken, RobotTestBodies.uuid(robotA));
        IntegrationJson.assertSuccess(del);
        try {
            robotService.destroyByAdministrator(robotA);
        } catch (Exception ignored) {
        }
    }

    @Test
    @Order(2)
    public void delete_frees_quota_and_cannot_enable() {
        robotApi.post("/config/save", userToken, RobotTestFixtures.configSaveBody(adminUuid, 1, null, null));

        String robotA = createRobot("it-quota-a");
        JSONObject second = robotApi.post("/create", userToken, RobotTestBodies.createRobot("it-quota-b"));
        IntegrationJson.assertBusinessFailure(second);
        assertTrue(IntegrationJson.msg(second).contains("上限"));

        JSONObject del = robotApi.post("/delete", userToken, RobotTestBodies.uuid(robotA));
        IntegrationJson.assertSuccess(del);

        JSONObject afterDelete = robotApi.post("/create", userToken, RobotTestBodies.createRobot("it-quota-c"));
        IntegrationJson.assertSuccess(afterDelete);
        String robotC = IntegrationJson.data(afterDelete).getJSONObject("robot").getString("uuid");

        JSONObject enableDeleted = robotApi.post("/enable", userToken, RobotTestBodies.uuid(robotA));
        IntegrationJson.assertBusinessFailure(enableDeleted);
        assertTrue(IntegrationJson.msg(enableDeleted).contains("不可恢复") || IntegrationJson.msg(enableDeleted).contains("已删除"));

        cleanupRobot(robotA);
        cleanupRobot(robotC);
    }

    private void cleanupRobot(String robotUuid) {
        if (robotUuid == null)
            return;
        try {
            JSONObject del = robotApi.post("/delete", userToken, RobotTestBodies.uuid(robotUuid));
            if (del != null && del.getIntValue("code") == 0)
                robotService.destroyByAdministrator(robotUuid);
        } catch (Exception ignored) {
        }
    }

    private String createRobot(String name) {
        JSONObject create = robotApi.post("/create", userToken, RobotTestBodies.createRobot(name + "-" + UUID.randomUUID().toString().substring(0, 6)));
        IntegrationJson.assertSuccess(create);
        return IntegrationJson.data(create).getJSONObject("robot").getString("uuid");
    }
}
