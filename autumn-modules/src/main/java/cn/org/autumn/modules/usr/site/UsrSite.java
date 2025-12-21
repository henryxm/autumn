package cn.org.autumn.modules.usr.site;

import cn.org.autumn.annotation.PageAware;
import cn.org.autumn.site.SiteFactory;
import cn.org.autumn.site.TemplateFactory;
import org.springframework.stereotype.Component;

@Component
public class UsrSite implements SiteFactory.Site, TemplateFactory.Template {
    public final static String siteId = "usr";
    public final static String pack = "usr";

    @PageAware(login = true)
    public String useropen = "modules/usr/useropen";

    @PageAware(login = true)
    public String userloginlog = "modules/usr/userloginlog";

    @PageAware(login = true)
    public String userprofile = "modules/usr/userprofile";

    @PageAware(login = true)
    public String usertoken = "modules/usr/usertoken";

    public String getUserOpenKey() {
        return getKey("useropen");
    }

    public String getUserLoginLogKey() {
        return getKey("userloginlog");
    }

    public String getUserProfileKey() {
        return getKey("userprofile");
    }

    public String getUserTokenKey() {
        return getKey("usertoken");
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
