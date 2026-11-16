package cn.org.autumn.modules.wall.controller;

import cn.org.autumn.exception.AException;
import cn.org.autumn.modules.sys.support.AdminAccessGuard;
import cn.org.autumn.modules.wall.entity.IpAdminWhiteEntity;
import cn.org.autumn.modules.wall.service.IpAdminWhiteService;
import cn.org.autumn.modules.wall.service.IpWhiteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

class WallControllerIpAdminWhiteTest {

    private WallController controller;
    private AdminAccessGuard adminAccessGuard;
    private IpAdminWhiteService ipAdminWhiteService;
    private IpWhiteService ipWhiteService;

    @BeforeEach
    void setUp() {
        controller = new WallController();
        adminAccessGuard = mock(AdminAccessGuard.class);
        ipAdminWhiteService = mock(IpAdminWhiteService.class);
        ipWhiteService = mock(IpWhiteService.class);
        ReflectionTestUtils.setField(controller, "adminAccessGuard", adminAccessGuard);
        ReflectionTestUtils.setField(controller, "ipAdminWhiteService", ipAdminWhiteService);
        ReflectionTestUtils.setField(controller, "ipWhiteService", ipWhiteService);
    }

    @Test
    void bootstrap_requiresSystemAdminOnlyThenClaims() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.20");
        IpAdminWhiteEntity entity = new IpAdminWhiteEntity();
        entity.setIp("203.0.113.20");
        when(ipAdminWhiteService.bootstrapClaim(request, "sec")).thenReturn(entity);

        Map<String, Object> result = controller.bootstrapIpAdminWhite("sec", request);
        assertTrue(Boolean.TRUE.equals(result.get("success")));
        assertEquals("203.0.113.20", result.get("ip"));
        verify(adminAccessGuard).requireSystemAdmin("ipadminwhiteBootstrap");
        verify(adminAccessGuard, never()).requireOpsAdmin(any(), any(), anyString());
        verify(ipAdminWhiteService).bootstrapClaim(request, "sec");
        verify(adminAccessGuard).auditWithIp(eq(request), eq("admin_ip_bootstrap"), anyString());
    }

    @Test
    void addAdminWhite_requiresOpsGuard() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        doThrow(new AException("无权限", 403)).when(adminAccessGuard)
                .requireOpsAdmin(eq(request), eq(WallController.class), eq("ipadminwhiteAdd"));
        assertThrows(AException.class, () -> controller.addIpAdminWhite("1.2.3.4", request));
        verify(ipAdminWhiteService, never()).create(anyString(), anyString(), anyString());
        verify(ipWhiteService, never()).create(anyString(), anyString(), anyString());
    }

    @Test
    void userWhiteUnaffectedByAdminService() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(ipWhiteService.create("8.8.8.8", "手动添加", "手动添加")).thenReturn(new cn.org.autumn.modules.wall.entity.IpWhiteEntity());
        controller.addIpWhite("8.8.8.8", request);
        verify(ipWhiteService).create("8.8.8.8", "手动添加", "手动添加");
        verify(ipAdminWhiteService, never()).create(anyString(), anyString(), anyString());
    }
}
