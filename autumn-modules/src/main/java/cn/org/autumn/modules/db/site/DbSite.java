package cn.org.autumn.modules.db.site;

import cn.org.autumn.annotation.PageAware;
import cn.org.autumn.site.SiteFactory;
import cn.org.autumn.site.TemplateFactory;
import org.springframework.stereotype.Component;

@Component
public class DbSite implements SiteFactory.Site, TemplateFactory.Template {
    public final static String siteId = "db";
    public final static String pack = "db";

    @PageAware(login = true)
    public String databasebackupstrategy = "modules/db/databasebackupstrategy";

    @PageAware(login = true)
    public String databasebackup = "modules/db/databasebackup";

    @PageAware(login = true)
    public String databasebackupupload = "modules/db/databasebackupupload";

    public String getDatabaseBackupUploadKey() {
        return getKey("databasebackupupload");
    }

    public String getDatabaseBackupStrategyKey() {
        return getKey("databasebackupstrategy");
    }

    public String getDatabaseBackupKey() {
        return getKey("databasebackup");
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
