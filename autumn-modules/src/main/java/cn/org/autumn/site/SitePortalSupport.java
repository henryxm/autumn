package cn.org.autumn.site;

import cn.org.autumn.model.ComplianceFilingItem;
import cn.org.autumn.model.ComplianceFilingType;
import cn.org.autumn.model.SiteFilingView;
import cn.org.autumn.model.SiteLegalLinksView;
import cn.org.autumn.model.SitePortalBranding;
import cn.org.autumn.model.SitePortalConfig;
import cn.org.autumn.model.SitePortalLegalLinks;
import cn.org.autumn.model.SitePortalMeta;
import cn.org.autumn.modules.sys.entity.LoadingTheme;
import cn.org.autumn.modules.sys.service.SysConfigService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

@Component
public class SitePortalSupport {

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private SiteLegalLinksFactory siteLegalLinksFactory;

    public static final String OAUTH2_AUTHORIZE_FAIL_VIEW = "modules/oauth/oauth2authorizefail";

    public void applyToModel(HttpServletRequest request, Model model) {
        if (model == null) {
            return;
        }
        SitePortalConfig config = sysConfigService.getSitePortalConfig();
        SitePortalBranding branding = resolveBranding(config);
        model.addAttribute("siteBranding", branding);
        model.addAttribute("siteName", StringUtils.defaultIfBlank(branding.getSiteName(), sysConfigService.getLoadingBrand()));

        SitePortalMeta meta = config.getMeta() == null ? new SitePortalMeta() : config.getMeta();
        model.addAttribute("siteMeta", meta);
        String copyrightLine = SitePortalConfig.buildCopyrightLine(meta);
        if (StringUtils.isNotBlank(copyrightLine)) {
            model.addAttribute("siteCopyrightLine", copyrightLine);
        }
        if (StringUtils.isNotBlank(meta.getVersionLabel())) {
            model.addAttribute("siteVersionLabel", meta.getVersionLabel().trim());
        }

        SitePortalLegalLinks legal = siteLegalLinksFactory.resolve(config);
        SiteLegalLinksView legalView = toLegalLinksView(request, legal);
        model.addAttribute("siteLegalLinks", legalView);

        List<SiteFilingView> filings = resolveFilings(request, config.getFilings());
        if (!filings.isEmpty()) {
            model.addAttribute("siteFilings", filings);
        }
    }

    /**
     * Inject portal attributes for known public/shell views that bypass {@link AuthPageAttributes}.
     */
    public void applyToModelForView(HttpServletRequest request, Model model, String viewName) {
        if (isAuthPortalView(viewName) || isShellPortalView(viewName)) {
            applyToModel(request, model);
        }
    }

    public static boolean isOauthAuthorizeFailView(String viewName) {
        return OAUTH2_AUTHORIZE_FAIL_VIEW.equals(normalizeViewName(viewName));
    }

    /**
     * Auth / OAuth card pages that render {@code _auth_site_footer} and may be reached via SPM without
     * {@link AuthPageAttributes#apply}.
     */
    public static boolean isAuthPortalView(String viewName) {
        String v = normalizeViewName(viewName);
        return "oauth2/login".equals(v)
                || "oauth2/success".equals(v)
                || "oauth2/callback-error".equals(v)
                || "oauth2/bind-choice".equals(v)
                || "open/oauth2/login".equals(v)
                || "open/oauth2/success".equals(v)
                || OAUTH2_AUTHORIZE_FAIL_VIEW.equals(v);
    }

    public static boolean isShellPortalView(String viewName) {
        String v = normalizeViewName(viewName);
        return "index".equals(v) || "index1".equals(v) || "main".equals(v);
    }

    private static String normalizeViewName(String viewName) {
        if (StringUtils.isBlank(viewName)) {
            return "";
        }
        String v = viewName.trim();
        if (v.startsWith("/")) {
            v = v.substring(1);
        }
        if (v.endsWith(".html")) {
            v = v.substring(0, v.length() - 5);
        }
        return v;
    }

