package cn.org.autumn.modules.test.site;

import cn.org.autumn.annotation.PageAware;
import cn.org.autumn.site.SiteFactory;
import org.springframework.stereotype.Component;

@Component
public class TestSite implements SiteFactory.Site {

    public final static String siteId = "test";
    public final static String pack = "test";

    @PageAware(login = true, resource = "modules/test/demoexample")
    String demoexample;

    @Override
    public String getId() {
        return siteId;
    }

    @Override
    public String getPack() {
        return pack;
    }

}
