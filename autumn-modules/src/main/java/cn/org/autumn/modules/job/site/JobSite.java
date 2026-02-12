package cn.org.autumn.modules.job.site;

import cn.org.autumn.annotation.PageAware;
import cn.org.autumn.site.SiteFactory;
import cn.org.autumn.site.TemplateFactory;
import org.springframework.stereotype.Component;

@Component
public class JobSite implements SiteFactory.Site, TemplateFactory.Template {
    public final static String siteId = "job";
    public final static String pack = "job";

    @PageAware(login = true)
    public String schedulejoblog = "modules/job/schedulejoblog";

    @PageAware(login = true)
    public String schedulejob = "modules/job/schedulejob";

    @PageAware(login = true)
    public String scheduleassign = "modules/job/scheduleassign";

    public String getScheduleJobLogKey() {
        return getKey("schedulejoblog");
    }

    public String getScheduleJobKey() {
        return getKey("schedulejob");
    }

    public String getScheduleAssignKey() {
        return getKey("scheduleassign");
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
