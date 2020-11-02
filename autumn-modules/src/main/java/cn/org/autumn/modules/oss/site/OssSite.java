package cn.org.autumn.modules.oss.site;

import cn.org.autumn.modules.spm.service.SuperPositionModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class OssSite {
    public final static String siteId = "202004";
    public final static String prefix = "oss_";
    public final static String suffix = "";

    public final static String ossPageId = "10000";
    public final static String ossResourceId = "modules/oss/oss";
    public final static String ossKeyId = prefix + "oss" + suffix;

    @Autowired
    SuperPositionModelService superPositionModelService;

    @PostConstruct
    public void init() {
        superPositionModelService.put(siteId, ossPageId, "oss", "0", ossResourceId, ossResourceId, ossKeyId, true);
    }
}
