package cn.org.autumn.modules.qrc.site;

import cn.org.autumn.annotation.PageAware;
import cn.org.autumn.site.SiteFactory;
import cn.org.autumn.site.TemplateFactory;

/**
 * 由代码生成器生成，请勿手动编辑。自定义页面请在 QrcSite 中扩展。
 */
public abstract class QrcPages implements SiteFactory.Site, TemplateFactory.Template {
    public final static String siteId = "qrc";
    public final static String pack = "qrc";

    @PageAware(login = true)
    public String scanticket = "modules/qrc/scanticket";

    @PageAware(login = true)
    public String clientgrant = "modules/qrc/clientgrant";

    public String getScanTicketKey() {
        return getKey("scanticket");
    }

    public String getClientGrantKey() {
        return getKey("clientgrant");
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
