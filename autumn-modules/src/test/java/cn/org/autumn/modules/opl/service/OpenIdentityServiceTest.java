package cn.org.autumn.modules.opl.service;

import cn.org.autumn.modules.opl.dao.OpenIdentityDao;
import cn.org.autumn.modules.opl.entity.OpenAppEntity;
import cn.org.autumn.modules.opl.entity.OpenIdentityEntity;
import cn.org.autumn.opl.model.OpenIdentitySnapshot;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

public class OpenIdentityServiceTest {

    @InjectMocks
    private OpenIdentityService openIdentityService;

    @Mock
    private OpenIdentityDao openIdentityDao;

    @Mock
    private OpenAppService openAppService;

    @Mock
    private OpenUnionService openUnionService;

    @Mock
    private OplExtensionService oplExtensionService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(openIdentityService, "baseMapper", openIdentityDao);
        Mockito.when(oplExtensionService.resolveOpenId(Mockito.any(), Mockito.anyString(), Mockito.anyString())).thenAnswer(invocation -> invocation.getArguments()[2]);
    }

    @Test
    public void sameAccountDifferentAppsShareUnionIdButDifferentOpenId() {
        OpenAppEntity app1 = activeApp("acc1", "app1");
        OpenAppEntity app2 = activeApp("acc1", "app2");
        Mockito.when(openAppService.requireActiveApp("app1")).thenReturn(app1);
        Mockito.when(openAppService.requireActiveApp("app2")).thenReturn(app2);
        Mockito.when(openIdentityDao.getByAppIdAndUser(Mockito.eq("app1"), Mockito.eq("user1"))).thenReturn(null);
        Mockito.when(openIdentityDao.getByAppIdAndUser(Mockito.eq("app2"), Mockito.eq("user1"))).thenReturn(null);
        Mockito.when(openUnionService.getOrCreate("acc1", "user1")).thenReturn("u_shared");

        OpenIdentitySnapshot first = openIdentityService.resolveOrCreate("app1", "user1");
        OpenIdentitySnapshot second = openIdentityService.resolveOrCreate("app2", "user1");

        Assert.assertNotEquals(first.getOpenId(), second.getOpenId());
        Assert.assertEquals("u_shared", first.getUnionId());
        Assert.assertEquals("u_shared", second.getUnionId());
        Mockito.verify(openIdentityDao, Mockito.times(2)).insert(Mockito.any(OpenIdentityEntity.class));
    }

    @Test
    public void identityEntityDoesNotStoreUnionId() {
        OpenAppEntity app = activeApp("acc1", "app1");
        Mockito.when(openAppService.requireActiveApp("app1")).thenReturn(app);
        Mockito.when(openIdentityDao.getByAppIdAndUser("app1", "user1")).thenReturn(null);
        Mockito.when(openUnionService.getOrCreate("acc1", "user1")).thenReturn("u_1");

        openIdentityService.resolveOrCreate("app1", "user1");

        Mockito.verify(openIdentityDao).insert(Mockito.argThat((OpenIdentityEntity entity) -> entity.getOpenId() != null && entity.getAppId().equals("app1")));
    }

    private OpenAppEntity activeApp(String account, String appId) {
        OpenAppEntity app = new OpenAppEntity();
        app.setAccount(account);
        app.setAppId(appId);
        app.setStatus(OpenAppEntity.STATUS_ACTIVE);
        return app;
    }
}
