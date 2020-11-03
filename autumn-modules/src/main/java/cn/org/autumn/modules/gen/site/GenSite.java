package cn.org.autumn.modules.gen.site;

import cn.org.autumn.annotation.PageAware;
import cn.org.autumn.site.SiteFactory;
import org.springframework.stereotype.Component;

@Component
public class GenSite implements SiteFactory.Site {

    public final static String siteId = "gen";
    public final static String pack = "gen";

    @Override
    public String getId() {
        return siteId;
    }

    @Override
    public String getPack() {
        return pack;
    }

    @PageAware(login = true, resource = "modules/gen/generator")
    String generator;

    @PageAware(login = true, resource = "modules/gen/gentype")
    String gentype;
}
