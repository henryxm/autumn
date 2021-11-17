package cn.org.autumn.site;

import cn.org.autumn.annotation.PageAware;
import org.springframework.stereotype.Component;

@Component
public class RootSite implements SiteFactory.Site, TemplateFactory.Template {

    public final static String siteId = "root";
    public final static String pack = "root";

    @Override
    public String getId() {
        return siteId;
    }

    @Override
    public String getPack() {
        return pack;
    }

    @PageAware(login = true)
    String index;

    @PageAware(page = "404")
    String _404;

    @PageAware
    String error;

    @PageAware(login = true)
    String index1;

    @PageAware
    public static String login = "root_login";

    @PageAware
    String main;

    @PageAware
    String loading;

    @PageAware(resource = "oauth2/login", login = false)
    public static String oauth2login = "root_oauth2login";
}
