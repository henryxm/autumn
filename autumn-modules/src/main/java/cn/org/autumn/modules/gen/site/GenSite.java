package cn.org.autumn.modules.gen.site;

import cn.org.autumn.modules.spm.service.SuperPositionModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class GenSite {
    public final static String siteId = "202001";
    public final static String prefix = "gen_";
    public final static String suffix = "";

    public final static String generatorPageId = "0010";
    public final static String generatorResourceId = "modules/gen/generator";
    public final static String generatorKeyId = prefix + "generator" + suffix;

    public final static String gentypePageId = "0011";
    public final static String gentypeResourceId = "modules/gen/gentype";
    public final static String gentypeKeyId = prefix + "gentype" + suffix;

    @Autowired
    SuperPositionModelService superPositionModelService;

    @PostConstruct
    public void init() {
        superPositionModelService.put(siteId, generatorPageId, "generator", "100", generatorResourceId, generatorResourceId, generatorKeyId, true);
        superPositionModelService.put(siteId, gentypePageId, "gentype", "101", gentypeResourceId, gentypeResourceId, gentypeKeyId, true);
    }
}
