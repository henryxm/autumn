package cn.org.autumn.modules.job.site;

import cn.org.autumn.modules.spm.service.SuperPositionModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class JobSite {
    public final static String siteId = "202002";
    public final static String prefix = "job_";
    public final static String suffix = "";

    public final static String schedulejobPageId = "10000";
    public final static String schedulejobResourceId = "modules/job/schedulejob";
    public final static String schedulejobKeyId = prefix + "schedulejob" + suffix;

    public final static String schedulejoblogPageId = "10100";
    public final static String schedulejoblogResourceId = "modules/job/schedulejoblog";
    public final static String schedulejoblogKeyId = prefix + "schedulejoblog" + suffix;

    @Autowired
    SuperPositionModelService superPositionModelService;

    @PostConstruct
    public void init() {
        superPositionModelService.put(siteId, schedulejobPageId, "schedulejob", "12r30", schedulejobResourceId, schedulejobResourceId, schedulejobKeyId, true);
        superPositionModelService.put(siteId, schedulejoblogPageId, "schedulejoblog", "0df233", schedulejoblogResourceId, schedulejoblogResourceId, schedulejoblogKeyId, true);
    }
}
