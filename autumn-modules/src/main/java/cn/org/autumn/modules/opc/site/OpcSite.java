package cn.org.autumn.modules.opc.site;

import cn.org.autumn.annotation.PageAware;
import cn.org.autumn.opc.OpcConstants;
import org.springframework.stereotype.Component;

/**
 * 模块站点入口：在此添加自定义页面与扩展。表结构对应的页面由 OpcPages 生成维护。
 * <p>
 * {@code connectbind} 已由 {@code opc/connectbind.html} 友好页覆盖（见 {@link cn.org.autumn.opc.OpcConstants#CONNECTBIND_MANAGE_PAGE}）。
 */
@Component
public class OpcSite extends OpcPages {

    @PageAware(login = true, page = "connectbind")
    public String connectbind = "opc/connectbind";

    @PageAware(login = true, page = "opcmanage")
    public String opcmanage = "opc/opcmanage";

    @PageAware(login = false)
    public String integration = "modules/docs/opc-integration";

    @PageAware(login = false, resource = OpcConstants.OAUTH2_LOGIN_PAGE)
    public String rpLogin = OpcConstants.OAUTH2_LOGIN_PAGE;

    @PageAware(login = false, resource = OpcConstants.OAUTH2_SUCCESS_PAGE)
    public String rpSuccess = OpcConstants.OAUTH2_SUCCESS_PAGE;

    public String getOpcManageKey() {
        return getKey("opcmanage");
    }

    public String getConnectBindManageKey() {
        return getKey("connectbind");
    }

    public String getIntegrationKey() {
        return getKey("integration");
    }

    public static String opcManagePage() {
        return OpcConstants.MANAGE_PAGE;
    }
}
