package cn.org.autumn.modules.opc.service;

import cn.org.autumn.modules.opc.dto.ConnectBindResolveResult;
import cn.org.autumn.modules.opc.dto.OpcTokenResult;
import cn.org.autumn.modules.opc.dto.OpcUserInfoResult;
import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import cn.org.autumn.modules.usr.dto.UserProfile;
import cn.org.autumn.modules.usr.service.UserProfileService;
import cn.org.autumn.opl.model.OpenUserInfoSnapshot;
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
    private UserProfileService userProfileService;

    private ConnectAppEntity app;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        app = new ConnectAppEntity();
        app.setUuid("ca1");
        app.setAppId("app_test");
    }

    @Test
    public void idempotentBindStillEstablishesSession() {
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

        connectLoginService.completeOAuthCallback(app, "code1");

        Mockito.verify(userProfileService).establishSession(profile);
    }
}
