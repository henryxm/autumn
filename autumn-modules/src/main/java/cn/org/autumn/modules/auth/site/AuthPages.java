package cn.org.autumn.modules.auth.site;

import cn.org.autumn.annotation.PageAware;
import cn.org.autumn.site.SiteFactory;
import cn.org.autumn.site.TemplateFactory;

/**
 * 由代码生成器生成，请勿手动编辑。自定义页面请在 AuthSite 中扩展。
 */
public abstract class AuthPages implements SiteFactory.Site, TemplateFactory.Template {
    public final static String siteId = "auth";
    public final static String pack = "auth";

    @PageAware(login = true)
    public String scopedef = "modules/auth/scopedef";

    public String getScopeDefKey() {
        return getKey("scopedef");
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
