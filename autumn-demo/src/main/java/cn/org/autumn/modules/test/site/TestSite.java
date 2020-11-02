package cn.org.autumn.modules.test.site;

import cn.org.autumn.modules.spm.service.SuperPositionModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class TestSite {

    public final static String siteId = "10000";
    public final static String pack = "test";
    public final static String prefix = pack + "_";
    public final static String suffix = "";

    public final static String demoexample = "demoexample";
    public final static String demoexamplePageId = "1001";
    public final static String demoexampleProductId = "0";
    public final static String demoexampleResourceId = "modules/" + pack + "/" + demoexample;
    public final static String demoexampleKeyId = prefix + demoexample + suffix;
    @Autowired
    SuperPositionModelService superPositionModelService;

    @PostConstruct
    public void init() {
        superPositionModelService.put(siteId, demoexamplePageId, demoexample, demoexampleProductId, demoexampleResourceId, demoexampleResourceId, demoexampleKeyId, true);
    }
}
