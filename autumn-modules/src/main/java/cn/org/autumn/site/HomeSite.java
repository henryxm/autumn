package cn.org.autumn.site;

import cn.org.autumn.annotation.PageAware;
import org.springframework.stereotype.Component;

@Component
public class HomeSite implements SiteFactory.Site {

    public final static String siteId = "1";
    public final static String pack = "root";

    @PageAware(login = true)
    String index;

    @PageAware(page = "404")
    String _404;

    @PageAware
    String error;

    @PageAware(login = true)
    String index1;

    @PageAware
    String login;

    @PageAware
    String main;

    @Override
    public String getId() {
        return siteId;
    }

    @Override
    public String getPack() {
        return pack;
    }
}
