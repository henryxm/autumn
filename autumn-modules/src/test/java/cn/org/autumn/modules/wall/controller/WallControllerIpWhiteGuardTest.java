package cn.org.autumn.modules.wall.controller;

import cn.org.autumn.exception.AException;
import cn.org.autumn.modules.sys.support.AdminAccessGuard;
import cn.org.autumn.modules.wall.entity.IpWhiteEntity;
import cn.org.autumn.modules.wall.service.IpWhiteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 白名单加白必须先过超管 + IP 白名单，未鉴权不得写库。
 */
class WallControllerIpWhiteGuardTest {

    private WallController controller;
    private AdminAccessGuard adminAccessGuard;
    private IpWhiteService ipWhiteService;

    @BeforeEach
    void setUp() {
        controller = new WallController();
        adminAccessGuard = mock(AdminAccessGuard.class);
        ipWhiteService = mock(IpWhiteService.class);
        ReflectionTestUtils.setField(controller, "adminAccessGuard", adminAccessGuard);
        ReflectionTestUtils.setField(controller, "ipWhiteService", ipWhiteService);
    }

    @Test
    void addIpWhite_rejectsWhenOpsGuardFails() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        doThrow(new AException("无权限", 403)).when(adminAccessGuard)
                .requireOpsAdmin(eq(request), eq(WallController.class), eq("ipwhiteAdd"));
        assertThrows(AException.class, () -> controller.addIpWhite("1.2.3.4", request));
        verify(ipWhiteService, never()).create(anyString(), anyString(), anyString());
    }

    @Test
    void addIpWhite_writesWhenOpsGuardPasses() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(ipWhiteService.create("8.8.8.8", "手动添加", "手动添加")).thenReturn(new IpWhiteEntity());
        var result = controller.addIpWhite("8.8.8.8", request);
        assertTrue(Boolean.TRUE.equals(result.get("success")));
        verify(adminAccessGuard).requireOpsAdmin(eq(request), eq(WallController.class), eq("ipwhiteAdd"));
        verify(ipWhiteService).create("8.8.8.8", "手动添加", "手动添加");
        verify(adminAccessGuard).auditWithIp(eq(request), eq("wall.ipwhite.add"), anyString());
    }
}
