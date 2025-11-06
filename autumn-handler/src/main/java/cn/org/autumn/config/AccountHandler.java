package cn.org.autumn.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(AccountHandler.class)
public interface AccountHandler {

    //账号正在创建
    default void creating(User obj) throws Exception {
    }

    //账号创建成功
    default void created(User obj) {
    }

    //账号正在注销
    default void canceling(User obj) throws Exception {
    }

    //账号注销成功
    default void canceled(User obj) {
    }

    //正在删除用户
    default void removing(User obj) throws Exception {
    }

    //用户删除成功
    default void removed(User obj) {
    }

    //正在修改用户信息
    default void changing(User obj) throws Exception {
    }

    //正在修改用户信息
    default void changed(User obj) {
    }

    public interface User {
        String getUuid();

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
