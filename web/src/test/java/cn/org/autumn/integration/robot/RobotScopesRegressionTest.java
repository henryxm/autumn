package cn.org.autumn.integration.robot;

import cn.org.autumn.integration.base.IntegrationTest;
import cn.org.autumn.integration.support.IntegrationJson;
import cn.org.autumn.integration.support.RobotTestBodies;
import cn.org.autumn.integration.support.RobotTestContext;
import cn.org.autumn.integration.support.RobotTestFixtures;
import cn.org.autumn.modules.bot.entity.RobotEntity;
import cn.org.autumn.modules.bot.service.RobotService;
import cn.org.autumn.modules.bot.support.RobotScopes;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 机器人 scopes 与 {@link RobotScopes#MESSAGE_PUSH} 权限回归。
 */
public class RobotScopesRegressionTest extends IntegrationTest {

    @Autowired
    private RobotService robotService;

    @Test
    public void push_rejectsWhenScopeExcludesMessagePush() {
        RobotTestContext ctx = RobotTestFixtures.createRobot(robotApi, userToken, "it-scope");

        RobotEntity robot = robotService.getByUuid(ctx.getRobotUuid());
        robot.setScopes("hook.only,other.read");
        robotService.updateById(robot);

        JSONObject resp = robotApi.post("/message/push", ctx.getRobotToken(),
                RobotTestBodies.messagePush("demo.ping", new HashMap<>(), "idem-scope-" + UUID.randomUUID()));
        IntegrationJson.assertBusinessFailure(resp);
        assertTrue(IntegrationJson.msg(resp).contains("message.push")
                || IntegrationJson.msg(resp).contains("权限"));
    }

    @Test
    public void push_allowedWhenScopeIncludesMessagePush() {
        RobotTestContext ctx = RobotTestFixtures.createRobot(robotApi, userToken, "it-scope-ok");

        RobotEntity robot = robotService.getByUuid(ctx.getRobotUuid());
        robot.setScopes(RobotScopes.MESSAGE_PUSH);
        robotService.updateById(robot);

        JSONObject resp = robotApi.post("/message/push", ctx.getRobotToken(),
                RobotTestBodies.messagePush("demo.ping", new HashMap<>(), "idem-scope-ok-" + UUID.randomUUID()));
        IntegrationJson.assertSuccess(resp);
    }
}
