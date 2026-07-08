package cn.org.autumn.modules.sys.service;

import cn.org.autumn.model.ComplianceFilingItem;
import cn.org.autumn.model.ComplianceFilingType;
import cn.org.autumn.model.SitePortalConfig;
import cn.org.autumn.model.SitePortalLegalLinks;
import cn.org.autumn.site.SitePortalSupport;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SitePortalAdminService {

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private SitePortalSupport sitePortalSupport;

    public SitePortalConfig loadConfig() {
        return sysConfigService.getSitePortalConfig();
    }

    public SitePortalConfig saveConfig(SitePortalConfig config) {
        SitePortalConfig saved = sysConfigService.updateSitePortalConfig(config);
        sitePortalSupport.syncLoadingTheme(saved);
        return saved;
    }

    public Map<String, Object> defaults() {
        Map<String, Object> data = new HashMap<>();
        data.put("filingUrls", sitePortalSupport.defaultFilingUrls());
        data.put("legalDefaults", new SitePortalLegalLinks());
        return data;
    }

    public String previewPsbUrl(String number) {
        return previewFilingUrl(ComplianceFilingType.psb.name(), number, "");
    }

    public String previewFilingUrl(String type, String number, String url) {
        ComplianceFilingItem item = new ComplianceFilingItem();
        item.setType(ComplianceFilingType.parse(type).name());
        item.setNumber(StringUtils.defaultString(number));
        item.setUrl(StringUtils.defaultString(url));
        return sitePortalSupport.resolveFilingUrl(item);
    }
}
