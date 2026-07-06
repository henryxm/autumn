package cn.org.autumn.modules.client.site;

import cn.org.autumn.annotation.PageAware;
import cn.org.autumn.site.SiteFactory;
import cn.org.autumn.site.TemplateFactory;

/**
 * 由代码生成器生成，请勿手动编辑。自定义页面请在 ClientSite 中扩展。
 */
public abstract class ClientPages implements SiteFactory.Site, TemplateFactory.Template {
    public final static String siteId = "client";
    public final static String pack = "client";

    @PageAware(login = true)
    public String weboauthbind = "modules/client/weboauthbind";

    @PageAware(login = true)
    public String weboauthcombine = "modules/client/weboauthcombine";

    @PageAware(login = true)
    public String webauthentication = "modules/client/webauthentication";

    public String getWebOauthBindKey() {
        return getKey("weboauthbind");
    }

    public String getWebOauthCombineKey() {
        return getKey("weboauthcombine");
    }

    public String getWebAuthenticationKey() {
        return getKey("webauthentication");
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
