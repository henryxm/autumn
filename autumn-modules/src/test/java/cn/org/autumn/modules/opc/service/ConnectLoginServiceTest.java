package cn.org.autumn.modules.opc.service;

import cn.org.autumn.modules.opc.dto.ConnectBindResolveResult;
import cn.org.autumn.modules.opc.dto.ConnectOAuthFinishResult;
import cn.org.autumn.modules.opc.dto.OpcTokenResult;
import cn.org.autumn.modules.opc.dto.OpcUserInfoResult;
import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import cn.org.autumn.modules.opc.support.ConnectBindException;
import cn.org.autumn.modules.usr.dto.UserProfile;
import cn.org.autumn.modules.usr.service.UserProfileService;
import cn.org.autumn.opl.model.OpenUserInfoSnapshot;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class ConnectLoginServiceTest {

    @InjectMocks
    private ConnectLoginService connectLoginService;

    @Mock
    private ConnectOauthService connectOauthService;

    @Mock
    private ConnectBindService connectBindService;

    @Mock
    private ConnectBindPendingService connectBindPendingService;

    @Mock
    private ConnectAppService connectAppService;

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private HttpServletRequest request;

    private ConnectAppEntity app;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        app = new ConnectAppEntity();
        app.setUuid("ca1");
        app.setAppId("app_test");
    }

    @Test
    public void resolveBindEstablishesSession() {
        OpcTokenResult token = new OpcTokenResult();
        token.setAccessToken("at_1");
        OpenUserInfoSnapshot snapshot = new OpenUserInfoSnapshot();
        snapshot.setOpenId("oid1");
        UserProfile profile = new UserProfile();
        profile.setUuid("local-u1");
        ConnectBindResolveResult bindResult = ConnectBindResolveResult.of(profile, true);
        Mockito.when(connectOauthService.exchangeCode(app, "code1")).thenReturn(token);
        Mockito.when(connectOauthService.fetchUserInfoForBind(app, "at_1")).thenReturn(OpcUserInfoResult.of(snapshot, "local-u1"));
        Mockito.when(connectBindService.resolveAndBind(app, snapshot, "local-u1")).thenReturn(bindResult);

        connectLoginService.completeOAuthCallback(request, app, "code1", null);

        Mockito.verify(userProfileService).establishSession(profile);
    }

    @Test
    public void bindChoiceRequiredSavesPendingAndRethrowsWithToken() {
        OpcTokenResult token = new OpcTokenResult();
        token.setAccessToken("at_1");
        OpenUserInfoSnapshot snapshot = new OpenUserInfoSnapshot();
        snapshot.setOpenId("oid1");
        Mockito.when(connectOauthService.exchangeCode(app, "code1")).thenReturn(token);
        Mockito.when(connectOauthService.fetchUserInfoForBind(app, "at_1")).thenReturn(OpcUserInfoResult.of(snapshot, null));
        Mockito.when(connectBindService.resolveAndBind(app, snapshot, null)).thenThrow(ConnectBindException.bindChoiceRequired(app, "oid1"));
        Mockito.when(connectBindPendingService.save(app, snapshot, "at_1", "")).thenReturn("pending-token-1");

        try {
            connectLoginService.completeOAuthCallback(request, app, "code1", null);
            Assert.fail("expected ConnectBindException");
        } catch (ConnectBindException e) {
            Assert.assertEquals(ConnectBindException.ConflictType.BIND_CHOICE_REQUIRED, e.getConflictType());
            Assert.assertEquals("pending-token-1", e.getPendingToken());
        }
        Mockito.verify(userProfileService, Mockito.never()).establishSession(Mockito.any());
    }

    @Test
    public void finishOAuthLoginReturnsSuccessRedirect() {
        OpcTokenResult token = new OpcTokenResult();
        token.setAccessToken("at_1");
        OpenUserInfoSnapshot snapshot = new OpenUserInfoSnapshot();
        snapshot.setOpenId("oid1");
        UserProfile profile = new UserProfile();
        profile.setUuid("local-u1");
        ConnectBindResolveResult bindResult = ConnectBindResolveResult.of(profile, true);
        Mockito.when(connectOauthService.exchangeCode(app, "code1")).thenReturn(token);
        Mockito.when(connectOauthService.fetchUserInfoForBind(app, "at_1")).thenReturn(OpcUserInfoResult.of(snapshot, "local-u1"));
        Mockito.when(connectBindService.resolveAndBind(app, snapshot, "local-u1")).thenReturn(bindResult);

        ConnectOAuthFinishResult result = connectLoginService.finishOAuthLogin(request, app, "code1", "/home");

        Assert.assertFalse(result.isBindChoice());
        Assert.assertEquals("/home", result.getRedirectUrl());
    }

    @Test
    public void finishOAuthLoginReturnsBindChoiceWithoutThrowing() {
        OpcTokenResult token = new OpcTokenResult();
        token.setAccessToken("at_1");
        OpenUserInfoSnapshot snapshot = new OpenUserInfoSnapshot();
        snapshot.setOpenId("oid1");
        Mockito.when(connectOauthService.exchangeCode(app, "code1")).thenReturn(token);
        Mockito.when(connectOauthService.fetchUserInfoForBind(app, "at_1")).thenReturn(OpcUserInfoResult.of(snapshot, null));
        Mockito.when(connectBindService.resolveAndBind(app, snapshot, null)).thenThrow(ConnectBindException.bindChoiceRequired(app, "oid1"));
        Mockito.when(connectBindPendingService.save(app, snapshot, "at_1", "/home")).thenReturn("pending-token-1");

        ConnectOAuthFinishResult result = connectLoginService.finishOAuthLogin(request, app, "code1", "/home");

        Assert.assertTrue(result.isBindChoice());
        Assert.assertTrue(result.getRedirectUrl().contains("pending-token-1"));
        Mockito.verify(userProfileService, Mockito.never()).establishSession(Mockito.any());
    }
}
