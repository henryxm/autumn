package cn.org.autumn.modules.oauth.service;

import cn.org.autumn.config.ClientType;
import cn.org.autumn.modules.auth.support.AuthScopeSupport;
import cn.org.autumn.modules.oauth.dto.OauthAsCreateOutcome;
import cn.org.autumn.modules.oauth.entity.ClientDetailsEntity;
import cn.org.autumn.modules.qrc.service.ClientGrantService;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.sys.service.SysUserService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

public class OauthAsAdminServiceTest {

    private OauthAsAdminService oauthAsAdminService;

    @Mock
    private ClientDetailsService clientDetailsService;

    @Mock
    private TokenStoreService tokenStoreService;

    @Mock
    private SysConfigService sysConfigService;

    @Mock
    private SysUserService sysUserService;

    @Mock
    private ClientGrantService clientGrantService;

    @Mock
    private AuthScopeSupport authScopeSupport;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        oauthAsAdminService = new OauthAsAdminService();
        ReflectionTestUtils.setField(oauthAsAdminService, "clientDetailsService", clientDetailsService);
        ReflectionTestUtils.setField(oauthAsAdminService, "tokenStoreService", tokenStoreService);
        ReflectionTestUtils.setField(oauthAsAdminService, "sysConfigService", sysConfigService);
        ReflectionTestUtils.setField(oauthAsAdminService, "sysUserService", sysUserService);
        ReflectionTestUtils.setField(oauthAsAdminService, "clientGrantService", clientGrantService);
        ReflectionTestUtils.setField(oauthAsAdminService, "authScopeSupport", authScopeSupport);
    }

    @Test
    public void createClient_generatesSecretAndDefaultRedirect() {
        Mockito.when(clientDetailsService.findByClientId("demo")).thenReturn(null);
        Mockito.when(sysConfigService.getBaseUrl()).thenReturn("https://auth.example.com");
        ClientDetailsEntity created = new ClientDetailsEntity();
        created.setClientId("demo");
        created.setClientSecret("secret-x");
        created.setClientName("demo");
        created.setScope("basic");
        created.setRedirectUri("https://auth.example.com/client/oauth2/callback");
        ArgumentCaptor<String> secretCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.when(clientDetailsService.create(
                Mockito.eq("https://auth.example.com"),
                Mockito.eq("demo"),
                secretCaptor.capture(),
                Mockito.eq(ClientType.ManualCreate),
                Mockito.eq("demo"),
                Mockito.eq("demo")
        )).thenReturn(created);
        Mockito.doAnswer(invocation -> {
            ClientDetailsEntity entity = invocation.getArgument(0);
            entity.setTrusted(1);
            entity.setArchived(0);
            return true;
        }).when(clientDetailsService).updateAllColumnById(Mockito.any(ClientDetailsEntity.class));

        OauthAsCreateOutcome outcome = oauthAsAdminService.createClient("demo", null, null, null, Boolean.TRUE);

        Assert.assertEquals("demo", outcome.getClientId());
        Assert.assertEquals("secret-x", outcome.getClientSecret());
        Assert.assertEquals("https://auth.example.com/oauth2/token", outcome.getTokenUrl());
        Assert.assertNotNull(secretCaptor.getValue());
        Mockito.verify(clientGrantService).saveQuick("demo", true);
    }

    @Test
    public void updateClient_persistsQuickGrant() {
        ClientDetailsEntity entity = new ClientDetailsEntity();
        entity.setClientId("demo");
        entity.setClientName("Demo");
        entity.setRedirectUri("https://rp.example.com/cb");
        entity.setScope("basic");
        entity.setTrusted(1);
        entity.setArchived(0);
        Mockito.when(clientDetailsService.findByClientId("demo")).thenReturn(entity);
        Mockito.when(clientDetailsService.updateAllColumnById(Mockito.any(ClientDetailsEntity.class))).thenReturn(1);

        oauthAsAdminService.updateClient("demo", "Demo2", "https://rp.example.com/cb", "basic", 1, 0, Boolean.FALSE);

        Mockito.verify(clientGrantService).saveQuick("demo", false);
    }
}
