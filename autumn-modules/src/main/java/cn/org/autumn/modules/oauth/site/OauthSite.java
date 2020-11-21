package cn.org.autumn.modules.oauth.site;

import cn.org.autumn.annotation.PageAware;
import cn.org.autumn.site.SiteFactory;
import org.springframework.stereotype.Component;

@Component
public class OauthSite implements SiteFactory.Site {
    public final static String siteId = "oauth";
    public final static String pack = "oauth";

    @Override
    public String getId() {
        return siteId;
    }

    @Override
    public String getPack() {
        return pack;
    }

    @PageAware(login = true, resource = "modules/oauth/clientdetails")
    String clientdetails;

    @PageAware(login = false, resource = "modules/oauth/oauth2authorizefail")
    public static String oauth2authorizefail = pack + "_oauth2authorizefail";
}
