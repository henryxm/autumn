package cn.org.autumn.modules.ops.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.org.autumn.site.SitePortalSupport;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;

@RunWith(MockitoJUnitRunner.class)
public class OpsDiscoverSupportTest {

    @Mock
    private OpsCertService opsCertService;

    @Mock
    private SitePortalSupport sitePortalSupport;

    @InjectMocks
    private OpsDiscoverSupport support;

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(support, "opsCertService", opsCertService);
        ReflectionTestUtils.setField(support, "sitePortalSupport", sitePortalSupport);
    }

    @Test
    public void publicView_returnsNullWhenDisabled() {
        when(opsCertService.isDiscoverEnabled()).thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        ExtendedModelMap model = new ExtendedModelMap();
        assertNull(support.resolvePublicDiscoverView(request, model));
        verify(sitePortalSupport, never()).applyDiscoverPublicModel(any(), any());
    }

    @Test
    public void publicView_returnsOpenWhenEnabled() {
        when(opsCertService.isDiscoverEnabled()).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        ExtendedModelMap model = new ExtendedModelMap();
        assertEquals(OpsDiscoverSupport.VIEW_OPEN, support.resolvePublicDiscoverView(request, model));
        verify(sitePortalSupport).applyDiscoverPublicModel(eq(request), eq(model));
        assertEquals(true, model.get("publicDiscoverEnabled"));
        assertEquals(false, model.get("adminDiscoverPreview"));
    }

    @Test
    public void adminView_alwaysOpen_andExposesPublicFlag() {
        when(opsCertService.isDiscoverEnabled()).thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        ExtendedModelMap model = new ExtendedModelMap();
        assertEquals(OpsDiscoverSupport.VIEW_OPEN, support.resolveAdminDiscoverView(request, model));
        verify(sitePortalSupport).applyDiscoverPublicModel(eq(request), eq(model));
        assertEquals(false, model.get("publicDiscoverEnabled"));
        assertEquals(true, model.get("adminDiscoverPreview"));
    }
}
