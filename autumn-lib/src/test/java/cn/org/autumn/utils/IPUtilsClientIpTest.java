package cn.org.autumn.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IPUtilsClientIpTest {

    @AfterEach
    void clearProps() {
        System.clearProperty(IPUtils.TRUSTED_PROXIES_PROP);
        System.clearProperty(IPUtils.TRUSTED_PROXIES_ENV);
    }

    @Test
    void getIp_ignoresXffWithoutTrustedProxies_publicRemote() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("198.51.100.10");
        req.addHeader("X-Forwarded-For", "203.0.113.1, 10.0.0.1");
        assertEquals("198.51.100.10", IPUtils.getIp(req));
    }

    @Test
    void getIp_privateRemote_takesRightmostXffHop() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("127.0.0.1");
        req.addHeader("X-Forwarded-For", "203.0.113.50");
        assertEquals("203.0.113.50", IPUtils.getIp(req));
    }

    @Test
    void getIp_privateRemote_multiHop_usesRightmost() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("10.0.0.2");
        // 边缘须覆盖为单跳；若误 append，取最右（直连代理所见客户端）
        req.addHeader("X-Forwarded-For", "198.51.100.1, 203.0.113.9");
        assertEquals("203.0.113.9", IPUtils.getIp(req));
    }

    @Test
    void getIp_stripsTrustedProxiesFromRight() {
        System.setProperty(IPUtils.TRUSTED_PROXIES_PROP, "10.0.0.0/8,127.0.0.1");
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("10.0.0.5");
        req.addHeader("X-Forwarded-For", "203.0.113.9, 10.0.0.5");
        assertEquals("203.0.113.9", IPUtils.getIp(req));
    }

    @Test
    void getIp_untrustedRemoteIgnoresXffEvenIfConfigured() {
        System.setProperty(IPUtils.TRUSTED_PROXIES_PROP, "10.0.0.0/8");
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("198.51.100.20");
        req.addHeader("X-Forwarded-For", "203.0.113.1");
        assertEquals("198.51.100.20", IPUtils.getIp(req));
    }

    @Test
    void normalizeAdminWhiteEntry_rejectsWideCidr() {
        assertNull(IPUtils.normalizeAdminWhiteEntry("0.0.0.0/1"));
        assertNull(IPUtils.normalizeAdminWhiteEntry("10.0.0.0/8"));
        assertEquals("203.0.113.10", IPUtils.normalizeAdminWhiteEntry("203.0.113.10"));
        assertEquals("10.0.0.0/24", IPUtils.normalizeAdminWhiteEntry("10.0.0.0/24"));
        assertTrue(IPUtils.isInRange("10.0.0.5", "10.0.0.0/24"));
        assertFalse(IPUtils.isInRange("10.0.1.5", "10.0.0.0/24"));
        assertFalse(IPUtils.isInRange("1.2.3.4", "0.0.0.0/0"));
    }
}
