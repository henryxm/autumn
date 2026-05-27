package cn.org.autumn.integration.robot;

import cn.org.autumn.integration.base.IntegrationTest;
import cn.org.autumn.modules.bot.service.RobotService;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 机器人集成测试基类：用例前清空管理员名下机器人，避免配额/软删门禁跨用例干扰。
 */
public abstract class RobotIntegrationTest extends IntegrationTest {

    @Autowired
    protected RobotService robotService;

    @BeforeEach
    void purgeAdminRobotsBeforeEach() {
        if (StringUtils.isNotBlank(adminUuid)) {
            robotService.purgeAllRobotsForOwner(adminUuid);
        }
    }
}
