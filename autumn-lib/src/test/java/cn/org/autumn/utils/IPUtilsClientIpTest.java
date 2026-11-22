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
    void getIp_default_usesXffLeftmost_likeHistory() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        // 阿里云 SLB：remote 常为 100.125.x，真实用户在 XFF 最左
        req.setRemoteAddr("100.125.12.34");
        req.addHeader("X-Forwarded-For", "203.0.113.77, 100.125.9.8");
        assertEquals("203.0.113.77", IPUtils.getIp(req));
    }

    @Test
    void getIp_default_fallsBackToXRealIpThenRemote() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("198.51.100.10");
        req.addHeader("X-Real-IP", "203.0.113.50");
        assertEquals("203.0.113.50", IPUtils.getIp(req));
    }

    @Test
    void getIp_default_multiHop_trimsLeftmost() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("10.0.0.2");
        req.addHeader("X-Forwarded-For", " 203.0.113.9 , 10.0.0.5");
        assertEquals("203.0.113.9", IPUtils.getIp(req));
    }

    @Test
    void getIp_stripsTrustedProxiesFromRight_whenConfigured() {
        System.setProperty(IPUtils.TRUSTED_PROXIES_PROP, "10.0.0.0/8,127.0.0.1,100.64.0.0/10");
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("10.0.0.5");
        req.addHeader("X-Forwarded-For", "203.0.113.9, 10.0.0.5");
        assertEquals("203.0.113.9", IPUtils.getIp(req));
    }

    @Test
    void getIp_untrustedRemoteIgnoresXff_whenTrustedConfigured() {
        System.setProperty(IPUtils.TRUSTED_PROXIES_PROP, "10.0.0.0/8");
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("198.51.100.20");
        req.addHeader("X-Forwarded-For", "203.0.113.1");
        assertEquals("198.51.100.20", IPUtils.getIp(req));
    }

    @Test
    void classicClientIp_matchesHistoricalAliyunCase() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("100.125.1.2");
        req.addHeader("X-Forwarded-For", "198.51.100.7, 100.125.1.2");
        assertEquals("198.51.100.7", IPUtils.classicClientIp(req));
        assertTrue(IPUtils.isCarrierGradeNatIp("100.125.1.2"));
        assertTrue(IPUtils.isInfrastructureHop("100.125.1.2"));
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
