package cn.org.autumn.modules.opc.service;

import cn.org.autumn.modules.opc.dto.ConnectBindPendingContext;
import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import cn.org.autumn.opl.model.OpenUserInfoSnapshot;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ConnectBindPendingServiceTest {

    private ConnectBindPendingService connectBindPendingService;

    @Before
    public void setUp() {
        connectBindPendingService = new ConnectBindPendingService();
    }

    @Test
    public void savePeekConsumeRoundTrip() {
        ConnectAppEntity app = new ConnectAppEntity();
        app.setUuid("ca_uuid");
        app.setAppId("app_demo");
        OpenUserInfoSnapshot userInfo = new OpenUserInfoSnapshot();
        userInfo.setOpenId("oid_1");
        userInfo.setNickname("nick");

        String token = connectBindPendingService.save(app, userInfo, "access-token", "/success");

        ConnectBindPendingContext peeked = connectBindPendingService.peek(token);
        Assert.assertNotNull(peeked);
        Assert.assertEquals("ca_uuid", peeked.getConnectAppUuid());
        Assert.assertEquals("app_demo", peeked.getAppId());
        Assert.assertEquals("/success", peeked.getCallback());

        ConnectBindPendingContext consumed = connectBindPendingService.consume(token);
        Assert.assertNotNull(consumed);
        Assert.assertNull(connectBindPendingService.peek(token));
    }
}
