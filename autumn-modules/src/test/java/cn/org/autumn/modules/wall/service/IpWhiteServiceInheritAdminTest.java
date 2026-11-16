package cn.org.autumn.modules.wall.service;

import cn.org.autumn.site.WallFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 用户白名单逻辑继承管理级白名单：管理 IP 视为 isWhite，无需写入 wall_ip_white。
 */
class IpWhiteServiceInheritAdminTest {

    private IpWhiteService ipWhiteService;
    private IpAdminWhiteService ipAdminWhiteService;
    private WallFactory wallFactory;

    @BeforeEach
    void setUp() {
        ipWhiteService = new IpWhiteService();
        ipAdminWhiteService = mock(IpAdminWhiteService.class);
        wallFactory = mock(WallFactory.class);
        ReflectionTestUtils.setField(ipWhiteService, "ipAdminWhiteService", ipAdminWhiteService);
        ReflectionTestUtils.setField(ipWhiteService, "wallFactory", wallFactory);
        ReflectionTestUtils.setField(ipWhiteService, "ipWhiteList", new HashSet<String>());
        ReflectionTestUtils.setField(ipWhiteService, "ipWhiteSectionList", new HashSet<String>());
        when(wallFactory.isIpWhiteEnable()).thenReturn(true);
    }

    @Test
    void isWhite_inheritsAdminWhitelist() {
        when(ipAdminWhiteService.isAdminWhite("203.0.113.50")).thenReturn(true);
        assertTrue(ipWhiteService.isWhite("203.0.113.50"));
        assertTrue(ipWhiteService.isWhite("203.0.113.50", "ua"));
    }

    @Test
    void isWhite_falseWhenNeitherList() {
        when(ipAdminWhiteService.isAdminWhite("198.51.100.9")).thenReturn(false);
        assertFalse(ipWhiteService.isWhite("198.51.100.9"));
        assertFalse(ipWhiteService.isWhite("198.51.100.9", "ua"));
    }

    @Test
    void isWhite_userListStillWorksWithoutAdmin() {
        when(ipAdminWhiteService.isAdminWhite("8.8.8.8")).thenReturn(false);
        HashSet<String> user = new HashSet<>();
        user.add("8.8.8.8");
        ReflectionTestUtils.setField(ipWhiteService, "ipWhiteList", user);
        assertTrue(ipWhiteService.isWhite("8.8.8.8"));
    }

    @Test
    void isWhite_oneArgIgnoresEnableSwitch_listMembershipOnly() {
        when(wallFactory.isIpWhiteEnable()).thenReturn(false);
        when(ipAdminWhiteService.isAdminWhite("198.51.100.9")).thenReturn(false);
        assertFalse(ipWhiteService.isWhite("198.51.100.9"));
    }
}
