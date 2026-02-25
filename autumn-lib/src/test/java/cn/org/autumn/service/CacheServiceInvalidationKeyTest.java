package cn.org.autumn.service;

import cn.org.autumn.config.CacheConfig;
import cn.org.autumn.config.EhCacheManager;
import cn.org.autumn.model.Invalidation;
import org.ehcache.Cache;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CacheServiceInvalidationKeyTest {

    @Test
    public void shouldRemoveWithLongKeyWhenInvalidationKeyIsString() {
        CacheService service = new CacheService();
        EhCacheManager ehCacheManager = mock(EhCacheManager.class);
        Cache<Object, Object> cache = mockCache();
        ReflectionTestUtils.setField(service, "ehCacheManager", ehCacheManager);

        String cacheName = "testcachename";
        String rawKey = "383194252848533504";
        CacheConfig config = CacheConfig.builder()
                .name(cacheName)
                .key(Long.class)
                .value(Object.class)
                .build();

        when(ehCacheManager.getConfig(cacheName)).thenReturn(config);
        when(ehCacheManager.getCache(cacheName)).thenReturn(cache);

        Invalidation message = new Invalidation(cacheName, rawKey, Invalidation.Operation.PUT);
        ReflectionTestUtils.invokeMethod(service, "handle", message);

        verify(cache).remove(383194252848533504L);
        verify(cache, never()).remove(rawKey);
    }

    @Test
    public void shouldRemoveWithIntegerKeyWhenInvalidationKeyIsDouble() {
        CacheService service = new CacheService();
        EhCacheManager ehCacheManager = mock(EhCacheManager.class);
        Cache<Object, Object> cache = mockCache();
        ReflectionTestUtils.setField(service, "ehCacheManager", ehCacheManager);

        String cacheName = "counter-cache";
        CacheConfig config = CacheConfig.builder()
                .name(cacheName)
                .key(Integer.class)
                .value(Object.class)
                .build();

        when(ehCacheManager.getConfig(cacheName)).thenReturn(config);
        when(ehCacheManager.getCache(cacheName)).thenReturn(cache);

        Invalidation message = new Invalidation(cacheName, 12.0d, Invalidation.Operation.REMOVE);
        ReflectionTestUtils.invokeMethod(service, "handle", message);

        verify(cache).remove(12);
    }

    @Test
    public void shouldFallbackToRawKeyWhenKeyConversionFails() {
        CacheService service = new CacheService();
        EhCacheManager ehCacheManager = mock(EhCacheManager.class);
        Cache<Object, Object> cache = mockCache();
        ReflectionTestUtils.setField(service, "ehCacheManager", ehCacheManager);

        String cacheName = "testcachename";
        String rawKey = "not-a-number";
        CacheConfig config = CacheConfig.builder()
                .name(cacheName)
                .key(Long.class)
                .value(Object.class)
                .build();

        when(ehCacheManager.getConfig(cacheName)).thenReturn(config);
        when(ehCacheManager.getCache(cacheName)).thenReturn(cache);

        Invalidation message = new Invalidation(cacheName, rawKey, Invalidation.Operation.REMOVE);
        ReflectionTestUtils.invokeMethod(service, "handle", message);

        verify(cache).remove(rawKey);
    }

    @SuppressWarnings("unchecked")
    private Cache<Object, Object> mockCache() {
        return mock(Cache.class);
    }
}
