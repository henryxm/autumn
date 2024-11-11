package cn.org.autumn.modules.wall.site;

import cn.org.autumn.annotation.PageAware;
import cn.org.autumn.site.SiteFactory;
import cn.org.autumn.site.TemplateFactory;
import org.springframework.stereotype.Component;

@Component
public class WallSite implements SiteFactory.Site, TemplateFactory.Template {
    public final static String siteId = "wall";
    public final static String pack = "wall";

    @PageAware(login = true)
    public String ipvisit = "modules/wall/ipvisit";

    @PageAware(login = true)
    public String ipwhite = "modules/wall/ipwhite";

    @PageAware(login = true)
    public String urlblack = "modules/wall/urlblack";

    @PageAware(login = true)
    public String ipblack = "modules/wall/ipblack";

    @PageAware(login = true)
    public String host = "modules/wall/host";

    @PageAware(login = true)
    public String shield = "modules/wall/shield";

    @PageAware(login = true)
    public String jump = "modules/wall/jump";

    public String getJumpKey() {
        return getKey("jump");
    }

    public String getShieldKey() {
        return getKey("shield");
    }

    public String getIpVisitKey() {
        return getKey("ipvisit");
    }

    public String getIpWhiteKey() {
        return getKey("ipwhite");
    }

    public String getUrlBlackKey() {
        return getKey("urlblack");
    }

    public String getIpBlackKey() {
        return getKey("ipblack");
    }

    public String getHostKey() {
        return getKey("host");
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
