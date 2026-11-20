package cn.org.autumn.modules.wall.controller;

import cn.org.autumn.exception.AException;
import cn.org.autumn.model.SecurityOpsConfig;
import cn.org.autumn.modules.sys.service.SysConfigService;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import cn.org.autumn.modules.wall.entity.IpWhiteEntity;

class WallControllerIpAdminWhiteTest {

    private WallController controller;
    private AdminAccessGuard adminAccessGuard;
    private IpAdminWhiteService ipAdminWhiteService;
    private IpWhiteService ipWhiteService;
    private SysConfigService sysConfigService;

    @BeforeEach
    void setUp() {
        controller = new WallController();
        adminAccessGuard = mock(AdminAccessGuard.class);
        ipAdminWhiteService = mock(IpAdminWhiteService.class);
        ipWhiteService = mock(IpWhiteService.class);
        sysConfigService = mock(SysConfigService.class);
        ReflectionTestUtils.setField(controller, "adminAccessGuard", adminAccessGuard);
        ReflectionTestUtils.setField(controller, "ipAdminWhiteService", ipAdminWhiteService);
        ReflectionTestUtils.setField(controller, "ipWhiteService", ipWhiteService);
        ReflectionTestUtils.setField(controller, "sysConfigService", sysConfigService);
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
        when(ipWhiteService.create("8.8.8.8", "手动添加", "手动添加")).thenReturn(new IpWhiteEntity());
        controller.addIpWhite("8.8.8.8", request);
        verify(ipWhiteService).create("8.8.8.8", "手动添加", "手动添加");
        verify(ipAdminWhiteService, never()).create(anyString(), anyString(), anyString());
    }

    @Test
    void unlock_withClearToken_claimsCurrentIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.9");
        when(ipAdminWhiteService.isBootstrapPending()).thenReturn(false);
        when(ipAdminWhiteService.isEmpty()).thenReturn(false);
        when(ipAdminWhiteService.isAdminWhite("198.51.100.9")).thenReturn(false);
        SecurityOpsConfig ops = new SecurityOpsConfig();
        ops.setClearToken("temp-ops-secret");
        when(sysConfigService.getSecurityOpsConfig()).thenReturn(ops);
        IpAdminWhiteEntity entity = new IpAdminWhiteEntity();
        entity.setIp("198.51.100.9");
        when(ipAdminWhiteService.create(eq("198.51.100.9"), eq("unlock"), anyString())).thenReturn(entity);

        Map<String, Object> result = controller.unlockIpAdminWhite("temp-ops-secret", request);
        assertTrue(Boolean.TRUE.equals(result.get("success")));
        assertEquals("clearToken", result.get("mode"));
        verify(adminAccessGuard).requireSystemAdmin("ipadminwhiteUnlock");
        verify(ipAdminWhiteService).create(eq("198.51.100.9"), eq("unlock"), anyString());
    }

    @Test
    void unlock_wrongSecret_rejected() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.9");
        when(ipAdminWhiteService.isBootstrapPending()).thenReturn(false);
        when(ipAdminWhiteService.isEmpty()).thenReturn(false);
        when(ipAdminWhiteService.isAdminWhite(anyString())).thenReturn(false);
        SecurityOpsConfig ops = new SecurityOpsConfig();
        ops.setClearToken("good");
        when(sysConfigService.getSecurityOpsConfig()).thenReturn(ops);
        assertThrows(AException.class, () -> controller.unlockIpAdminWhite("bad", request));
        verify(ipAdminWhiteService, never()).create(anyString(), anyString(), anyString());
    }

    @Test
    void gate_reportsLockedForNonAdminIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.9");
        when(ipAdminWhiteService.isAdminIpEnable()).thenReturn(true);
        when(ipAdminWhiteService.isBootstrapPending()).thenReturn(false);
        doThrow(new AException("服务繁忙")).when(ipAdminWhiteService)
                .checkAdmin(eq(request), eq(WallController.class), eq("ipadminwhiteGate"));
        Map<String, Object> gate = controller.getIpAdminWhiteGate(request);
        assertTrue(Boolean.TRUE.equals(gate.get("locked")));
        assertFalse(Boolean.TRUE.equals(gate.get("bootstrapPending")));
        assertEquals("clearToken", gate.get("mode"));
        verify(adminAccessGuard).requireSystemAdmin("ipadminwhiteGate");
    }

    @Test
    void gate_unlockedWhenAdminIpDisabled() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(ipAdminWhiteService.isAdminIpEnable()).thenReturn(false);
        when(ipAdminWhiteService.isBootstrapPending()).thenReturn(true);
        Map<String, Object> gate = controller.getIpAdminWhiteGate(request);
        assertFalse(Boolean.TRUE.equals(gate.get("locked")));
        assertFalse(Boolean.TRUE.equals(gate.get("bootstrapPending")));
        assertFalse(Boolean.TRUE.equals(gate.get("adminIpEnable")));
        verify(ipAdminWhiteService, never()).checkAdmin(any(), any(), anyString());
    }

    @Test
    void setAdminIpEnable_offRequiresClearToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(ipAdminWhiteService.isAdminIpEnable()).thenReturn(true);
        SecurityOpsConfig ops = new SecurityOpsConfig();
        ops.setClearToken("ops-secret");
        ops.setAdminIpEnable(true);
        when(sysConfigService.getSecurityOpsConfig()).thenReturn(ops);
        assertThrows(AException.class, () -> controller.setAdminIpEnable(false, request));
        verify(sysConfigService, never()).saveSecurityOpsConfig(any());
    }

    @Test
    void setAdminIpEnable_onClaimsCurrentIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.55");
        when(ipAdminWhiteService.isAdminIpEnable()).thenReturn(false, true);
        when(ipAdminWhiteService.isAdminWhite("203.0.113.55")).thenReturn(false);
        SecurityOpsConfig ops = new SecurityOpsConfig();
        ops.setAdminIpEnable(false);
        when(sysConfigService.getSecurityOpsConfig()).thenReturn(ops);
        when(sysConfigService.saveSecurityOpsConfig(any())).thenAnswer(inv -> inv.getArgument(0));
        IpAdminWhiteEntity entity = new IpAdminWhiteEntity();
        entity.setIp("203.0.113.55");
        when(ipAdminWhiteService.create(eq("203.0.113.55"), eq("enable"), anyString())).thenReturn(entity);

        Map<String, Object> result = controller.setAdminIpEnable(true, request);
        assertTrue(Boolean.TRUE.equals(result.get("adminIpEnable")));
        assertEquals("203.0.113.55", result.get("claimedIp"));
        verify(sysConfigService).saveSecurityOpsConfig(any());
        verify(ipAdminWhiteService).create(eq("203.0.113.55"), eq("enable"), anyString());
        verify(adminAccessGuard).auditWithIp(eq(request), eq("wall.adminIpEnable"), anyString());
    }

    @Test
    void setAdminIpEnable_offWithClearToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("x-clear-authentication", "ops-secret");
        when(ipAdminWhiteService.isAdminIpEnable()).thenReturn(true, false);
        SecurityOpsConfig ops = new SecurityOpsConfig();
        ops.setClearToken("ops-secret");
        ops.setAdminIpEnable(true);
        when(sysConfigService.getSecurityOpsConfig()).thenReturn(ops);
        when(sysConfigService.saveSecurityOpsConfig(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = controller.setAdminIpEnable(false, request);
        assertFalse(Boolean.TRUE.equals(result.get("adminIpEnable")));
        verify(sysConfigService).saveSecurityOpsConfig(any());
    }
}
