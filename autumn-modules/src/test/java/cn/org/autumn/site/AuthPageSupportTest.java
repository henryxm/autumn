package cn.org.autumn.site;

import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.client.service.WebAuthenticationService;
import cn.org.autumn.modules.sys.service.SysConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthPageSupportTest {

    @Mock
    private SysConfigService sysConfigService;

    @Mock
    private WebAuthenticationService webAuthenticationService;

    @InjectMocks
    private AuthPageSupport authPageSupport;

    @Test
    void prepareOauthLoginEntry_resolvesClientFromService() {
        WebAuthenticationEntity entity = new WebAuthenticationEntity();
        entity.setClientId("demo-client");
        entity.setName("Demo");
        entity.setRedirectUri("http://localhost/client/oauth2/callback");
        when(webAuthenticationService.getByClientId("demo-client")).thenReturn(entity);
        when(sysConfigService.getLoadingBrand()).thenReturn("Autumn");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/login");
        request.setContextPath("");
        Model model = new ExtendedModelMap();

        authPageSupport.prepareOauthLoginEntry(request, model, "demo-client");

        assertEquals("demo-client", model.getAttribute("clientId"));
        assertEquals(AuthPageAttributes.FLOW_OAUTH_LOGIN_ENTRY, model.getAttribute(AuthPageAttributes.ATTR_AUTH_FLOW_KIND));
    }

    @Test
    void prepareOplAuthorize_setsOplLoginAction() {
        when(sysConfigService.getLoadingBrand()).thenReturn("Autumn");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/open/oauth2/authorize");
        Model model = new ExtendedModelMap();

        authPageSupport.prepareOplAuthorize(request, model);

        assertEquals("/open/oauth2/opl/login", model.getAttribute("authorizeLoginAction"));
    }
}
