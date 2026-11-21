package cn.org.autumn.modules.ops.controller;

import cn.org.autumn.exception.AException;
import cn.org.autumn.model.OpsClientCertEntry;
import cn.org.autumn.model.Response;
import cn.org.autumn.model.SecurityOpsCertConfig;
import cn.org.autumn.model.SecurityOpsConfig;
import cn.org.autumn.modules.ops.service.OpsCertService;
import cn.org.autumn.modules.ops.service.OpsRuntimeSnapshotService;
import cn.org.autumn.modules.ops.service.OpsSessionService;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.sys.support.AdminAccessGuard;
import cn.org.autumn.modules.sys.support.ClearTokenSupport;
import cn.org.autumn.modules.wall.entity.IpAdminWhiteEntity;
import cn.org.autumn.modules.wall.entity.IpBlackEntity;
import cn.org.autumn.modules.wall.entity.IpWhiteEntity;
import cn.org.autumn.modules.wall.entity.JumpEntity;
import cn.org.autumn.modules.wall.entity.ShieldEntity;
import cn.org.autumn.modules.wall.service.IpAdminWhiteService;
import cn.org.autumn.modules.wall.service.IpBlackService;
import cn.org.autumn.modules.wall.service.IpWhiteService;
import cn.org.autumn.modules.wall.service.JumpService;
import cn.org.autumn.modules.wall.service.ShieldService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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

@SuppressWarnings("unchecked")
class OpsApiControllerTest {

    private OpsApiController controller;
    private OpsCertService opsCertService;
    private OpsSessionService opsSessionService;
    private SysConfigService sysConfigService;
    private IpAdminWhiteService ipAdminWhiteService;
    private IpWhiteService ipWhiteService;
    private IpBlackService ipBlackService;
    private ShieldService shieldService;
    private JumpService jumpService;
    private AdminAccessGuard adminAccessGuard;
    private OpsRuntimeSnapshotService opsRuntimeSnapshotService;

    @BeforeEach
    void setUp() {
        ClearTokenSupport.resetForTests();
        controller = new OpsApiController();
        opsCertService = mock(OpsCertService.class);
        opsSessionService = mock(OpsSessionService.class);
        sysConfigService = mock(SysConfigService.class);
        ipAdminWhiteService = mock(IpAdminWhiteService.class);
        ipWhiteService = mock(IpWhiteService.class);
        ipBlackService = mock(IpBlackService.class);
        shieldService = mock(ShieldService.class);
        jumpService = mock(JumpService.class);
        adminAccessGuard = mock(AdminAccessGuard.class);
        opsRuntimeSnapshotService = mock(OpsRuntimeSnapshotService.class);
        ReflectionTestUtils.setField(controller, "opsCertService", opsCertService);
        ReflectionTestUtils.setField(controller, "opsSessionService", opsSessionService);
        ReflectionTestUtils.setField(controller, "sysConfigService", sysConfigService);
        ReflectionTestUtils.setField(controller, "ipAdminWhiteService", ipAdminWhiteService);
        ReflectionTestUtils.setField(controller, "ipWhiteService", ipWhiteService);
        ReflectionTestUtils.setField(controller, "ipBlackService", ipBlackService);
        ReflectionTestUtils.setField(controller, "shieldService", shieldService);
        ReflectionTestUtils.setField(controller, "jumpService", jumpService);
        ReflectionTestUtils.setField(controller, "adminAccessGuard", adminAccessGuard);
        ReflectionTestUtils.setField(controller, "opsRuntimeSnapshotService", opsRuntimeSnapshotService);
    }

    @Test
    void probe_requiresFixedToken() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        assertThrows(AException.class, () -> controller.probe(req));

        req.addHeader(AutumnOpsProbe.HEADER, AutumnOpsProbe.TOKEN);
        when(opsCertService.opsCertStatus()).thenReturn("pending");
        when(opsCertService.isEnrollAvailable()).thenReturn(true);
        when(opsCertService.isDiscoverEnabled()).thenReturn(true);
        when(sysConfigService.isAdminIpEnable()).thenReturn(false);
        SecurityOpsCertConfig cfg = new SecurityOpsCertConfig();
        cfg.setSiteId("autumn-test");
        cfg.setFingerprint("abcd");
        when(opsCertService.getConfig()).thenReturn(cfg);

