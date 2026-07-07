package cn.org.autumn.modules.client.service;

import cn.org.autumn.model.AuthSiteConfig;
import cn.org.autumn.modules.sys.service.SysConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthSiteRoleServiceTest {

    @Mock
    SysConfigService sysConfigService;

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
}
