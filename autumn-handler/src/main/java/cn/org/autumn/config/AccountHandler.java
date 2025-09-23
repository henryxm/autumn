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

    public interface User {
        String getUuid();

        String getNickname();

        String getPassword();

        String getSalt();

        String getEmail();

        String getMobile();

        String getQq();

        String getWeixin();

        String getIdCard();

        String getIcon();
    }
}
