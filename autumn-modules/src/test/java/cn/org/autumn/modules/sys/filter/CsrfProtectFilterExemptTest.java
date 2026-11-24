package cn.org.autumn.modules.sys.filter;

import cn.org.autumn.modules.bot.service.RobotTokenService;
import cn.org.autumn.modules.oauth.service.ClientDetailsService;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.usr.service.UserTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CsrfProtectFilterExemptTest {

    private CsrfProtectFilter filter;
    private UserTokenService userTokenService;
    private RobotTokenService robotTokenService;
    private ClientDetailsService clientDetailsService;

    @BeforeEach
    void setUp() {
        filter = new CsrfProtectFilter();
        SysConfigService sysConfigService = mock(SysConfigService.class);
        userTokenService = mock(UserTokenService.class);
        robotTokenService = mock(RobotTokenService.class);
        clientDetailsService = mock(ClientDetailsService.class);
        ReflectionTestUtils.setField(filter, "sysConfigService", sysConfigService);
        ReflectionTestUtils.setField(filter, "userTokenService", userTokenService);
        ReflectionTestUtils.setField(filter, "robotTokenService", robotTokenService);
        ReflectionTestUtils.setField(filter, "clientDetailsService", clientDetailsService);
    }

    @Test
    void fakeLongTokenHeader_notExempt() {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/sys/user/update");
        req.addHeader("Token", "0123456789abcdef");
        when(clientDetailsService.isValidAccessToken("0123456789abcdef")).thenReturn(false);
        when(userTokenService.queryByToken("0123456789abcdef")).thenReturn(null);
        Boolean exempt = ReflectionTestUtils.invokeMethod(filter, "isExempt", req);
        assertFalse(Boolean.TRUE.equals(exempt));
    }

    @Test
    void pathExempt_login() {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/sys/login");
        Boolean exempt = ReflectionTestUtils.invokeMethod(filter, "isExempt", req);
        assertTrue(Boolean.TRUE.equals(exempt));
    }

    @Test
    void pathExempt_scanWebTicketCreate() {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/qrc/scanticket/web/ticket/create");
        Boolean exempt = ReflectionTestUtils.invokeMethod(filter, "isExempt", req);
        assertTrue(Boolean.TRUE.equals(exempt));
    }

    @Test
    void pathExempt_clientOauth2QrcWeb() {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/client/oauth2/qrc/web/ticket/create");
        Boolean exempt = ReflectionTestUtils.invokeMethod(filter, "isExempt", req);
        assertTrue(Boolean.TRUE.equals(exempt));
    }

    @Test
    void validOauthAccessToken_exempt() {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/sys/user/update");
        req.addHeader("Authorization", "Bearer real-access-token-xyz");
        when(clientDetailsService.isValidAccessToken("real-access-token-xyz")).thenReturn(true);
        Boolean exempt = ReflectionTestUtils.invokeMethod(filter, "isExempt", req);
        assertTrue(Boolean.TRUE.equals(exempt));
    }
}
