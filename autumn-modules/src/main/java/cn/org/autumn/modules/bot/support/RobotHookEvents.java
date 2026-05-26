package cn.org.autumn.modules.bot.support;

public final class RobotHookEvents {

    public static final String ROBOT_CREATED = "robot.created";
    public static final String ROBOT_DISABLED = "robot.disabled";
    public static final String ROBOT_ENABLED = "robot.enabled";
    public static final String ROBOT_DELETED = "robot.deleted";
    public static final String ROBOT_DESTROYED = "robot.destroyed";

    /** 入站消息通配（Hook / 流程订阅可用 {@code *} 或具体类型如 {@code order.paid}） */
    public static final String MESSAGE_WILDCARD = "*";

    private RobotHookEvents() {
    }
}
