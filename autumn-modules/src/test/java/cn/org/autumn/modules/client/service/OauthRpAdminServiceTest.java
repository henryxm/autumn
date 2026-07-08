package cn.org.autumn.modules.client.service;

import cn.org.autumn.config.ClientType;
import cn.org.autumn.modules.client.dto.OauthRpClientView;
import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.oauth.entity.ClientDetailsEntity;
import cn.org.autumn.modules.oauth.service.ClientDetailsService;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.sys.service.SysUserService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

public class OauthRpAdminServiceTest {

    private OauthRpAdminService oauthRpAdminService;

    @Mock
    private WebAuthenticationService webAuthenticationService;

    @Mock
    private WebOauthBindService webOauthBindService;

    @Mock
    private ClientDetailsService clientDetailsService;

    @Mock
    private SysConfigService sysConfigService;

    @Mock
    private SysUserService sysUserService;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        oauthRpAdminService = new OauthRpAdminService();
        ReflectionTestUtils.setField(oauthRpAdminService, "webAuthenticationService", webAuthenticationService);
        ReflectionTestUtils.setField(oauthRpAdminService, "webOauthBindService", webOauthBindService);
        ReflectionTestUtils.setField(oauthRpAdminService, "clientDetailsService", clientDetailsService);
        ReflectionTestUtils.setField(oauthRpAdminService, "sysConfigService", sysConfigService);
        ReflectionTestUtils.setField(oauthRpAdminService, "sysUserService", sysUserService);
    }

    @Test
    public void quickCreate_syncsSecretFromUpstreamAs() {
        Mockito.when(sysConfigService.getBaseUrl()).thenReturn("https://auth.example.com");
        Mockito.when(webAuthenticationService.getByClientId("demo")).thenReturn(null);
        ClientDetailsEntity asClient = new ClientDetailsEntity();
        asClient.setClientSecret("as-secret");
        asClient.setRedirectUri("https://auth.example.com/client/oauth2/callback");
        Mockito.when(clientDetailsService.findByClientId("demo")).thenReturn(asClient);
        WebAuthenticationEntity created = new WebAuthenticationEntity();
        created.setUuid("web-uuid");
        created.setClientId("demo");
        created.setClientSecret("as-secret");
        created.setName("demo");
        created.setRedirectUri("https://auth.example.com/client/oauth2/callback");
        created.setAuthorizeUri("https://auth.example.com/oauth2/authorize");
        created.setAccessTokenUri("https://auth.example.com/oauth2/token");
        created.setUserInfoUri("https://auth.example.com/oauth2/userInfo");
        Mockito.when(webAuthenticationService.create(
                Mockito.eq("https://auth.example.com"),
                Mockito.eq("demo"),
                Mockito.eq("as-secret"),
                Mockito.eq(ClientType.ManualCreate),
                Mockito.eq("demo"),
                Mockito.eq("basic"),
                Mockito.eq("normal")
        )).thenReturn(created);
        Mockito.when(webOauthBindService.selectCount(Mockito.any())).thenReturn(0);

        OauthRpClientView view = oauthRpAdminService.quickCreate("demo");

        Assert.assertEquals("demo", view.getClientId());
        Assert.assertTrue(view.isSameInstance());
        Assert.assertEquals("oauth2:demo", view.getLoginAuthentication());
    }
}