        Response<Map<String, Object>> res = controller.probe(req);
        assertEquals(0, res.getCode());
        assertEquals(true, res.getData().get("autumn"));
        assertEquals("pending", res.getData().get("opsCert"));
        assertEquals(true, res.getData().get("discoverEnabled"));
        assertFalse(String.valueOf(res.getData()).contains("Private"));
    }

    @Test
    void enroll_returnsPublicMaterialOnly_andAutoEnablesAdminIp() {
        OpsClientCertEntry entry = new OpsClientCertEntry();
        entry.setCertId("ops-1");
        when(opsCertService.enroll(anyString(), anyString(), anyString())).thenReturn(entry);
        when(opsCertService.isDiscoverEnabled()).thenReturn(false);
        SecurityOpsCertConfig cfg = new SecurityOpsCertConfig();
        cfg.setSiteId("site");
        cfg.setServerPublicKey("SERVER_PUB");
        cfg.setServerPrivateKey("SERVER_PRIV_MUST_NOT_LEAK");
        cfg.setFingerprint("fp");
        when(opsCertService.getConfig()).thenReturn(cfg);
        when(sysConfigService.isAdminIpEnable()).thenReturn(false, true);
        when(sysConfigService.getSecurityOpsConfig()).thenReturn(new SecurityOpsConfig());
        when(ipAdminWhiteService.isAdminWhite(anyString())).thenReturn(false);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("198.51.100.20");
        Map<String, Object> body = new HashMap<>();
        body.put("secret", "s");
        body.put("clientPublicKey", "CLIENT_PUB");
        Response<Map<String, Object>> res = controller.enroll(body, req);
        assertEquals("ops-1", res.getData().get("certId"));
        assertEquals("SERVER_PUB", res.getData().get("serverPublicKey"));
        assertEquals(true, res.getData().get("adminIpEnable"));
        assertEquals(false, res.getData().get("discoverEnabled"));
        assertFalse(res.getData().containsKey("serverPrivateKey"));
        assertFalse(res.getData().values().stream().anyMatch(v -> "SERVER_PRIV_MUST_NOT_LEAK".equals(String.valueOf(v))));
        verify(sysConfigService).saveSecurityOpsConfig(argThat(c -> c.isAdminIpEnable()));
        verify(ipAdminWhiteService).create(eq("198.51.100.20"), eq("ops-enroll"), anyString());
    }

    @Test
    void runtimeSnapshot_requiresSession_andReturnsJvmSystem() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        AException missing = assertThrows(AException.class, () -> controller.runtimeSnapshot(req));
        assertEquals(401, missing.getCode());

        OpsSessionService.OpsSession session = new OpsSessionService.OpsSession(
                "opss-1", "ops-cert", "aes", "", System.currentTimeMillis() + 60_000);
        when(opsSessionService.requireSession("opss-1")).thenReturn(session);
        req.addHeader(AutumnOpsProbe.SESSION_HEADER, "opss-1");
        Map<String, Object> snap = new HashMap<>();
        snap.put("collectedAt", 1L);
        snap.put("system", Map.of("available", true, "osName", "macOS"));
        snap.put("jvm", Map.of("available", true, "threadLive", 10));
        when(opsRuntimeSnapshotService.snapshot()).thenReturn(snap);

        Response<Map<String, Object>> res = controller.runtimeSnapshot(req);
        assertEquals(0, res.getCode());
        assertTrue(res.getData().containsKey("system"));
        assertTrue(res.getData().containsKey("jvm"));
        verify(adminAccessGuard).auditWithIp(eq(req), eq("ops.runtime.snapshot"), contains("ops-cert"));
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
    void gateVerifyClearToken_okAndReject() {
        OpsSessionService.OpsSession session = new OpsSessionService.OpsSession(
                "opss-1", "ops-cert", "aes", "", System.currentTimeMillis() + 60_000);
        when(opsSessionService.requireSession("opss-1")).thenReturn(session);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(AutumnOpsProbe.SESSION_HEADER, "opss-1");

        SecurityOpsConfig empty = new SecurityOpsConfig();
        empty.setClearToken("");
        when(sysConfigService.getSecurityOpsConfig()).thenReturn(empty);
        AException unset = assertThrows(AException.class, () -> controller.gateVerifyClearToken(
                Map.of("clearToken", "any"), req));
        assertTrue(unset.getMessage().contains("尚未配置"));

        SecurityOpsConfig ops = new SecurityOpsConfig();
        ops.setClearToken("tok");
        when(sysConfigService.getSecurityOpsConfig()).thenReturn(ops);

        assertThrows(AException.class, () -> controller.gateVerifyClearToken(
                Map.of("clearToken", "bad"), req));

        Response<Map<String, Object>> res = controller.gateVerifyClearToken(
                Map.of("clearToken", "tok"), req);
        assertEquals(true, res.getData().get("valid"));
    }

    @Test
    void gateSetClearToken_rejectsFirstTimeFromOps_andAllowsRotate() {
        OpsSessionService.OpsSession session = new OpsSessionService.OpsSession(
                "opss-1", "ops-cert", "aes", "", System.currentTimeMillis() + 60_000);
        when(opsSessionService.requireSession("opss-1")).thenReturn(session);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(AutumnOpsProbe.SESSION_HEADER, "opss-1");

        SecurityOpsConfig empty = new SecurityOpsConfig();
        empty.setClearToken("");
        when(sysConfigService.getSecurityOpsConfig()).thenReturn(empty);
        AException first = assertThrows(AException.class, () -> controller.gateSetClearToken(
                Map.of("clearToken", "sixteen-chars-01"), req));
        assertTrue(first.getMessage().contains("系统管理员"));

        SecurityOpsConfig existing = new SecurityOpsConfig();
        existing.setClearToken("sixteen-chars-01");
        when(sysConfigService.getSecurityOpsConfig()).thenReturn(existing);
        assertThrows(AException.class, () -> controller.gateSetClearToken(
                Map.of("clearToken", "sixteen-chars-02", "oldClearToken", "wrong-old-token!"), req));

        Response<Map<String, Object>> changed = controller.gateSetClearToken(
                Map.of("clearToken", "sixteen-chars-02", "oldClearToken", "sixteen-chars-01"), req);
        assertEquals(true, changed.getData().get("changed"));
        verify(sysConfigService).saveSecurityOpsConfig(argThat(c -> "sixteen-chars-02".equals(c.getClearToken())));
    }

    @Test
    void adminIpAdd_remoteOrCidrRequiresClearToken() {
        OpsSessionService.OpsSession session = new OpsSessionService.OpsSession(
                "opss-1", "ops-cert", "aes", "", System.currentTimeMillis() + 60_000);
        when(opsSessionService.requireSession("opss-1")).thenReturn(session);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("198.51.100.9");
        req.addHeader(AutumnOpsProbe.SESSION_HEADER, "opss-1");

        SecurityOpsConfig ops = new SecurityOpsConfig();
        ops.setClearToken("sixteen-chars-ok");
        when(sysConfigService.getSecurityOpsConfig()).thenReturn(ops);

        assertThrows(AException.class, () -> controller.adminIpAdd(
                Map.of("ip", "0.0.0.0/1"), req));

        IpAdminWhiteEntity entity = new IpAdminWhiteEntity();
        entity.setIp("203.0.113.50");
        when(ipAdminWhiteService.create(eq("203.0.113.50"), anyString(), anyString())).thenReturn(entity);
        Response<Map<String, Object>> res = controller.adminIpAdd(
                Map.of("ip", "203.0.113.50", "clearToken", "sixteen-chars-ok"), req);
        assertEquals(true, res.getData().get("success"));
    }

    @Test
    void gateGet_exposesClearTokenConfiguredOnly() {
        OpsSessionService.OpsSession session = new OpsSessionService.OpsSession(
                "opss-1", "ops-cert", "aes", "", System.currentTimeMillis() + 60_000);
        when(opsSessionService.requireSession("opss-1")).thenReturn(session);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(AutumnOpsProbe.SESSION_HEADER, "opss-1");
        when(sysConfigService.isAdminIpEnable()).thenReturn(false);
        when(opsCertService.isDiscoverEnabled()).thenReturn(false);

        SecurityOpsConfig ops = new SecurityOpsConfig();
        ops.setClearToken("secret-token");
        when(sysConfigService.getSecurityOpsConfig()).thenReturn(ops);

        Response<Map<String, Object>> res = controller.gateGet(req);
        assertEquals(true, res.getData().get("clearTokenConfigured"));
        assertEquals(false, res.getData().get("discoverEnabled"));
        assertFalse(res.getData().values().stream().anyMatch(v -> "secret-token".equals(String.valueOf(v))));
    }

    @Test
    void gateSetDiscoverEnabled_togglesWithoutClearToken() {
        OpsSessionService.OpsSession session = new OpsSessionService.OpsSession(
                "opss-1", "ops-cert", "aes", "", System.currentTimeMillis() + 60_000);
        when(opsSessionService.requireSession("opss-1")).thenReturn(session);
        when(opsCertService.isDiscoverEnabled()).thenReturn(true);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(AutumnOpsProbe.SESSION_HEADER, "opss-1");

        Response<Map<String, Object>> res = controller.gateSetDiscoverEnabled(
                Map.of("enable", 1), req);
        assertEquals(true, res.getData().get("discoverEnabled"));
        verify(opsCertService).setDiscoverEnabled(true);

        when(opsCertService.isDiscoverEnabled()).thenReturn(false);
        Response<Map<String, Object>> off = controller.gateSetDiscoverEnabled(
                Map.of("enable", false), req);
        assertEquals(false, off.getData().get("discoverEnabled"));
        verify(opsCertService).setDiscoverEnabled(false);
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
    void currentIp_returnsMembershipFlags() {
        OpsSessionService.OpsSession session = new OpsSessionService.OpsSession(
                "opss-1", "ops-cert", "aes", "", System.currentTimeMillis() + 60_000);
        when(opsSessionService.requireSession("opss-1")).thenReturn(session);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("203.0.113.50");
        req.addHeader(AutumnOpsProbe.SESSION_HEADER, "opss-1");
        when(ipAdminWhiteService.getByIp("203.0.113.50")).thenReturn(new IpAdminWhiteEntity());
        when(ipWhiteService.getByIp("203.0.113.50")).thenReturn(null);

        Response<Map<String, Object>> res = controller.currentIp(req);
        assertEquals("203.0.113.50", res.getData().get("ip"));
        assertEquals(true, res.getData().get("inAdmin"));
        assertEquals(false, res.getData().get("inWhite"));
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
        when(ipWhiteService.queryPage(anyMap())).thenReturn(new PageUtils(List.of(), 0, 20, 1));
        Response<Map<String, Object>> res = controller.ipWhiteList(Map.of(), req);
        assertEquals(0, res.getData().get("count"));
        assertEquals(1, res.getData().get("page"));
        assertEquals(20, res.getData().get("limit"));
        assertEquals(false, res.getData().get("hasMore"));
    }

    @Test
    void adminIpSetForbidden_togglesAndRejectsInvalid() {
        OpsSessionService.OpsSession session = new OpsSessionService.OpsSession(
                "opss-1", "ops-cert", "aes", "", System.currentTimeMillis() + 60_000);
        when(opsSessionService.requireSession("opss-1")).thenReturn(session);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(AutumnOpsProbe.SESSION_HEADER, "opss-1");

        assertThrows(AException.class, () -> controller.adminIpSetForbidden(
                Map.of("ip", "198.51.100.9", "forbidden", 2), req));

        IpAdminWhiteEntity entity = new IpAdminWhiteEntity();
        entity.setIp("198.51.100.9");
        entity.setForbidden(0);
        when(ipAdminWhiteService.getByIp("198.51.100.9")).thenReturn(entity);

        Response<Map<String, Object>> res = controller.adminIpSetForbidden(
                Map.of("ip", "198.51.100.9", "forbidden", 1), req);
        assertEquals(true, res.getData().get("success"));
        assertEquals(1, res.getData().get("forbidden"));
        verify(ipAdminWhiteService).updateById(argThat(e -> e.getForbidden() != null && e.getForbidden() == 1));
    }

    @Test
    void ipBlackSetAvailable_toggles() {
        OpsSessionService.OpsSession session = new OpsSessionService.OpsSession(
                "opss-1", "ops-cert", "aes", "", System.currentTimeMillis() + 60_000);
        when(opsSessionService.requireSession("opss-1")).thenReturn(session);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(AutumnOpsProbe.SESSION_HEADER, "opss-1");

        Response<Map<String, Object>> res = controller.ipBlackSetAvailable(
                Map.of("ip", "203.0.113.8", "available", 0), req);
        assertEquals(true, res.getData().get("success"));
        assertEquals(0, res.getData().get("available"));
        verify(ipBlackService).setAvailable("203.0.113.8", 0);
    }

    @Test
    void ipBlackAdd_rejectsPolicyIp() {
        OpsSessionService.OpsSession session = new OpsSessionService.OpsSession(
                "opss-1", "ops-cert", "aes", "", System.currentTimeMillis() + 60_000);
        when(opsSessionService.requireSession("opss-1")).thenReturn(session);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(AutumnOpsProbe.SESSION_HEADER, "opss-1");

        AException ex = assertThrows(AException.class,
                () -> controller.ipBlackAdd(Map.of("ip", "0.0.0.0"), req));
        assertTrue(ex.getMsg().contains("策略"));
        verify(ipBlackService, never()).create(anyString(), anyString(), anyString());
    }

    @Test
    void ipBlackFirewallCount_getAndSet() {
        OpsSessionService.OpsSession session = new OpsSessionService.OpsSession(
                "opss-1", "ops-cert", "aes", "", System.currentTimeMillis() + 60_000);
        when(opsSessionService.requireSession("opss-1")).thenReturn(session);
        when(ipBlackService.getFirewallCount()).thenReturn(1000, 2000);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(AutumnOpsProbe.SESSION_HEADER, "opss-1");

        Response<Map<String, Object>> got = controller.ipBlackFirewallCountGet(req);
        assertEquals(1000, got.getData().get("firewallCount"));

        Response<Map<String, Object>> set = controller.ipBlackFirewallCountSet(Map.of("count", 2000), req);
        assertEquals(true, set.getData().get("success"));
        assertEquals(2000, set.getData().get("firewallCount"));
        verify(ipBlackService).setFirewallCount(2000);
    }

    @Test
    void shieldSave_andStatus_requireSession() {
        MockHttpServletRequest bare = new MockHttpServletRequest();
        assertThrows(AException.class, () -> controller.shieldList(Map.of(), bare));

        OpsSessionService.OpsSession session = new OpsSessionService.OpsSession(
                "opss-1", "ops-cert", "aes", "", System.currentTimeMillis() + 60_000);
        when(opsSessionService.requireSession("opss-1")).thenReturn(session);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(AutumnOpsProbe.SESSION_HEADER, "opss-1");
        when(shieldService.getOne(any(QueryWrapper.class), eq(false))).thenReturn(null);
        when(shieldService.save(any(ShieldEntity.class))).thenAnswer(inv -> {
            ShieldEntity e = inv.getArgument(0);
            e.setId(9L);
            return true;
        });
        when(shieldService.getEnable()).thenReturn(true);

        Response<Map<String, Object>> saved = controller.shieldSave(
                Map.of("uri", "/api/test", "enable", true, "auto", 10000), req);
        assertEquals("/api/test", saved.getData().get("uri"));
        verify(shieldService).clear();

        Response<Map<String, Object>> status = controller.shieldStatus(req);
        assertEquals(true, status.getData().get("enable"));
        assertTrue(status.getData().containsKey("attack"));
        assertTrue(status.getData().containsKey("print"));
    }

    @Test
    void jumpSave_rejectsBlank() {
        OpsSessionService.OpsSession session = new OpsSessionService.OpsSession(
                "opss-1", "ops-cert", "aes", "", System.currentTimeMillis() + 60_000);
        when(opsSessionService.requireSession("opss-1")).thenReturn(session);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(AutumnOpsProbe.SESSION_HEADER, "opss-1");

        assertThrows(AException.class, () -> controller.jumpSave(
                Map.of("host", "a.com", "uri", "/", "url", ""), req));

        when(jumpService.getOne(any(QueryWrapper.class), eq(false))).thenReturn(null);
        when(jumpService.save(any(JumpEntity.class))).thenAnswer(inv -> {
            JumpEntity e = inv.getArgument(0);
            e.setId(3L);
            return true;
        });
        Response<Map<String, Object>> res = controller.jumpSave(
                Map.of("host", "a.com", "uri", "/", "url", "https://x"), req);
        assertEquals("a.com", res.getData().get("host"));
        verify(jumpService).clear();
    }

    @Test
    void adminIpList_pageMeta_hasMore() {
        OpsSessionService.OpsSession session = new OpsSessionService.OpsSession(
                "opss-1", "ops-cert", "aes", "", System.currentTimeMillis() + 60_000);
        when(opsSessionService.requireSession("opss-1")).thenReturn(session);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(AutumnOpsProbe.SESSION_HEADER, "opss-1");

        IpAdminWhiteEntity e = new IpAdminWhiteEntity();
        e.setIp("198.51.100.1");
        e.setForbidden(0);
        when(ipAdminWhiteService.queryPage(anyMap())).thenReturn(new PageUtils(List.of(e), 25, 20, 1));

        Response<Map<String, Object>> res = controller.adminIpList(Map.of("page", 1, "limit", 20), req);
        assertEquals(25, res.getData().get("count"));
        assertEquals(true, res.getData().get("hasMore"));
        assertEquals(1, ((List<?>) res.getData().get("list")).size());
    }
}
