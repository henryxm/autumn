package cn.org.autumn.modules.spm.site;

import cn.org.autumn.modules.spm.service.SuperPositionModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class SpmSite {
    public final static String siteId = "202005";
    public final static String prefix = "spm_";
    public final static String suffix = "";
    public final static String MENU_WITH_SPM = "MENU_WITH_SPM";

    public final static String superpositionmodelPageId = "10000";
    public final static String superpositionmodelResourceId = "modules/spm/superpositionmodel";
    public final static String superpositionmodelKeyId = prefix + "superpositionmodel" + suffix;

    @Autowired
    SuperPositionModelService superPositionModelService;

    @PostConstruct
    public void init() {
        superPositionModelService.put(siteId, superpositionmodelPageId, "superpositionmodel", "0", superpositionmodelResourceId, superpositionmodelResourceId, superpositionmodelKeyId, true);
    }
}
