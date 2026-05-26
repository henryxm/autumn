package cn.org.autumn.modules.bot.site;

import cn.org.autumn.annotation.PageAware;
import cn.org.autumn.site.SiteFactory;
import cn.org.autumn.site.TemplateFactory;

/**
 * 由代码生成器生成，请勿手动编辑。自定义页面请在 BotSite 中扩展。
 */
public abstract class BotPages implements SiteFactory.Site, TemplateFactory.Template {
    public final static String siteId = "bot";
    public final static String pack = "bot";

    @PageAware(login = true)
    public String robottoken = "modules/bot/robottoken";

    @PageAware(login = true)
    public String robothook = "modules/bot/robothook";

    @PageAware(login = true)
    public String robotconfig = "modules/bot/robotconfig";

    @PageAware(login = true)
    public String robot = "modules/bot/robot";

    public String getRobotTokenKey() {
        return getKey("robottoken");
    }

    public String getRobotHookKey() {
        return getKey("robothook");
    }

    public String getRobotConfigKey() {
        return getKey("robotconfig");
    }

    public String getRobotKey() {
        return getKey("robot");
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
