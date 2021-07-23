package cn.org.autumn.cluster;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnClass(UserHandler.class)
public interface UserHandler extends ServiceHandler {
    default UserMapping getByUuid(String uuid) {
        return null;
    }

    default UserMapping getByUsername(String username) {
        return null;
    }
}
