package cn.org.autumn.modules.ops.service;

import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.wall.service.IpAdminWhiteService;
import cn.org.autumn.modules.wall.service.IpBlackService;
import cn.org.autumn.modules.wall.service.IpWhiteService;
import cn.org.autumn.modules.wall.service.ShieldService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OpsRuntimeSnapshotServiceTest {

    private OpsRuntimeSnapshotService service;
    private SysConfigService sysConfigService;
    private IpAdminWhiteService ipAdminWhiteService;
    private IpWhiteService ipWhiteService;
    private IpBlackService ipBlackService;
    private ShieldService shieldService;
    private OpsRuntimeTrafficCounter trafficCounter;

    @BeforeEach
    void setUp() {
        service = new OpsRuntimeSnapshotService();
        sysConfigService = mock(SysConfigService.class);
        ipAdminWhiteService = mock(IpAdminWhiteService.class);
        ipWhiteService = mock(IpWhiteService.class);
        ipBlackService = mock(IpBlackService.class);
        shieldService = mock(ShieldService.class);
        trafficCounter = new OpsRuntimeTrafficCounter();
        ReflectionTestUtils.setField(service, "sysConfigService", sysConfigService);
        ReflectionTestUtils.setField(service, "ipAdminWhiteService", ipAdminWhiteService);
        ReflectionTestUtils.setField(service, "ipWhiteService", ipWhiteService);
        ReflectionTestUtils.setField(service, "ipBlackService", ipBlackService);
        ReflectionTestUtils.setField(service, "shieldService", shieldService);
        ReflectionTestUtils.setField(service, "trafficCounter", trafficCounter);
        ReflectionTestUtils.setField(service, "backupDir", System.getProperty("java.io.tmpdir"));

        when(sysConfigService.isAdminIpEnable()).thenReturn(true);
        when(ipAdminWhiteService.count()).thenReturn(2L);
        when(ipWhiteService.count()).thenReturn(3L);
        when(ipBlackService.count()).thenReturn(4L);
        when(ipBlackService.hasIp(IpBlackService.POLICY_IP)).thenReturn(true);
        when(ipBlackService.getFirewallCount()).thenReturn(500);
        when(shieldService.getEnable()).thenReturn(true);
        when(shieldService.getVisitWindowSize()).thenReturn(0);
        when(shieldService.getAllowedIpSize()).thenReturn(0);
        when(shieldService.getAllowedIpSample(anyInt())).thenReturn(java.util.List.of());
    }

    @Test
    void collectJvm_exposesHeapAndThreads() {
        Map<String, Object> jvm = service.collectJvm();
        assertEquals(true, jvm.get("available"));
        assertTrue(jvm.containsKey("uptimeMs"));
        assertTrue(jvm.containsKey("javaVersion"));
        assertNotNull(jvm.get("heap"));
        @SuppressWarnings("unchecked")
        Map<String, Object> heap = (Map<String, Object>) jvm.get("heap");
        assertTrue(heap.containsKey("used"));
        assertTrue(heap.containsKey("max"));
        assertTrue(((Number) jvm.get("threadLive")).intValue() > 0);
    }

    @Test
    void collectSystem_exposesOsAndProcessors() throws Exception {
        Map<String, Object> system = service.collectSystem();
        assertEquals(true, system.get("available"));
        assertNotNull(system.get("osName"));
        assertTrue(((Number) system.get("availableProcessors")).intValue() >= 1);
    }

    @Test
    void snapshot_includesCoreSections_withoutDataSource() {
        Map<String, Object> snap = service.snapshot();
        assertTrue(snap.containsKey("collectedAt"));
        assertTrue(snap.containsKey("system"));
        assertTrue(snap.containsKey("jvm"));
        assertTrue(snap.containsKey("disk"));
        assertTrue(snap.containsKey("database"));
        assertTrue(snap.containsKey("firewall"));
        assertTrue(snap.containsKey("traffic"));
        assertTrue(snap.containsKey("redis"));
        assertTrue(snap.containsKey("paths"));
        @SuppressWarnings("unchecked")
        Map<String, Object> db = (Map<String, Object>) snap.get("database");
        assertEquals(false, db.get("available"));
        @SuppressWarnings("unchecked")
        Map<String, Object> redis = (Map<String, Object>) snap.get("redis");
        assertEquals(false, redis.get("enabled"));
        @SuppressWarnings("unchecked")
        Map<String, Object> firewall = (Map<String, Object>) snap.get("firewall");
        assertEquals(true, firewall.get("adminIpEnable"));
        assertEquals(3L, ((Number) firewall.get("ipBlackCount")).longValue());
    }

    @Test
    void trafficCounter_recordsIntoSnapshot() {
        trafficCounter.record();
        trafficCounter.record();
        Map<String, Object> traffic = service.collectTraffic();
        assertEquals(2L, ((Number) traffic.get("requestTotal")).longValue());
        assertTrue(((Number) traffic.get("approxQps")).doubleValue() >= 0);
    }
}
