package cn.org.autumn.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(AccountHandler.class)
public interface AccountHandler {

    //账号正在创建（含机器人，见 User#isRobot）
    default void creating(Account obj) throws Exception {
    }

    //账号创建成功
    default void created(Account obj) {
    }

    //账号正在注销（人类用户）
    default void canceling(Account obj) throws Exception {
    }

    //账号注销成功
    default void canceled(Account obj) {
    }

    //正在删除用户（人类物理删除；机器人软删见 deleting）
    default void removing(Account obj) throws Exception {
    }

    //用户删除成功
    default void removed(Account obj) {
    }

    //正在修改用户信息
    default void changing(Account obj) throws Exception {
    }

    //用户信息修改成功
    default void changed(Account obj) {
    }

    //机器人正在停用（仅 isRobot 为 true 时由框架触发）
    default void disabling(Account obj) throws Exception {
    }

    default void disabled(Account obj) {
    }

    //机器人正在启用
    default void enabling(Account obj) throws Exception {
    }

    default void enabled(Account obj) {
    }

    //机器人正在软删除
    default void deleting(Account obj) throws Exception {
    }

    default void deleted(Account obj) {
    }

    //机器人正在销毁（不可逆）
    default void destroying(Account obj) throws Exception {
    }

    default void destroyed(Account obj) {
    }

    /**
     * 统一账号视图：人类用户（sys_user）与机器人（bot_robot）均实现本接口；
     * 通过 {@link #isRobot()} 区分，扩展实现中按需过滤。
     */
    interface Account {
        String getUuid();

        /**
         * true 表示 bot_robot 实体，非 sys_user 表行。
         */
        default boolean isRobot() {
            return false;
        }

        /**
         * 机器人所属用户 uuid；人类用户返回 null。数据权限主体请用 UserContext#getSubject()，勿以 null 判断是否为人类（应使用 {@link #isRobot()}）。
         */
        default String getOwner() {
            return null;
        }

        default String getNickname() {
            return null;
        }

        default String getPassword() {
            return null;
        }

        default String getSalt() {
            return null;
        }

        default String getEmail() {
            return null;
        }

        default String getMobile() {
            return null;
        }

        default String getQq() {
            return null;
        }

        default String getWeixin() {
            return null;
        }

        default String getIdCard() {
            return null;
        }

        default String getIcon() {
            return null;
        }

        default int getStatus() {
            return 0;
        }

        default int getVerify() {
            return 0;
        }
    }
}
