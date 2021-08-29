package cn.org.autumn.modules.test.site;

import cn.org.autumn.annotation.PageAware;
import cn.org.autumn.site.SiteFactory;
import cn.org.autumn.site.TemplateFactory;
import org.springframework.stereotype.Component;

@Component
public class TestSite implements SiteFactory.Site, TemplateFactory.Template {
    public final static String siteId = "test";
    public final static String pack = "test";

    @PageAware(login = true)
    public String demoexample = "modules/test/demoexample";

    public String getDemoExampleKey() {
        return getKey("demoexample");
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
