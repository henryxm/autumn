package cn.org.autumn.modules.sys.support;

import cn.org.autumn.modules.sys.shiro.ShiroSessionService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import org.apache.commons.lang3.StringUtils;

/** 显式登出：清理 Shiro 身份并写入强制重登标记，阻断 RememberMe 静默恢复。 */
public final class SysLogoutSupport {

    private SysLogoutSupport() {
    }

    public static void logoutAndForceReauth(ShiroSessionService shiroSessionService) {
        String userUuid = ShiroUtils.getUserUuid();
        if (shiroSessionService != null && StringUtils.isNotBlank(userUuid)) {
            shiroSessionService.markForceLogoutForUser(userUuid);
        }
        ShiroUtils.logout();
    }
}
