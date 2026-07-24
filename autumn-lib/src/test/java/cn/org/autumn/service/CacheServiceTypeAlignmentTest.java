package cn.org.autumn.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import cn.org.autumn.config.CacheConfig;
import cn.org.autumn.config.EhCacheManager;
import cn.org.autumn.model.AesKey;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.ehcache.Cache;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 回归：Null 扩宽与声明类型分离、compute/get 交替、Null 粘性。
 */
public class CacheServiceTypeAlignmentTest {

    private EhCacheManager ehCacheManager;
    private CacheService cacheService;

    @Before
    public void setUp() {
        ehCacheManager = new EhCacheManager();
        cacheService = new CacheService();
        ReflectionTestUtils.setField(cacheService, "ehCacheManager", ehCacheManager);
    }

    @Test
    public void computeAndGetAlternateWithoutThrashAndKeepDeclaredType() {
        CacheConfig declared = CacheConfig.builder()
                .name("aseservice-align")
                .key(String.class)
                .value(AesKey.class)
                .expire(10)
                .unit(TimeUnit.MINUTES)
                .Null(true)
                .build();
        declared.validate();

        AtomicInteger loads = new AtomicInteger();
        AesKey first = AesKey.builder().session("s1").key("k").vector("v").expireTime(System.currentTimeMillis() + 60_000).build();

        for (int i = 0; i < 30; i++) {
            AesKey viaCompute = cacheService.compute("s1", () -> {
                loads.incrementAndGet();
                return first;
            }, declared);
            AesKey viaGet = cacheService.get(declared, "s1");
            assertNotNull(viaCompute);
            assertNotNull(viaGet);
            assertEquals("k", viaGet.getKey());
        }

        assertEquals(1, loads.get());
        CacheConfig registered = ehCacheManager.getConfig("aseservice-align");
        assertNotNull(registered);
        assertSame(AesKey.class, registered.getValue());
        assertTrue(registered.isNull());

        Cache<?, ?> runtime = ehCacheManager.getCache("aseservice-align");
        assertNotNull(runtime);
        assertSame(Object.class, runtime.getRuntimeConfiguration().getValueType());
    }

    @Test
    public void nullFlagStickyAfterFirstRegister() {
        String name = "sticky-null-cache";
        AtomicInteger loads = new AtomicInteger();

        // 首次以 Null=true 注册并写入
        String v1 = cacheService.compute(name, "k1", () -> {
            loads.incrementAndGet();
            return "value";
        }, String.class, String.class, 10L, 10L, TimeUnit.MINUTES, 100L, true, null, null);
        assertEquals("value", v1);
        assertEquals(1, loads.get());
        assertTrue(ehCacheManager.getConfig(name).isNull());
        assertSame(String.class, ehCacheManager.getConfig(name).getValue());

        // 再次以 Null=false 请求同名：应忽略翻转，继续命中本地缓存
        String v2 = cacheService.compute(name, "k1", () -> {
            loads.incrementAndGet();
            return "other";
        }, String.class, String.class, 10L, 10L, TimeUnit.MINUTES, 100L, false, null, null);
        assertEquals("value", v2);
        assertEquals(1, loads.get());
        assertTrue(ehCacheManager.getConfig(name).isNull());
        assertSame(Object.class, ehCacheManager.getCache(name).getRuntimeConfiguration().getValueType());
    }

    @Test
    public void putGetWithNullTrueCachesPlaceholder() {
        CacheConfig config = CacheConfig.builder()
                .name("null-placeholder")
                .key(String.class)
                .value(String.class)
                .expire(5)
                .unit(TimeUnit.MINUTES)
                .Null(true)
                .build();
        config.validate();

        AtomicInteger loads = new AtomicInteger();
        String miss = cacheService.compute("x", () -> {
            loads.incrementAndGet();
            return null;
        }, config);
        assertNull(miss);
        assertEquals(1, loads.get());

        String again = cacheService.compute("x", () -> {
            loads.incrementAndGet();
            return "should-not-run";
        }, config);
        assertNull(again);
        assertEquals(1, loads.get());
        assertSame(String.class, ehCacheManager.getConfig("null-placeholder").getValue());
    }
}
