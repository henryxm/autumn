package cn.org.autumn.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.org.autumn.exception.AException;
import cn.org.autumn.model.DefaultEntity;
import cn.org.autumn.model.Error;
import cn.org.autumn.model.StubMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 验证写库开关由 {@link cn.org.autumn.database.CrudInterceptor} 拦截后，
 * {@link BaseCacheService} 仅在写库成功时失效缓存（不在拦截/失败时误清缓存）。
 */
public class BaseCacheServiceWriteGuardTest {

    private StubMapper mapper;
    private ProbeCacheService service;

    @Before
    public void setUp() {
        mapper = mock(StubMapper.class);
        service = new ProbeCacheService();
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
    }

    @Test
    public void updateByIdSkipsCacheInvalidationWhenMapperReturnsZero() {
        when(mapper.updateById(any(DefaultEntity.class))).thenReturn(0);
        boolean ok = service.updateById(new DefaultEntity());
        Assert.assertFalse(ok);
        Assert.assertEquals(0, service.removeCalls);
    }

    @Test
    public void updateByIdInvalidatesCacheWhenMapperSucceeds() {
        when(mapper.updateById(any(DefaultEntity.class))).thenReturn(1);
        boolean ok = service.updateById(new DefaultEntity());
        Assert.assertTrue(ok);
        Assert.assertEquals(1, service.removeCalls);
    }

    @Test
    public void updateByIdSkipsCacheInvalidationWhenMapperThrowsReadOnly() {
        when(mapper.updateById(any(DefaultEntity.class)))
                .thenThrow(new AException(Error.DATABASE_READ_ONLY));
        try {
            service.updateById(new DefaultEntity());
            Assert.fail("应抛出只读异常");
        } catch (AException e) {
            Assert.assertEquals(834, e.getCode());
        }
        Assert.assertEquals(0, service.removeCalls);
    }

    @Test
    public void insertOrUpdateSkipsCacheInvalidationWhenMapperReturnsZero() {
        when(mapper.insert(any(DefaultEntity.class))).thenReturn(0);
        boolean ok = service.insertOrUpdate(new DefaultEntity());
        Assert.assertFalse(ok);
        Assert.assertEquals(0, service.removeCalls);
    }

    @Test
    public void insertOrUpdateInvalidatesCacheWhenMapperSucceeds() {
        when(mapper.insert(any(DefaultEntity.class))).thenReturn(1);
        boolean ok = service.insertOrUpdate(new DefaultEntity());
        Assert.assertTrue(ok);
        Assert.assertEquals(1, service.removeCalls);
    }

    @Test
    public void insertOrUpdateSkipsCacheInvalidationWhenMapperThrowsReadOnly() {
        when(mapper.insert(any(DefaultEntity.class)))
                .thenThrow(new AException(Error.DATABASE_READ_ONLY));
        try {
            service.insertOrUpdate(new DefaultEntity());
            Assert.fail("应抛出只读异常");
        } catch (AException e) {
            Assert.assertEquals(834, e.getCode());
        }
        Assert.assertEquals(0, service.removeCalls);
    }

    private static final class ProbeCacheService extends BaseCacheService<StubMapper, DefaultEntity> {
        int removeCalls;

        @Override
        protected void removeCacheByEntity(DefaultEntity entity) {
            removeCalls++;
        }
    }
}
