package cn.org.autumn.modules.wall.site;

import cn.org.autumn.modules.spm.service.SuperPositionModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class WallSite {
    public final static String siteId = "100002";
    public final static String pack = "wall";
    public final static String prefix = pack + "_";
    public final static String suffix = "";

    public final static String host = "host";
    public final static String hostPageId = "1001";
    public final static String hostProductId = "0";
    public final static String hostResourceId = "modules/" + pack + "/" + host;
    public final static String hostKeyId = prefix + host + suffix;

    public final static String ipblack = "ipblack";
    public final static String ipblackPageId = "1002";
    public final static String ipblackProductId = "0";
    public final static String ipblackResourceId = "modules/" + pack + "/" + ipblack;
    public final static String ipblackKeyId = prefix + ipblack + suffix;

    public final static String ipwhite = "ipwhite";
    public final static String ipwhitePageId = "1003";
    public final static String ipwhiteProductId = "0";
    public final static String ipwhiteResourceId = "modules/" + pack + "/" + ipwhite;
    public final static String ipwhiteKeyId = prefix + ipwhite + suffix;

    public final static String urlblack = "urlblack";
    public final static String urlblackPageId = "1004";
    public final static String urlblackProductId = "0";
    public final static String urlblackResourceId = "modules/" + pack + "/" + urlblack;
    public final static String urlblackKeyId = prefix + urlblack + suffix;

    @Autowired
    SuperPositionModelService superPositionModelService;

    @PostConstruct
    public void init() {
        superPositionModelService.put(siteId, hostPageId, host, hostProductId, hostResourceId, hostResourceId, hostKeyId, true);
        superPositionModelService.put(siteId, ipblackPageId, ipblack, ipblackProductId, ipblackResourceId, ipblackResourceId, ipblackKeyId, true);
        superPositionModelService.put(siteId, ipwhitePageId, ipwhite, ipwhiteProductId, ipwhiteResourceId, ipwhiteResourceId, ipwhiteKeyId, true);
        superPositionModelService.put(siteId, urlblackPageId, urlblack, urlblackProductId, urlblackResourceId, urlblackResourceId, urlblackKeyId, true);
    }
}
