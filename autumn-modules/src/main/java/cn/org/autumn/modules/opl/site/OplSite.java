package cn.org.autumn.modules.opl.site;

import cn.org.autumn.annotation.PageAware;
import cn.org.autumn.opl.OplConstants;
import org.springframework.stereotype.Component;

/**
 * 模块站点入口：在此添加自定义页面与扩展。表结构对应的页面由 OplPages 生成维护。
 */
@Component
public class OplSite extends OplPages {

    @PageAware(login = true, page = "oplmanage")
    public String oplmanage = "opl/oplmanage";

    @PageAware(login = false)
    public String integration = "modules/docs/opl-integration";

    public String getOplManageKey() {
        return getKey("oplmanage");
    }

    public String getIntegrationKey() {
        return getKey("integration");
    }

    public static String oplManagePage() {
        return OplConstants.MANAGE_PAGE;
    }
}
