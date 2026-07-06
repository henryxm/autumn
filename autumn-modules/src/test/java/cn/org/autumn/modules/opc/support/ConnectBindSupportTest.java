package cn.org.autumn.modules.opc.support;

import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import cn.org.autumn.opl.OplConstants;
import cn.org.autumn.opl.model.OpenAppSnapshot;
import cn.org.autumn.opl.spi.OpenPlatformService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

public class ConnectBindSupportTest {

    private ConnectBindSupport connectBindSupport;

    @Mock
    private OpenPlatformService openPlatformService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        connectBindSupport = new ConnectBindSupport();
        ReflectionTestUtils.setField(connectBindSupport, "openPlatformService", openPlatformService);
    }

    @Test
    public void samePlatformWhenAppRegisteredLocally() {
        ConnectAppEntity app = new ConnectAppEntity();
        app.setAppId("app_local");
        OpenAppSnapshot snapshot = new OpenAppSnapshot();
        snapshot.setStatus(OplConstants.STATUS_ACTIVE);
        Mockito.when(openPlatformService.getApp("app_local")).thenReturn(snapshot);

        Assert.assertTrue(connectBindSupport.isSamePlatform(app));
    }

    @Test
    public void notSamePlatformWhenAppMissingLocally() {
        ConnectAppEntity app = new ConnectAppEntity();
        app.setAppId("app_remote");
        Mockito.when(openPlatformService.getApp("app_remote")).thenReturn(null);

        Assert.assertFalse(connectBindSupport.isSamePlatform(app));
    }

    @Test
    public void notSamePlatformWhenOplServiceAbsent() {
        ConnectBindSupport support = new ConnectBindSupport();
        ConnectAppEntity app = new ConnectAppEntity();
        app.setAppId("app_local");

        Assert.assertFalse(support.isSamePlatform(app));
    }

    @Test
    public void isSameUserIgnoresCase() {
        Assert.assertTrue(connectBindSupport.isSameUser("abc-UUID", "ABC-uuid"));
        Assert.assertFalse(connectBindSupport.isSameUser("a", "b"));
    }
}
