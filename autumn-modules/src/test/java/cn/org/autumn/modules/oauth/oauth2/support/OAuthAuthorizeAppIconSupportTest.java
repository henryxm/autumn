package cn.org.autumn.modules.oauth.oauth2.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import cn.org.autumn.modules.oauth.entity.ClientDetailsEntity;
import cn.org.autumn.modules.oauth.service.ClientDetailsService;
import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import cn.org.autumn.modules.opc.service.ConnectAppService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class OAuthAuthorizeAppIconSupportTest {

    @Mock
    private ConnectAppService connectAppService;

    @Mock
    private ClientDetailsService clientDetailsService;

    @InjectMocks
    private OAuthAuthorizeAppIconSupport support;

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(support, "connectAppService", connectAppService);
        ReflectionTestUtils.setField(support, "clientDetailsService", clientDetailsService);
    }

    @Test
    public void resolveByAppIdPrefersConnectAppIcon() {
        ConnectAppEntity connectApp = new ConnectAppEntity();
        connectApp.setIcon(" https://example.com/opc.png ");
        when(connectAppService.getByAppId("app-1")).thenReturn(connectApp);
        assertEquals("https://example.com/opc.png", support.resolveByAppId("app-1"));
    }

    @Test
    public void resolveByAppIdFallsBackToClientDetails() {
        when(connectAppService.getByAppId("app-2")).thenReturn(null);
        ClientDetailsEntity client = new ClientDetailsEntity();
        client.setClientIconUri("https://example.com/oauth.png");
        when(clientDetailsService.findByClientId("app-2")).thenReturn(client);
        assertEquals("https://example.com/oauth.png", support.resolveByAppId("app-2"));
    }

    @Test
    public void resolveByClientPrefersClientIconUri() {
        ClientDetailsEntity client = new ClientDetailsEntity();
        client.setClientId("client-1");
        client.setClientIconUri("https://example.com/client.png");
        assertEquals("https://example.com/client.png", support.resolveByClient(client));
    }

    @Test
    public void resolveByClientFallsBackToConnectAppWhenClientIconBlank() {
        ClientDetailsEntity client = new ClientDetailsEntity();
        client.setClientId("client-2");
        client.setClientIconUri("");
        ConnectAppEntity connectApp = new ConnectAppEntity();
        connectApp.setIcon("https://example.com/connect.png");
        when(connectAppService.getByAppId("client-2")).thenReturn(connectApp);
        assertEquals("https://example.com/connect.png", support.resolveByClient(client));
    }

    @Test
    public void resolveByAppIdReturnsNullWhenBlank() {
        assertNull(support.resolveByAppId(" "));
        assertNull(support.resolveByAppId(null));
    }
}
