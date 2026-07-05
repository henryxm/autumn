package cn.org.autumn.modules.opc.site;

import cn.org.autumn.annotation.PageAware;
import cn.org.autumn.opc.OpcConstants;
import org.springframework.stereotype.Component;

/**
 * 模块站点入口：在此添加自定义页面与扩展。表结构对应的页面由 OpcPages 生成维护。
 */
@Component
public class OpcSite extends OpcPages {

    @PageAware(login = true, page = "opcmanage")
    public String opcmanage = "opc/opcmanage";

    @PageAware(login = false)
    public String integration = "modules/docs/opc-integration";

    public String getOpcManageKey() {
        return getKey("opcmanage");
    }

    public String getIntegrationKey() {
        return getKey("integration");
    }

    public static String opcManagePage() {
        return OpcConstants.MANAGE_PAGE;
    }
}