    public SitePortalBranding resolveBranding(SitePortalConfig config) {
        SitePortalBranding branding = config != null && config.getBranding() != null ? config.getBranding() : new SitePortalBranding();
        SitePortalBranding resolved = new SitePortalBranding();
        String siteName = StringUtils.defaultIfBlank(branding.getSiteName(), sysConfigService.getLoadingTheme().getBrand());
        resolved.setSiteName(siteName);
        resolved.setTagline(StringUtils.defaultString(branding.getTagline()));
        String logoUrl = StringUtils.defaultIfBlank(branding.getLogoUrl(), sysConfigService.getLoadingTheme().getLogoUrl());
        resolved.setLogoUrl(logoUrl);
        resolved.setLogoAlt(StringUtils.defaultIfBlank(branding.getLogoAlt(), siteName));
        return resolved;
    }

    public List<SiteFilingView> resolveFilings(HttpServletRequest request, List<ComplianceFilingItem> items) {
        List<SiteFilingView> views = new ArrayList<>();
        if (items == null || items.isEmpty()) {
            return views;
        }
        for (ComplianceFilingItem item : items) {
            if (item == null || StringUtils.isBlank(item.getNumber())) {
                continue;
            }
            SiteFilingView view = new SiteFilingView();
            view.setType(item.getType());
            view.setNumber(item.getNumber().trim());
            view.setPrefix(StringUtils.trimToEmpty(item.getPrefix()));
            view.setSuffix(StringUtils.trimToEmpty(item.getSuffix()));
            view.setShowIcon(item.isShowIcon());
            String url = resolveFilingUrl(item);
            view.setUrl(resolveUrl(request, url));
            view.setExternal(isExternal(url));
            views.add(view);
        }
        return views;
    }

    public String resolveFilingUrl(ComplianceFilingItem item) {
        if (item == null) {
            return "";
        }
        return ComplianceFilingType.parse(item.getType()).resolveUrl(item.getNumber(), item.getUrl());
    }

    public Map<String, String> defaultFilingUrls() {
        Map<String, String> map = new LinkedHashMap<>();
        for (ComplianceFilingType type : ComplianceFilingType.values()) {
            map.put(type.name(), type.defaultUrlTemplate());
        }
        return map;
    }

    public SiteLegalLinksView toLegalLinksView(HttpServletRequest request, SitePortalLegalLinks links) {
        SiteLegalLinksView view = new SiteLegalLinksView();
        if (links == null) {
            return view;
        }
        view.setPrivacyUrl(resolveUrl(request, links.getPrivacyUrl()));
        view.setTermsUrl(resolveUrl(request, links.getTermsUrl()));
        view.setAboutUrl(resolveUrl(request, links.getAboutUrl()));
        view.setHelpUrl(resolveUrl(request, links.getHelpUrl()));
        view.setContactUrl(resolveUrl(request, links.getContactUrl()));
        view.setPrivacyExternal(isExternal(links.getPrivacyUrl()));
        view.setTermsExternal(isExternal(links.getTermsUrl()));
        view.setAboutExternal(isExternal(links.getAboutUrl()));
        view.setHelpExternal(isExternal(links.getHelpUrl()));
        view.setContactExternal(isExternal(links.getContactUrl()));
        return view;
    }

    public void syncLoadingTheme(SitePortalConfig config) {
        if (config == null || !config.isSyncLoadingTheme()) {
            return;
        }
        SitePortalBranding branding = config.getBranding();
        if (branding == null) {
            return;
        }
        LoadingTheme theme = sysConfigService.getLoadingTheme();
        if (StringUtils.isNotBlank(branding.getSiteName())) {
            theme.setBrand(branding.getSiteName().trim());
        }
        if (StringUtils.isNotBlank(branding.getLogoUrl())) {
            theme.setLogoUrl(branding.getLogoUrl().trim());
        }
        theme.normalize();
        sysConfigService.updateLoadingTheme(theme);
    }

    public static String resolveUrl(HttpServletRequest request, String url) {
        if (StringUtils.isBlank(url)) {
            return "";
        }
        String trimmed = url.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        if (trimmed.startsWith("/")) {
            String ctx = request == null ? "" : StringUtils.defaultString(request.getContextPath());
            return ctx + trimmed;
        }
        return trimmed;
    }

    public static boolean isExternal(String url) {
        if (StringUtils.isBlank(url)) {
            return false;
        }
        String trimmed = url.trim();
        return trimmed.startsWith("http://") || trimmed.startsWith("https://");
    }
}
