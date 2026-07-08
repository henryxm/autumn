package cn.org.autumn.modules.sys.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.org.autumn.model.ComplianceFilingItem;
import cn.org.autumn.model.ComplianceFilingType;
import cn.org.autumn.model.SitePortalConfig;
import cn.org.autumn.site.SitePortalSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SitePortalAdminServiceTest {

    @Mock
    private SysConfigService sysConfigService;

    @Mock
    private SitePortalSupport sitePortalSupport;

    @InjectMocks
    private SitePortalAdminService service;

    @Test
    public void previewFilingUrlDelegatesToSupportWithParsedType() {
        when(sitePortalSupport.resolveFilingUrl(any(ComplianceFilingItem.class)))
                .thenReturn(ComplianceFilingType.DEFAULT_ICP_URL);
        assertEquals(ComplianceFilingType.DEFAULT_ICP_URL, service.previewFilingUrl("icp", "蜀ICP备2021014482号-17", ""));
        ArgumentCaptor<ComplianceFilingItem> captor = ArgumentCaptor.forClass(ComplianceFilingItem.class);
        verify(sitePortalSupport).resolveFilingUrl(captor.capture());
        assertEquals(ComplianceFilingType.icp.name(), captor.getValue().getType());
        assertEquals("蜀ICP备2021014482号-17", captor.getValue().getNumber());
    }

    @Test
    public void previewFilingUrlPrefersConfiguredUrlOnItem() {
        when(sitePortalSupport.resolveFilingUrl(any(ComplianceFilingItem.class)))
                .thenAnswer(invocation -> {
                    ComplianceFilingItem item = invocation.getArgument(0);
                    return item.getUrl();
                });
        assertEquals("https://custom.example/filing", service.previewFilingUrl("custom", "X-1", "https://custom.example/filing"));
    }

    @Test
    public void previewPsbUrlUsesPsbType() {
        when(sitePortalSupport.resolveFilingUrl(any(ComplianceFilingItem.class)))
                .thenReturn("https://beian.mps.gov.cn/web/search/recordcode=51010802000202");
        service.previewPsbUrl("川公网安备51010802000202号");
        ArgumentCaptor<ComplianceFilingItem> captor = ArgumentCaptor.forClass(ComplianceFilingItem.class);
        verify(sitePortalSupport).resolveFilingUrl(captor.capture());
        assertEquals(ComplianceFilingType.psb.name(), captor.getValue().getType());
    }

    @Test
    public void saveConfigSyncsLoadingThemeAfterPersist() {
        SitePortalConfig config = new SitePortalConfig();
        when(sysConfigService.updateSitePortalConfig(config)).thenReturn(config);
        service.saveConfig(config);
        verify(sysConfigService).updateSitePortalConfig(config);
        verify(sitePortalSupport).syncLoadingTheme(config);
    }
}
