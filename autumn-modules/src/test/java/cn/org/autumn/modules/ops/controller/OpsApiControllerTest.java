package cn.org.autumn.modules.ops.controller;

import cn.org.autumn.exception.AException;
import cn.org.autumn.model.OpsClientCertEntry;
import cn.org.autumn.model.Response;
import cn.org.autumn.model.SecurityOpsCertConfig;
import cn.org.autumn.model.SecurityOpsConfig;
import cn.org.autumn.modules.ops.service.OpsCertService;
import cn.org.autumn.modules.ops.service.OpsSessionService;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.sys.support.AdminAccessGuard;
import cn.org.autumn.modules.wall.entity.IpAdminWhiteEntity;
import cn.org.autumn.modules.wall.entity.IpWhiteEntity;
import cn.org.autumn.modules.wall.service.IpAdminWhiteService;
import cn.org.autumn.modules.wall.service.IpBlackService;
import cn.org.autumn.modules.wall.service.IpWhiteService;
import cn.org.autumn.ops.AutumnOpsProbe;
import cn.org.autumn.utils.PageUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OpsApiControllerTest {

    private OpsApiController controller;
    private OpsCertService opsCertService;
    private OpsSessionService opsSessionService;
    private SysConfigService sysConfigService;
    private IpAdminWhiteService ipAdminWhiteService;
    private IpWhiteService ipWhiteService;
    private IpBlackService ipBlackService;
    private AdminAccessGuard adminAccessGuard;

    @BeforeEach
    void setUp() {
        controller = new OpsApiController();
        opsCertService = mock(OpsCertService.class);
        opsSessionService = mock(OpsSessionService.class);
        sysConfigService = mock(SysConfigService.class);
        ipAdminWhiteService = mock(IpAdminWhiteService.class);
        ipWhiteService = mock(IpWhiteService.class);
        ipBlackService = mock(IpBlackService.class);
        adminAccessGuard = mock(AdminAccessGuard.class);
        ReflectionTestUtils.setField(controller, "opsCertService", opsCertService);
        ReflectionTestUtils.setField(controller, "opsSessionService", opsSessionService);
        ReflectionTestUtils.setField(controller, "sysConfigService", sysConfigService);
        ReflectionTestUtils.setField(controller, "ipAdminWhiteService", ipAdminWhiteService);
        ReflectionTestUtils.setField(controller, "ipWhiteService", ipWhiteService);
        ReflectionTestUtils.setField(controller, "ipBlackService", ipBlackService);
        ReflectionTestUtils.setField(controller, "adminAccessGuard", adminAccessGuard);
    }

    @Test
    void probe_requiresFixedToken() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        assertThrows(AException.class, () -> controller.probe(req));

        req.addHeader(AutumnOpsProbe.HEADER, AutumnOpsProbe.TOKEN);
        when(opsCertService.opsCertStatus()).thenReturn("pending");
        when(opsCertService.isEnrollAvailable()).thenReturn(true);
        when(sysConfigService.isAdminIpEnable()).thenReturn(false);
        SecurityOpsCertConfig cfg = new SecurityOpsCertConfig();
        cfg.setSiteId("autumn-test");
        cfg.setFingerprint("abcd");
        when(opsCertService.getConfig()).thenReturn(cfg);

        Response<Map<String, Object>> res = controller.probe(req);
        assertEquals(0, res.getCode());
        assertEquals(true, res.getData().get("autumn"));
        assertEquals("pending", res.getData().get("opsCert"));
        assertFalse(String.valueOf(res.getData()).contains("Private"));
    }

    @Test
    void enroll_returnsPublicMaterialOnly() {
        OpsClientCertEntry entry = new OpsClientCertEntry();
        entry.setCertId("ops-1");
        when(opsCertService.enroll(anyString(), anyString(), anyString())).thenReturn(entry);
        SecurityOpsCertConfig cfg = new SecurityOpsCertConfig();
        cfg.setSiteId("site");
        cfg.setServerPublicKey("SERVER_PUB");
        cfg.setServerPrivateKey("SERVER_PRIV_MUST_NOT_LEAK");
        cfg.setFingerprint("fp");
        when(opsCertService.getConfig()).thenReturn(cfg);

        Map<String, Object> body = new HashMap<>();
        body.put("secret", "s");
        body.put("clientPublicKey", "CLIENT_PUB");
        Response<Map<String, Object>> res = controller.enroll(body, new MockHttpServletRequest());
        assertEquals("ops-1", res.getData().get("certId"));
        assertEquals("SERVER_PUB", res.getData().get("serverPublicKey"));
        assertFalse(res.getData().containsKey("serverPrivateKey"));
        assertFalse(res.getData().values().stream().anyMatch(v -> "SERVER_PRIV_MUST_NOT_LEAK".equals(String.valueOf(v))));
    }

    @Test
    void adminIpAdd_requiresSession_andClaimsCurrentIp() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("198.51.100.9");
        assertThrows(AException.class, () -> controller.adminIpAdd(Map.of("claimCurrent", true), req));

        OpsSessionService.OpsSession session = new OpsSessionService.OpsSession(
                "opss-1", "ops-cert", "aes", "", System.currentTimeMillis() + 60_000);
        when(opsSessionService.requireSession("opss-1")).thenReturn(session);
        req.addHeader(AutumnOpsProbe.SESSION_HEADER, "opss-1");
        IpAdminWhiteEntity entity = new IpAdminWhiteEntity();
        entity.setIp("198.51.100.9");
        when(ipAdminWhiteService.create(eq("198.51.100.9"), anyString(), anyString())).thenReturn(entity);

        Response<Map<String, Object>> res = controller.adminIpAdd(Map.of("claimCurrent", true), req);
        assertEquals(true, res.getData().get("success"));
        assertEquals("198.51.100.9", res.getData().get("ip"));
    }

    @Test
    void gateSetAdminIpEnable_offRequiresClearToken() {
        OpsSessionService.OpsSession session = new OpsSessionService.OpsSession(
                "opss-1", "ops-cert", "aes", "", System.currentTimeMillis() + 60_000);
        when(opsSessionService.requireSession("opss-1")).thenReturn(session);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(AutumnOpsProbe.SESSION_HEADER, "opss-1");

        SecurityOpsConfig ops = new SecurityOpsConfig();
        ops.setClearToken("tok");
        when(sysConfigService.getSecurityOpsConfig()).thenReturn(ops);

        assertThrows(AException.class, () -> controller.gateSetAdminIpEnable(
                Map.of("enable", false, "clearToken", "bad"), req));

        when(sysConfigService.isAdminIpEnable()).thenReturn(false);
        Response<Map<String, Object>> res = controller.gateSetAdminIpEnable(
                Map.of("enable", false, "clearToken", "tok"), req);
        assertEquals(false, res.getData().get("adminIpEnable"));
        verify(sysConfigService).saveSecurityOpsConfig(any());
    }

    @Test
    void adminIpRemove_currentIpRequiresClearToken() {
        OpsSessionService.OpsSession session = new OpsSessionService.OpsSession(
                "opss-1", "ops-cert", "aes", "", System.currentTimeMillis() + 60_000);
        when(opsSessionService.requireSession("opss-1")).thenReturn(session);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("198.51.100.9");
        req.addHeader(AutumnOpsProbe.SESSION_HEADER, "opss-1");

        SecurityOpsConfig ops = new SecurityOpsConfig();
        ops.setClearToken("tok");
        when(sysConfigService.getSecurityOpsConfig()).thenReturn(ops);

        assertThrows(AException.class, () -> controller.adminIpRemove(
                Map.of("ip", "198.51.100.9", "clearToken", "bad"), req));

        Response<Map<String, Object>> res = controller.adminIpRemove(
                Map.of("ip", "198.51.100.9", "clearToken", "tok"), req);
        assertEquals(true, res.getData().get("success"));
        verify(ipAdminWhiteService).removeByIp("198.51.100.9");
    }

    @Test
    void ipWhiteAdd_claimCurrent() {
        OpsSessionService.OpsSession session = new OpsSessionService.OpsSession(
                "opss-1", "ops-cert", "aes", "", System.currentTimeMillis() + 60_000);
        when(opsSessionService.requireSession("opss-1")).thenReturn(session);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("203.0.113.10");
        req.addHeader(AutumnOpsProbe.SESSION_HEADER, "opss-1");
        IpWhiteEntity entity = new IpWhiteEntity();
        entity.setIp("203.0.113.10");
        when(ipWhiteService.create(eq("203.0.113.10"), anyString(), anyString())).thenReturn(entity);

        Response<Map<String, Object>> res = controller.ipWhiteAdd(Map.of("claimCurrent", true), req);
        assertEquals("203.0.113.10", res.getData().get("ip"));
    }

    @Test
    void ipWhiteList_requiresSession() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        assertThrows(AException.class, () -> controller.ipWhiteList(Map.of(), req));

        OpsSessionService.OpsSession session = new OpsSessionService.OpsSession(
                "opss-1", "ops-cert", "aes", "", System.currentTimeMillis() + 60_000);
        when(opsSessionService.requireSession("opss-1")).thenReturn(session);
        req.addHeader(AutumnOpsProbe.SESSION_HEADER, "opss-1");
        when(ipWhiteService.queryPage(anyMap())).thenReturn(new PageUtils(List.of(), 0, 10, 1));
        Response<Map<String, Object>> res = controller.ipWhiteList(Map.of(), req);
        assertEquals(0, res.getData().get("count"));
    }
}
