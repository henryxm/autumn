package cn.org.autumn.modules.lan.site;

import cn.org.autumn.annotation.PageAware;
import cn.org.autumn.site.SiteFactory;
import org.springframework.stereotype.Component;

@Component
public class LanSite implements SiteFactory.Site {

    public final static String siteId = "lan";
    public final static String pack = "lan";

    @Override
    public String getId() {
        return siteId;
    }

    @Override
    public String getPack() {
        return pack;
    }

    @PageAware(login = true, resource = "modules/lan/language")
    String language;

    @PageAware(login = true, resource = "modules/lan/supportedlanguage")
    String supportedlanguage;
}
