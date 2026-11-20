package cn.org.autumn.site;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import cn.org.autumn.model.ComplianceFilingItem;
import cn.org.autumn.model.ComplianceFilingType;
import cn.org.autumn.model.SitePortalConfig;
import cn.org.autumn.modules.sys.entity.LoadingTheme;
import cn.org.autumn.modules.sys.service.SysConfigService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.when;
import cn.org.autumn.model.SitePortalBranding;
import cn.org.autumn.model.SitePortalChaoranxin;
import cn.org.autumn.model.SitePortalLegalLinks;
import java.util.Collections;
import org.springframework.ui.ExtendedModelMap;

@RunWith(MockitoJUnitRunner.class)
public class SitePortalSupportTest {

    @Mock
    private SysConfigService sysConfigService;

    @Mock
    private SiteLegalLinksFactory siteLegalLinksFactory;

    @InjectMocks
    private SitePortalSupport support;

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(support, "sysConfigService", sysConfigService);
        ReflectionTestUtils.setField(support, "siteLegalLinksFactory", siteLegalLinksFactory);
    }

    @Test
    public void resolveFilingUrlUsesPsbTemplate() {
        ComplianceFilingItem item = new ComplianceFilingItem();
        item.setType(ComplianceFilingType.psb.name());
        item.setNumber("川公网安备51010802000202号");
        String url = support.resolveFilingUrl(item);
        assertTrue(url.contains("recordcode=51010802000202"));
    }

    @Test
    public void resolveFilingUrlUsesIcpDefaultWhenUrlBlank() {
        ComplianceFilingItem item = new ComplianceFilingItem();
        item.setType(ComplianceFilingType.icp.name());
        item.setNumber("蜀ICP备2021014482号-17");
        assertEquals(ComplianceFilingType.DEFAULT_ICP_URL, support.resolveFilingUrl(item));
    }

    @Test
    public void resolveFilingsPopulatesDefaultUrlForIcp() {
        ComplianceFilingItem item = new ComplianceFilingItem();
        item.setType(ComplianceFilingType.icp.name());
        item.setNumber("蜀ICP备2021014482号-17");
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertEquals(ComplianceFilingType.DEFAULT_ICP_URL, support.resolveFilings(request, Collections.singletonList(item)).get(0).getUrl());
    }

    @Test
    public void resolveBrandingFallsBackToLoadingTheme() {
        SitePortalConfig config = new SitePortalConfig();
        LoadingTheme theme = new LoadingTheme();
        theme.setBrand("Autumn Platform");
        when(sysConfigService.getLoadingTheme()).thenReturn(theme);
        assertEquals("Autumn Platform", support.resolveBranding(config).getSiteName());
    }

    @Test
    public void resolveUrlAddsContextPathForRelativeLinks() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/ctx");
        assertEquals("/ctx/user/privacy.html", SitePortalSupport.resolveUrl(request, "/user/privacy.html"));
    }

    @Test
    public void applyToModelResolvesRelativeLogoUrlWithContextPath() {
        SitePortalConfig config = new SitePortalConfig();
        SitePortalBranding branding = new SitePortalBranding();
        branding.setSiteName("Demo");
        branding.setLogoUrl("/statics/img/logo.png");
        config.setBranding(branding);
        LoadingTheme theme = new LoadingTheme();
        theme.setBrand("Autumn Platform");
        when(sysConfigService.getSitePortalConfig()).thenReturn(config);
        when(sysConfigService.getLoadingTheme()).thenReturn(theme);
        when(sysConfigService.getLoadingBrand()).thenReturn("Demo");
        when(siteLegalLinksFactory.resolve(config)).thenReturn(new SitePortalLegalLinks());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/ctx");
        ExtendedModelMap model = new ExtendedModelMap();
        support.applyToModel(request, model);
        SitePortalBranding applied = (SitePortalBranding) model.get("siteBranding");
        assertEquals("/ctx/statics/img/logo.png", applied.getLogoUrl());
        assertEquals("Demo", model.get("siteName"));
    }

    @Test
    public void resolveFilingsPropagatesPrefixAndSuffix() {
        ComplianceFilingItem item = new ComplianceFilingItem();
        item.setType(ComplianceFilingType.telecom.name());
        item.setPrefix("增值电信业务经营许可证:");
        item.setNumber("川B2-20231966");
        item.setSuffix("号");
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertEquals("增值电信业务经营许可证:", support.resolveFilings(request, Collections.singletonList(item)).get(0).getPrefix());
        assertEquals("号", support.resolveFilings(request, Collections.singletonList(item)).get(0).getSuffix());
    }

    @Test
    public void previewFilingUrlMatchesResolveFilingsUrl() {
        ComplianceFilingItem item = new ComplianceFilingItem();
        item.setType(ComplianceFilingType.psb.name());
        item.setNumber("川公网安备51010802000202号");
        MockHttpServletRequest request = new MockHttpServletRequest();
        String resolved = support.resolveFilingUrl(item);
        assertEquals(resolved, support.resolveFilings(request, Collections.singletonList(item)).get(0).getUrl());
    }

    @Test
    public void viewNameHelpersRecognizePortalViews() {
        assertTrue(SitePortalSupport.isOauthAuthorizeFailView("/modules/oauth/oauth2authorizefail.html"));
        assertTrue(SitePortalSupport.isOauthAuthorizeFailView("modules/oauth/oauth2authorizefail"));
        assertFalse(SitePortalSupport.isOauthAuthorizeFailView("oauth2/callback-error"));
        assertTrue(SitePortalSupport.isAuthPortalView("oauth2/login"));
        assertTrue(SitePortalSupport.isAuthPortalView("/open/oauth2/success.html"));
        assertTrue(SitePortalSupport.isAuthPortalView("oauth2/callback-error"));
        assertTrue(SitePortalSupport.isAuthPortalView("modules/oauth/oauth2authorizefail"));
        assertFalse(SitePortalSupport.isAuthPortalView("login"));
        assertTrue(SitePortalSupport.isShellPortalView("index"));
        assertTrue(SitePortalSupport.isShellPortalView("/index1.html"));
        assertTrue(SitePortalSupport.isShellPortalView("main.html"));
        assertTrue(SitePortalSupport.isShellPortalView("ops/discover"));
        assertFalse(SitePortalSupport.isShellPortalView("login"));
    }

    @Test
    public void applyDiscoverPublicModelExposesOnlyPublicFields() {
        SitePortalConfig config = new SitePortalConfig();
        SitePortalBranding branding = new SitePortalBranding();
        branding.setSiteName("示例站");
        branding.setLogoUrl("/statics/img/logo.png");
        config.setBranding(branding);
        SitePortalChaoranxin cx = new SitePortalChaoranxin();
        cx.setOfficialUrl("https://chaoran.xin/");
        cx.setInstallQrUrl("https://chaoran.xin/download");
        config.setChaoranxin(cx);
        LoadingTheme theme = new LoadingTheme();
        theme.setBrand("Autumn");
        when(sysConfigService.getSitePortalConfig()).thenReturn(config);
        when(sysConfigService.getLoadingTheme()).thenReturn(theme);
        when(sysConfigService.getLoadingBrand()).thenReturn("示例站");
        when(sysConfigService.isSsl()).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("https");
        request.setServerName("ops.example.com");
        request.setServerPort(443);
        request.setContextPath("");
        ExtendedModelMap model = new ExtendedModelMap();
        support.applyDiscoverPublicModel(request, model);
        assertEquals("示例站", model.get("siteName"));
        assertEquals("/statics/img/logo.png", model.get("logoUrl"));
        assertEquals("https://chaoran.xin/", model.get("officialUrl"));
        assertEquals("https://chaoran.xin/download", model.get("installQrUrl"));
        assertEquals("https://ops.example.com/ops/discover", model.get("discoverAbsoluteUrl"));
        assertFalse(model.containsAttribute("siteId"));
        assertFalse(model.containsAttribute("fingerprint"));
        assertFalse(model.containsAttribute("opsCert"));
    }
}
