package cn.org.autumn.modules.job.site;

import cn.org.autumn.annotation.PageAware;
import cn.org.autumn.site.SiteFactory;
import org.springframework.stereotype.Component;

@Component
public class JobSite implements SiteFactory.Site {

    public final static String siteId = "job";
    public final static String pack = "job";

    @Override
    public String getId() {
        return siteId;
    }

    @Override
    public String getPack() {
        return pack;
    }

    @PageAware(login = true, resource = "modules/job/schedulejob")
    String schedulejob;

    @PageAware(login = true, resource = "modules/job/schedulejoblog")
    String schedulejoblog;
}
