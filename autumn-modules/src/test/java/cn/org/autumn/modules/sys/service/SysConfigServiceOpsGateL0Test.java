package cn.org.autumn.modules.sys.service;

import cn.org.autumn.config.GsonConfig;
import cn.org.autumn.model.SecurityOpsConfig;
import cn.org.autumn.modules.sys.dao.SysConfigDao;
import cn.org.autumn.modules.sys.entity.SysConfigEntity;
import cn.org.autumn.modules.sys.redis.SysConfigRedis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SECURITY_OPS_CONFIG 门禁 L0：本机 volatile 快照 / 布尔位与失效同步。
 */
class SysConfigServiceOpsGateL0Test {

    private SysConfigService service;
    private SysConfigDao dao;
    private AtomicInteger dbQueryCount;
    private AtomicReference<SysConfigEntity> dbRow;

    @BeforeEach
    void setUp() {
        dbQueryCount = new AtomicInteger();
        dbRow = new AtomicReference<>();

        dao = mock(SysConfigDao.class);
        when(dao.queryByKey(anyString())).thenAnswer(inv -> {
            dbQueryCount.incrementAndGet();
            SysConfigEntity row = dbRow.get();
            if (row == null || !inv.getArgument(0).equals(row.getParamKey())) {
                return null;
            }
            SysConfigEntity copy = new SysConfigEntity();
            copy.setParamKey(row.getParamKey());
            copy.setParamValue(row.getParamValue());
            return copy;
        });
        when(dao.updateValueByKey(anyString(), anyString())).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            String value = inv.getArgument(1);
            SysConfigEntity row = new SysConfigEntity();
            row.setParamKey(key);
            row.setParamValue(value);
            dbRow.set(row);
            return 1;
        });

        SysConfigRedis redis = mock(SysConfigRedis.class);
        when(redis.get(anyString())).thenReturn(null);
        doAnswer(inv -> null).when(redis).saveOrUpdate(any());
        doAnswer(inv -> null).when(redis).delete(anyString());

        AsyncTaskExecutor executor = mock(AsyncTaskExecutor.class);
        // 不跑 onFiveMinute：验证 update 本机路径已即时刷新 L0
        doAnswer(inv -> null).when(executor).execute(any(Runnable.class));

        service = new SysConfigService();
        ReflectionTestUtils.setField(service, "map", new ConcurrentHashMap<String, SysConfigEntity>());
        ReflectionTestUtils.setField(service, "baseMapper", dao);
        ReflectionTestUtils.setField(service, "sysConfigRedis", redis);
        ReflectionTestUtils.setField(service, "asyncTaskExecutor", executor);
        // 无字段加密：updateValueByKey 直写明文
        ReflectionTestUtils.setField(service, "encrypt", null);
    }

    private void putOpsInMap(SecurityOpsConfig ops) {
        SysConfigEntity entity = new SysConfigEntity();
        entity.setParamKey(SysConfigService.SECURITY_OPS_CONFIG);
        entity.setParamValue(GsonConfig.getGson().toJson(ops));
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, SysConfigEntity> map =
                (ConcurrentHashMap<String, SysConfigEntity>) ReflectionTestUtils.getField(service, "map");
        map.put(SysConfigService.SECURITY_OPS_CONFIG, entity);
        dbRow.set(entity);
    }

    @Test
    void isAdminIpEnable_defaultsFalseAndCachesL0() {
        assertFalse(service.isAdminIpEnable());
        assertFalse(service.isBeanExecuteEnabled());
        assertFalse(service.isWebShellEnabled());
        int afterFirst = dbQueryCount.get();
        assertFalse(service.isAdminIpEnable());
        assertEquals(afterFirst, dbQueryCount.get(), "L0 命中后不应再打 DB");
    }

    @Test
    void isAdminIpEnable_readsFromMapWithoutRepeatedDb() {
        SecurityOpsConfig ops = new SecurityOpsConfig();
        ops.setAdminIpEnable(true);
        ops.setBeanExecuteEnabled(true);
        putOpsInMap(ops);
        dbQueryCount.set(0);

        assertTrue(service.isAdminIpEnable());
        assertTrue(service.isBeanExecuteEnabled());
        assertEquals(0, dbQueryCount.get());
        assertTrue(service.isAdminIpEnable());
        assertEquals(0, dbQueryCount.get());
    }

    @Test
    void invalidateLocalCache_refreshesGateFlags() {
        SecurityOpsConfig ops = new SecurityOpsConfig();
        ops.setAdminIpEnable(false);
        putOpsInMap(ops);
        assertFalse(service.isAdminIpEnable());

        SecurityOpsConfig next = new SecurityOpsConfig();
        next.setAdminIpEnable(true);
        putOpsInMap(next);
        service.invalidateLocalCache(SysConfigService.SECURITY_OPS_CONFIG);
        // invalidate 清了 map；再放入以便回源
        putOpsInMap(next);

        assertTrue(service.isAdminIpEnable());
        SecurityOpsConfig snap = service.getSecurityOpsConfig();
        assertTrue(snap.isAdminIpEnable());
    }

    @Test
    void saveSecurityOpsConfig_installsImmediateL0() {
        SecurityOpsConfig toSave = new SecurityOpsConfig();
        toSave.setClearToken("tok");
        toSave.setAdminIpEnable(true);
        toSave.setWebShellEnabled(true);

        // updateValueByKey 需要 encrypt；空 encrypt 会 NPE。用可透传的 stub。
        Object encrypt = mock(cn.org.autumn.crypto.FieldEncryptService.class);
        when(((cn.org.autumn.crypto.FieldEncryptService) encrypt).encryptValue(anyString(), any()))
                .thenAnswer(inv -> inv.getArgument(0));
        ReflectionTestUtils.setField(service, "encrypt", encrypt);

        service.saveSecurityOpsConfig(toSave);
        assertTrue(service.isAdminIpEnable());
        assertTrue(service.isWebShellEnabled());
        assertFalse(service.isBeanExecuteEnabled());
        assertEquals("tok", service.getSecurityOpsConfig().getClearToken());
    }

    @Test
    void ensureOpsGate_discardsStaleInstallWhenEpochBumpedMidLoad() {
        SecurityOpsConfig ops = new SecurityOpsConfig();
        ops.setAdminIpEnable(true);
        putOpsInMap(ops);

        // 先装载 L0
        assertTrue(service.isAdminIpEnable());

        // 模拟「加载中失效」：bump epoch 且清空快照后，未重新 put map 时回源 seed（false）
        service.invalidateLocalCache(SysConfigService.SECURITY_OPS_CONFIG);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, SysConfigEntity> map =
                (ConcurrentHashMap<String, SysConfigEntity>) ReflectionTestUtils.getField(service, "map");
        map.clear();
        dbRow.set(null);

        assertFalse(service.isAdminIpEnable());
        // 再写入 true 并失效后应读到 true
        putOpsInMap(ops);
        service.invalidateLocalCache(SysConfigService.SECURITY_OPS_CONFIG);
        putOpsInMap(ops);
        assertTrue(service.isAdminIpEnable());
    }

    @Test
    void invalidateOpsGateL0_failClosedBooleansBeforeReload() {
        SecurityOpsConfig ops = new SecurityOpsConfig();
        ops.setAdminIpEnable(true);
        ops.setBeanExecuteEnabled(true);
        ops.setWebShellEnabled(true);
        putOpsInMap(ops);
        assertTrue(service.isAdminIpEnable());
        assertTrue(service.isBeanExecuteEnabled());
        assertTrue(service.isWebShellEnabled());

        ReflectionTestUtils.invokeMethod(service, "invalidateOpsGateL0");
        assertFalse((Boolean) ReflectionTestUtils.getField(service, "gateAdminIpEnable"));
        assertFalse((Boolean) ReflectionTestUtils.getField(service, "gateBeanExecuteEnabled"));
        assertFalse((Boolean) ReflectionTestUtils.getField(service, "gateWebShellEnabled"));
    }

    @Test
    void updateEntity_refreshesOpsGateImmediatelyWithoutFiveMinute() {
        SecurityOpsConfig initial = new SecurityOpsConfig();
        initial.setAdminIpEnable(false);
        putOpsInMap(initial);
        assertFalse(service.isAdminIpEnable());

        SecurityOpsConfig next = new SecurityOpsConfig();
        next.setAdminIpEnable(true);
        next.setClearToken("new-clear");
        SysConfigEntity entity = new SysConfigEntity();
        entity.setId(1L);
        entity.setParamKey(SysConfigService.SECURITY_OPS_CONFIG);
        entity.setParamValue(GsonConfig.getGson().toJson(next));
        when(dao.updateById(any(SysConfigEntity.class))).thenReturn(1);

        service.update(entity);

        assertTrue(service.isAdminIpEnable());
        assertEquals("new-clear", service.getSecurityOpsConfig().getClearToken());
    }

    @Test
    void getSecurityOpsConfig_returnsDefensiveCopy() {
        SecurityOpsConfig ops = new SecurityOpsConfig();
        ops.setAdminIpEnable(true);
        ops.setClearToken("secret");
        putOpsInMap(ops);

        SecurityOpsConfig a = service.getSecurityOpsConfig();
        SecurityOpsConfig b = service.getSecurityOpsConfig();
        assertNotSame(a, b);
        a.setAdminIpEnable(false);
        a.setClearToken("mutated");
        assertTrue(service.isAdminIpEnable());
        assertEquals("secret", service.getSecurityOpsConfig().getClearToken());
    }
}
