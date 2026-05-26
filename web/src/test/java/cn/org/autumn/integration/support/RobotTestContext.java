package cn.org.autumn.integration.support;

/**
 * 集成测试内创建的机器人 + 首个 {@code rbt_} 令牌。
 */
public class RobotTestContext {

    private final String robotUuid;
    private final String robotToken;

    public RobotTestContext(String robotUuid, String robotToken) {
        this.robotUuid = robotUuid;
        this.robotToken = robotToken;
    }

    public String getRobotUuid() {
        return robotUuid;
    }

    public String getRobotToken() {
        return robotToken;
    }
}
