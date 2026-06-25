package cn.org.autumn.cluster;

import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * 通过用户的UUID获取用户的权限
 */
@Component
@ConditionalOnMissingBean(PermissionHandler.class)
public interface PermissionHandler {
    Set<String> getPermissions(String userUuid);
}
