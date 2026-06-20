package cn.org.autumn.model;

import java.io.Serializable;

/**
 * 无 Session API 调用的统一调用者视图（接口）。
 * <p>
 * 实现类为业务实体本身（如 {@code SysUserEntity}、{@code RobotEntity}），类型不同、uuid 不得相同；
 * 业务表 {@code user} 列可存任一侧 uuid，见 {@code docs/AI_DUAL_KEY.md} §1.1。
 * <ul>
 *   <li>{@link #getUuid()}：当前调用者（真人或机器人；二者 uuid 全局互斥，见 {@code UuidNamespaceService}）</li>
 *   <li>{@link #getSubject()}：数据权限主体（真人为自身 uuid，机器人为主人 uuid）</li>
 *   <li>{@link #getOwner()}：机器人所属用户 uuid；真人常为 null</li>
 *   <li>{@link #isRobot()}：是否机器人调用</li>
 * </ul>
 */
public interface UserContext extends Serializable {

    String getUuid();

    /**
     * 机器人所属用户 uuid；人类用户对应 {@code Account} 视图时常为 null，数据权限请用 {@link #getSubject()}。
     */
    String getOwner();

    boolean isRobot();

    String getNickname();

    String getIcon();

    default String getHash() {
        return "";
    }

    int getStatus();

    default boolean isBlack() {
        return false;
    }

    default int getVerify() {
        return 0;
    }

    default String getUsername() {
        return getUuid();
    }

    default String getAccess() {
        return "";
    }

    default String getScopes() {
        return "";
    }

    /**
     * 数据权限主体：真人为自身 {@link #getUuid()}，机器人为 {@link #getOwner()}。
     */
    default String getSubject() {
        if (isRobot())
            return getOwner();
        return getUuid();
    }

    default boolean isActive() {
        return getStatus() >= 1;
    }
}
