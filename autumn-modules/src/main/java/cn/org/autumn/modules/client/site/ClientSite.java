package cn.org.autumn.modules.client.site;

import cn.org.autumn.annotation.PageAware;
import cn.org.autumn.modules.client.support.OauthRpAdminConstants;
import org.springframework.stereotype.Component;

/**
 * 模块站点入口：在此添加自定义页面与扩展。表结构对应的页面由 ClientPages 生成维护。
 */
@Component
public class ClientSite extends ClientPages {

    @PageAware(login = true, page = "oauthrpmanage")
    public String oauthrpmanage = "client/oauthrpmanage";

    public String getOauthRpManageKey() {
        return getKey("oauthrpmanage");
    }

    public static String oauthRpManagePage() {
        return OauthRpAdminConstants.MANAGE_RP_PAGE;
    }
}
