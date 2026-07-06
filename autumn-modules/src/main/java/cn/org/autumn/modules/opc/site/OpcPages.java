package cn.org.autumn.modules.opc.site;

import cn.org.autumn.annotation.PageAware;
import cn.org.autumn.site.SiteFactory;
import cn.org.autumn.site.TemplateFactory;

/**
 * 由代码生成器生成，请勿手动编辑。自定义页面请在 OpcSite 中扩展。
 */
public abstract class OpcPages implements SiteFactory.Site, TemplateFactory.Template {
    public final static String siteId = "opc";
    public final static String pack = "opc";

    @PageAware(login = true)
    public String connectbind = "modules/opc/connectbind";

    @PageAware(login = true)
    public String connectapp = "modules/opc/connectapp";

    public String getConnectBindKey() {
        return getKey("connectbind");
    }

    public String getConnectAppKey() {
        return getKey("connectapp");
    }

    @Override
    public String getId() {
        return siteId;
    }

    @Override
    public String getPack() {
        return pack;
    }
}
