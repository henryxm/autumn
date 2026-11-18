package cn.org.autumn.modules.wall.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IpBlackServicePolicyIpTest {

    @Test
    void policyIp_isExactlyZeroZero() {
        assertTrue(IpBlackService.isPolicyIp("0.0.0.0"));
        assertTrue(IpBlackService.isPolicyIp(" 0.0.0.0 "));
        assertFalse(IpBlackService.isPolicyIp("0.0.0.1"));
        assertFalse(IpBlackService.isPolicyIp("127.0.0.1"));
        assertFalse(IpBlackService.isPolicyIp(null));
        assertFalse(IpBlackService.isPolicyIp(""));
    }

    @Test
    void minFirewallCount_floor() {
        assertEquals(500, IpBlackService.MIN_FIREWALL_COUNT);
        assertEquals("0.0.0.0", IpBlackService.POLICY_IP);
    }
}
