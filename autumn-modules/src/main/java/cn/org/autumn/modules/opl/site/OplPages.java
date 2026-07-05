package cn.org.autumn.modules.opl.site;

import cn.org.autumn.annotation.PageAware;
import cn.org.autumn.site.SiteFactory;
import cn.org.autumn.site.TemplateFactory;

/**
 * 由代码生成器生成，请勿手动编辑。自定义页面请在 OplSite 中扩展。
 */
public abstract class OplPages implements SiteFactory.Site, TemplateFactory.Template {
    public final static String siteId = "opl";
    public final static String pack = "opl";

    @PageAware(login = true)
    public String openunion = "modules/opl/openunion";

    @PageAware(login = true)
    public String opentoken = "modules/opl/opentoken";

    @PageAware(login = true)
    public String openidentity = "modules/opl/openidentity";

    @PageAware(login = true)
    public String opencode = "modules/opl/opencode";

    @PageAware(login = true)
    public String openapp = "modules/opl/openapp";

    @PageAware(login = true)
    public String openaccount = "modules/opl/openaccount";

    public String getOpenUnionKey() {
        return getKey("openunion");
    }

    public String getOpenTokenKey() {
        return getKey("opentoken");
    }

    public String getOpenIdentityKey() {
        return getKey("openidentity");
    }

    public String getOpenCodeKey() {
        return getKey("opencode");
    }

    public String getOpenAppKey() {
        return getKey("openapp");
    }

    public String getOpenAccountKey() {
        return getKey("openaccount");
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
