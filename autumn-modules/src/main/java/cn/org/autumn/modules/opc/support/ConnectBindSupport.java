package cn.org.autumn.modules.opc.support;

import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import cn.org.autumn.opl.OplConstants;
import cn.org.autumn.opl.model.OpenAppSnapshot;
import cn.org.autumn.opl.spi.OpenPlatformService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** OPC 绑定辅助：是否本地 OPL（同库同实例，与域名/Session 无关）。 */
@Component
public class ConnectBindSupport {

    @Autowired(required = false)
    private OpenPlatformService openPlatformService;

    /** appId 在本 JVM 的 OPL 中已注册且有效 → 视为同平台，可走 platformUser 幂等绑定。 */
    public boolean isSamePlatform(ConnectAppEntity app) {
        if (app == null || openPlatformService == null || StringUtils.isBlank(app.getAppId())) {
            return false;
        }
        OpenAppSnapshot localApp = openPlatformService.getApp(app.getAppId());
        return localApp != null && localApp.getStatus() == OplConstants.STATUS_ACTIVE;
    }

    public boolean isSameUser(String left, String right) {
        return StringUtils.isNotBlank(left) && StringUtils.isNotBlank(right) && left.equalsIgnoreCase(right);
    }

    public boolean isSameOpenId(String left, String right) {
        return StringUtils.isNotBlank(left) && StringUtils.isNotBlank(right) && left.equals(right);
    }
}
