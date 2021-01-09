package cn.org.autumn.modules.wall.site;

import cn.org.autumn.annotation.PageAware;
import cn.org.autumn.site.SiteFactory;
import org.springframework.stereotype.Component;

@Component
public class WallSite implements SiteFactory.Site {
    public final static String siteId = "wall";
    public final static String pack = "wall";

    @Override
    public String getId() {
        return siteId;
    }

    @Override
    public String getPack() {
        return pack;
    }

    @PageAware(login = true, resource = "modules/wall/host")
    String host;

    @PageAware(login = true, resource = "modules/wall/ipblack")
    String ipblack;

    @PageAware(login = true, resource = "modules/wall/ipwhite")
    String ipwhite;

    @PageAware(login = true, resource = "modules/wall/urlblack")
    String urlblack;
}
