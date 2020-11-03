package cn.org.autumn.modules.oss.site;

import cn.org.autumn.annotation.PageAware;
import cn.org.autumn.site.SiteFactory;
import org.springframework.stereotype.Component;

@Component
public class OssSite implements SiteFactory.Site {
    public final static String siteId = "oss";
    public final static String pack = "oss";

    @Override
    public String getId() {
        return siteId;
    }

    @Override
    public String getPack() {
        return pack;
    }

    @PageAware(login = true, resource = "modules/oss/oss")
    String oss;
}
