package cn.org.autumn.cluster;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(UserHandler.class)
public interface UserHandler extends ServiceHandler {
    default UserMapping getByUuid(String uuid) {
        return null;
    }

    default UserMapping getByUsername(String username) {
        return null;
    }
}
