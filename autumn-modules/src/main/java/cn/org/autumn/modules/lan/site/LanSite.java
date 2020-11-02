package cn.org.autumn.modules.lan.site;

import cn.org.autumn.modules.spm.service.SuperPositionModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class LanSite {

    public final static String siteId = "202003";
    public final static String prefix = "lan_";
    public final static String suffix = "";

    public final static String languagePageId = "10000";
    public final static String languageResourceId = "modules/lan/language";
    public final static String languageKeyId = prefix + "language" + suffix;

    @Autowired
    SuperPositionModelService superPositionModelService;

    @PostConstruct
    public void init() {
        superPositionModelService.put(siteId, languagePageId, "language", "0", languageResourceId, languageResourceId, languageKeyId, true);
    }
}
