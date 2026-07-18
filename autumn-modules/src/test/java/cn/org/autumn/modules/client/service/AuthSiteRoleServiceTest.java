package cn.org.autumn.modules.client.service;

import cn.org.autumn.model.AuthSiteConfig;
import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.sys.service.SysConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthSiteRoleServiceTest {

    @Mock
    SysConfigService sysConfigService;

    @Mock
    WebAuthenticationService webAuthenticationService;

    @InjectMocks
    AuthSiteRoleService authSiteRoleService;

    @Test
    void resolveQrcWebMode_rpOnlyReturnsRp() {
        AuthSiteConfig config = new AuthSiteConfig();
        config.setSiteRole(AuthSiteConfig.ROLE_RP_ONLY);
        when(sysConfigService.getAuthSiteConfig()).thenReturn(config);
        assertEquals("rp", authSiteRoleService.resolveQrcWebMode());
        assertTrue(authSiteRoleService.isRpEnabled());
    }

    @Test
    void resolveQrcWebMode_asOnlyReturnsAs() {
        AuthSiteConfig config = new AuthSiteConfig();
        config.setSiteRole(AuthSiteConfig.ROLE_AS_ONLY);
        config.setQrcWebMode("auto");
        when(sysConfigService.getAuthSiteConfig()).thenReturn(config);
        assertEquals("as", authSiteRoleService.resolveQrcWebMode());
        assertFalse(authSiteRoleService.isRpEnabled());
    }

    @Test
    void resolveRpClientId_prefersRequestParamOverLoginAuthentication() {
        AuthSiteConfig config = new AuthSiteConfig();
        config.setSiteRole(AuthSiteConfig.ROLE_AS_AND_RP);
        when(sysConfigService.getAuthSiteConfig()).thenReturn(config);
        when(sysConfigService.getOauth2LoginClientId()).thenReturn("default-client");

        WebAuthenticationEntity entity = new WebAuthenticationEntity();
        entity.setClientId("param-client");
        when(webAuthenticationService.getByClientId("param-client")).thenReturn(entity);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/login");
        request.setParameter("client_id", "param-client");

        assertEquals("param-client", authSiteRoleService.resolveRpClientId(request));
        assertEquals("param-client", authSiteRoleService.resolveRpClient(request).getClientId());
    }

    @Test
    void resolveExplicitRpClientId_ignoresLoginAuthentication() {
        AuthSiteConfig config = new AuthSiteConfig();
        config.setSiteRole(AuthSiteConfig.ROLE_AS_AND_RP);
        when(sysConfigService.getAuthSiteConfig()).thenReturn(config);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/login");
        assertNull(authSiteRoleService.resolveExplicitRpClientId(request));

        request.setParameter("client_id", "param-client");
        assertEquals("param-client", authSiteRoleService.resolveExplicitRpClientId(request));
    }
}
