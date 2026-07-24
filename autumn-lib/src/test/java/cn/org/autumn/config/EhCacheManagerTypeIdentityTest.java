package cn.org.autumn.config;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import cn.org.autumn.model.AesKey;
import java.util.concurrent.TimeUnit;
import org.ehcache.Cache;
import org.junit.Test;

/**
 * 回归：Null=true 时 Object 扩宽与声明具体类型不得互相拆建（aseservice 震荡）。
 */
public class EhCacheManagerTypeIdentityTest {

    @Test
    public void objectCacheServesConcreteValueTypeWithoutRecreate() {
        EhCacheManager manager = new EhCacheManager();
        CacheConfig objectConfig = CacheConfig.builder()
                .name("aseservice")
                .key(String.class)
                .value(Object.class)
                .expire(10)
                .unit(TimeUnit.MINUTES)
                .Null(true)
                .build();
        objectConfig.validate();

        Cache<String, Object> created = manager.getOrCreate(objectConfig);
        assertNotNull(created);

        Cache<String, AesKey> byConcrete = manager.getCache("aseservice", String.class, AesKey.class);
        assertNotNull(byConcrete);
        assertSame(created, byConcrete);
        assertTrue(manager.getAllInstanceNames().contains("aseservice"));
    }

    @Test
    public void concreteThenObjectRequestRecreatesOnceForNullPlaceholder() {
        EhCacheManager manager = new EhCacheManager();
        CacheConfig aesConfig = CacheConfig.builder()
                .name("aseservice-concrete")
                .key(String.class)
                .value(AesKey.class)
                .expire(10)
                .unit(TimeUnit.MINUTES)
                .Null(false)
                .build();
        aesConfig.validate();
        Cache<String, AesKey> concrete = manager.getOrCreate(aesConfig);
        assertNotNull(concrete);

        CacheConfig objectConfig = aesConfig.toBuilder().value(Object.class).Null(true).build();
        objectConfig.validate();
        Cache<String, Object> widened = manager.getOrCreate(objectConfig);
        assertNotNull(widened);
        // 请求 Object 时具体类型缓存应被替换，以便存放 NULL_PLACEHOLDER
        assertSame(Object.class, widened.getRuntimeConfiguration().getValueType());
    }

    @Test
    public void alternatingGetOrCreateWithNullDefaultDoesNotThrash() {
        EhCacheManager manager = new EhCacheManager();
        // 模拟默认 Null=true 的业务配置（声明 AesKey）
        CacheConfig declared = CacheConfig.builder()
                .name("aseservice-alt")
                .key(String.class)
                .value(AesKey.class)
                .expire(10)
                .unit(TimeUnit.MINUTES)
                .build();
        declared.validate();

        CacheConfig asObject = declared.toBuilder().value(Object.class).build();
        asObject.validate();

        Cache<?, ?> first = manager.getOrCreate(asObject);
        for (int i = 0; i < 20; i++) {
            Cache<?, ?> viaObject = manager.getCache("aseservice-alt", String.class, Object.class);
            Cache<?, ?> viaAes = manager.getCache("aseservice-alt", String.class, AesKey.class);
            assertNotNull(viaObject);
            assertNotNull(viaAes);
            assertSame(first, viaObject);
            assertSame(first, viaAes);
        }
    }
}
