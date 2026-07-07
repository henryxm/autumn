package cn.org.autumn.modules.client.service;

import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.client.oauth2.WebOauthEndpointResolver;
import cn.org.autumn.modules.oauth.oauth2.support.OAuth2HttpClient;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OauthRpLoginServiceTest {

    @Mock
    AuthSiteRoleService authSiteRoleService;

    @Mock
    OauthRpStateService oauthRpStateService;

    @Mock
    OAuth2HttpClient oauth2HttpClient;

    @Mock
    WebOauthLoginService webOauthLoginService;

    @Mock
    WebOauthEndpointResolver webOauthEndpointResolver;

    @InjectMocks
    OauthRpLoginService oauthRpLoginService;

    @Test
    void handleCallback_rejectsInvalidState() {
        HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
        when(oauthRpStateService.consumeState("bad-state")).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> oauthRpLoginService.handleCallback(request, "auth-code", "bad-state", null, null));
    }

    @Test
    void handleCallback_rejectsMissingCode() {
        HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
        assertThrows(IllegalArgumentException.class, () -> oauthRpLoginService.handleCallback(request, "", "state", null, null));
    }
}
