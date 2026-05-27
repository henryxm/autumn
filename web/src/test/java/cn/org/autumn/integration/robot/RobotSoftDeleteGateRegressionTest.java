package cn.org.autumn.integration.robot;

import cn.org.autumn.integration.support.IntegrationJson;
import cn.org.autumn.integration.support.RobotTestBodies;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 软删门禁：软删数超过全局 maxSoftDeletePending（默认 5）禁止创建。
 */
public class RobotSoftDeleteGateRegressionTest extends RobotIntegrationTest {

    private String releasedRobotUuid;

    @AfterEach
    void cleanupGateFixtures() {
        if (StringUtils.isNotBlank(releasedRobotUuid)) {
            try {
                JSONObject del = robotApi.post("/delete", userToken, RobotTestBodies.uuid(releasedRobotUuid));
                if (del != null && del.getIntValue("code") == 0)
                    robotService.destroyByAdministrator(releasedRobotUuid);
            } catch (Exception ignored) {
            }
            releasedRobotUuid = null;
        }
        robotService.purgeAllRobotsForOwner(adminUuid);
    }

    @Test
    public void soft_delete_gate_blocks_then_releases_after_admin_destroy() throws Exception {
        List<String> deletedUuids = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            String name = "it-gate-" + i + "-" + UUID.randomUUID().toString().substring(0, 4);
            JSONObject create = robotApi.post("/create", userToken, RobotTestBodies.createRobot(name));
            IntegrationJson.assertSuccess(create);
            String uuid = IntegrationJson.data(create).getJSONObject("robot").getString("uuid");
            JSONObject del = robotApi.post("/delete", userToken, RobotTestBodies.uuid(uuid));
            IntegrationJson.assertSuccess(del);
            deletedUuids.add(uuid);
        }

        JSONObject blocked = robotApi.post("/create", userToken, RobotTestBodies.createRobot("it-gate-blocked"));
        IntegrationJson.assertBusinessFailure(blocked);
        assertTrue(IntegrationJson.msg(blocked).contains("软删除"));

        JSONObject config = robotApi.post("/config/get", userToken, null);
        IntegrationJson.assertSuccess(config);
        JSONObject data = IntegrationJson.data(config);
        assertTrue(data.getIntValue("pendingSoftDeleted") > data.getIntValue("effectiveMaxSoftDeletePending"));
        assertTrue(data.getBooleanValue("softDeleteCreateBlocked"));

        robotService.destroyByAdministrator(deletedUuids.get(0));

        JSONObject afterPurge = robotApi.post("/create", userToken, RobotTestBodies.createRobot("it-gate-ok"));
        IntegrationJson.assertSuccess(afterPurge);
        releasedRobotUuid = IntegrationJson.data(afterPurge).getJSONObject("robot").getString("uuid");

        JSONObject configAfter = robotApi.post("/config/get", userToken, null);
        IntegrationJson.assertSuccess(configAfter);
        assertFalse(IntegrationJson.data(configAfter).getBooleanValue("softDeleteCreateBlocked"));
    }
}
