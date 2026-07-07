package cn.org.autumn.modules.opc.service;

import cn.org.autumn.modules.opc.dao.ConnectBindDao;
import cn.org.autumn.modules.opc.dto.ConnectBindResolveResult;
import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import cn.org.autumn.modules.opc.entity.ConnectBindEntity;
import cn.org.autumn.modules.opc.support.ConnectBindException;
import cn.org.autumn.modules.opc.support.ConnectBindSupport;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.modules.usr.entity.UserProfileEntity;
import cn.org.autumn.modules.usr.service.UserProfileService;
import cn.org.autumn.opl.model.OpenUserInfoSnapshot;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

public class ConnectBindServiceTest {

    private static final String CONNECT_APP_UUID = "ca_test";
    private static final String OPEN_ID = "oid_user_a";
    private static final String UNION_ID = "uid_shared";
    private static final String LOCAL_A = "local-user-a";
    private static final String LOCAL_B = "local-user-b";
    private static final String LOCAL_NEW = "local-user-new";

    private ConnectBindService connectBindService;

    @Mock
    private ConnectBindDao connectBindDao;

    @Mock
    private SysUserService sysUserService;

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private ConnectBindSupport connectBindSupport;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        connectBindService = new ConnectBindService();
        ReflectionTestUtils.setField(connectBindService, "baseMapper", connectBindDao);
        ReflectionTestUtils.setField(connectBindService, "sysUserService", sysUserService);
        ReflectionTestUtils.setField(connectBindService, "userProfileService", userProfileService);
        ReflectionTestUtils.setField(connectBindService, "connectBindSupport", connectBindSupport);
        connectBindService = Mockito.spy(connectBindService);
        Mockito.when(connectBindSupport.isSameUser(Mockito.anyString(), Mockito.anyString())).thenAnswer(invocation -> {
            String left = (String) invocation.getArguments()[0];
            String right = (String) invocation.getArguments()[1];
            return left != null && left.equalsIgnoreCase(right);
        });
        Mockito.when(connectBindSupport.isSameOpenId(Mockito.anyString(), Mockito.anyString())).thenAnswer(invocation -> {
            String left = (String) invocation.getArguments()[0];
            String right = (String) invocation.getArguments()[1];
            return left != null && left.equals(right);
        });
    }

    @Test
    public void existingOpenIdBindIsIdempotent() {
        ConnectAppEntity app = app();
        OpenUserInfoSnapshot userInfo = userInfo(OPEN_ID, UNION_ID);
        ConnectBindEntity existing = bind(OPEN_ID, LOCAL_A);
        Mockito.when(connectBindDao.getByConnectAppAndOpenId(CONNECT_APP_UUID, OPEN_ID)).thenReturn(existing);
        mockProfile(LOCAL_A);

        ConnectBindResolveResult result = connectBindService.resolveAndBind(app, userInfo, LOCAL_A);

        Assert.assertTrue(result.isIdempotent());
        Assert.assertEquals(LOCAL_A, result.getProfile().getUuid());
        Mockito.verify(connectBindDao, Mockito.never()).insert(Mockito.any(ConnectBindEntity.class));
    }

    @Test
    public void samePlatformFirstBindUsesPlatformUserWithoutAutoRegister() {
        ConnectAppEntity app = app();
        OpenUserInfoSnapshot userInfo = userInfo(OPEN_ID, UNION_ID);
        Mockito.when(connectBindDao.getByConnectAppAndOpenId(CONNECT_APP_UUID, OPEN_ID)).thenReturn(null);
        Mockito.when(connectBindDao.getByConnectAppAndUnionId(CONNECT_APP_UUID, UNION_ID)).thenReturn(null);
        Mockito.when(connectBindDao.getByConnectAppAndUser(CONNECT_APP_UUID, LOCAL_A)).thenReturn(null);
        Mockito.when(sysUserService.getByUuid(LOCAL_A)).thenReturn(user(LOCAL_A));
        mockProfile(LOCAL_A);

        ConnectBindResolveResult result = connectBindService.resolveAndBind(app, userInfo, LOCAL_A);

        Assert.assertFalse(result.isIdempotent());
        Assert.assertEquals(LOCAL_A, result.getProfile().getUuid());
        Mockito.verify(connectBindDao).insert(Mockito.argThat(bind -> OPEN_ID.equals(bind.getOpenId()) && LOCAL_A.equals(bind.getUser())));
        Mockito.verify(sysUserService, Mockito.never()).provisionConnectUser(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void platformUserMismatchExistingBindThrowsConflict() {
        ConnectAppEntity app = app();
        OpenUserInfoSnapshot userInfo = userInfo(OPEN_ID, UNION_ID);
        ConnectBindEntity existing = bind(OPEN_ID, LOCAL_A);
        Mockito.when(connectBindDao.getByConnectAppAndOpenId(CONNECT_APP_UUID, OPEN_ID)).thenReturn(existing);

        try {
            connectBindService.resolveAndBind(app, userInfo, LOCAL_B);
            Assert.fail("expected ConnectBindException");
        } catch (ConnectBindException e) {
            Assert.assertEquals(ConnectBindException.ConflictType.UPSTREAM_BOUND_TO_OTHER, e.getConflictType());
        }
    }

    @Test
    public void localUserAlreadyBoundOtherOpenIdThrowsConflict() {
        ConnectAppEntity app = app();
        OpenUserInfoSnapshot userInfo = userInfo(OPEN_ID, UNION_ID);
        ConnectBindEntity localBind = bind("other-open-id", LOCAL_A);
        Mockito.when(connectBindDao.getByConnectAppAndOpenId(CONNECT_APP_UUID, OPEN_ID)).thenReturn(null);
        Mockito.when(connectBindDao.getByConnectAppAndUnionId(CONNECT_APP_UUID, UNION_ID)).thenReturn(null);
        Mockito.when(sysUserService.getByUuid(LOCAL_A)).thenReturn(user(LOCAL_A));
        Mockito.when(connectBindDao.getByConnectAppAndUser(CONNECT_APP_UUID, LOCAL_A)).thenReturn(localBind);

        try {
            connectBindService.resolveAndBind(app, userInfo, LOCAL_A);
            Assert.fail("expected ConnectBindException");
        } catch (ConnectBindException e) {
            Assert.assertEquals(ConnectBindException.ConflictType.LOCAL_ALREADY_BOUND, e.getConflictType());
        }
    }

    @Test
    public void crossPlatformWithoutPlatformUserRequiresBindChoice() {
        ConnectAppEntity app = app();
        OpenUserInfoSnapshot userInfo = userInfo(OPEN_ID, UNION_ID);
        Mockito.doReturn(null).when(connectBindService).sessionUserUuid();
        Mockito.when(connectBindDao.getByConnectAppAndOpenId(CONNECT_APP_UUID, OPEN_ID)).thenReturn(null);
        Mockito.when(connectBindDao.getByConnectAppAndUnionId(CONNECT_APP_UUID, UNION_ID)).thenReturn(null);

        try {
            connectBindService.resolveAndBind(app, userInfo, null);
            Assert.fail("expected ConnectBindException");
        } catch (ConnectBindException e) {
            Assert.assertEquals(ConnectBindException.ConflictType.BIND_CHOICE_REQUIRED, e.getConflictType());
        }
    }

    @Test
    public void loggedInSessionUserBindsWithoutChoice() {
        ConnectAppEntity app = app();
        OpenUserInfoSnapshot userInfo = userInfo(OPEN_ID, UNION_ID);
        Mockito.doReturn(LOCAL_B).when(connectBindService).sessionUserUuid();
        Mockito.when(connectBindDao.getByConnectAppAndOpenId(CONNECT_APP_UUID, OPEN_ID)).thenReturn(null);
        Mockito.when(connectBindDao.getByConnectAppAndUnionId(CONNECT_APP_UUID, UNION_ID)).thenReturn(null);
        Mockito.when(connectBindDao.getByConnectAppAndUser(CONNECT_APP_UUID, LOCAL_B)).thenReturn(null);
        mockProfile(LOCAL_B);

        ConnectBindResolveResult result = connectBindService.resolveAndBind(app, userInfo, null);

        Assert.assertEquals(LOCAL_B, result.getProfile().getUuid());
        Mockito.verify(connectBindDao).insert(Mockito.argThat(bind -> OPEN_ID.equals(bind.getOpenId()) && LOCAL_B.equals(bind.getUser())));
    }

    @Test
    public void bindCreateNewUser_insertsBind() {
        ConnectAppEntity app = app();
        OpenUserInfoSnapshot userInfo = userInfo(OPEN_ID, UNION_ID);
        Mockito.when(connectBindDao.getByConnectAppAndOpenId(CONNECT_APP_UUID, OPEN_ID)).thenReturn(null);
        SysUserEntity created = user(LOCAL_NEW);
        Mockito.when(sysUserService.provisionConnectUser(Mockito.anyString(), Mockito.anyString())).thenReturn(created);
        mockProfile(LOCAL_NEW);

        ConnectBindResolveResult result = connectBindService.bindCreateNewUser(app, userInfo);

        Assert.assertEquals(LOCAL_NEW, result.getProfile().getUuid());
        Mockito.verify(connectBindDao).insert(Mockito.argThat(bind -> OPEN_ID.equals(bind.getOpenId()) && LOCAL_NEW.equals(bind.getUser())));
    }

    private ConnectAppEntity app() {
        ConnectAppEntity app = new ConnectAppEntity();
        app.setUuid(CONNECT_APP_UUID);
        app.setAppId("app_test");
        return app;
    }

    private OpenUserInfoSnapshot userInfo(String openId, String unionId) {
        OpenUserInfoSnapshot snapshot = new OpenUserInfoSnapshot();
        snapshot.setOpenId(openId);
        snapshot.setUnionId(unionId);
        snapshot.setNickname("nick");
        return snapshot;
    }

    private ConnectBindEntity bind(String openId, String localUser) {
        ConnectBindEntity bind = new ConnectBindEntity();
        bind.setConnectApp(CONNECT_APP_UUID);
        bind.setOpenId(openId);
        bind.setUser(localUser);
        return bind;
    }

    private SysUserEntity user(String uuid) {
        SysUserEntity user = new SysUserEntity();
        user.setUuid(uuid);
        return user;
    }

    private void mockProfile(String uuid) {
        UserProfileEntity profile = new UserProfileEntity();
        profile.setUuid(uuid);
        profile.setNickname("nick");
        Mockito.when(userProfileService.getByUuid(uuid)).thenReturn(profile);
    }
}
