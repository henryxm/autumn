package cn.org.autumn.modules.opl.service;

import cn.org.autumn.modules.opl.dao.OpenUnionDao;
import cn.org.autumn.modules.opl.entity.OpenUnionEntity;
import cn.org.autumn.service.DistributedLockService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

public class OpenUnionServiceTest {

    @InjectMocks
    private OpenUnionService openUnionService;

    @Mock
    private OpenUnionDao openUnionDao;

    @Mock
    private OpenAccountService openAccountService;

    @Mock
    private DistributedLockService distributedLockService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(openUnionService, "baseMapper", openUnionDao);
        Mockito.when(distributedLockService.withLockUnchecked(Mockito.any(String.class), Mockito.any(java.util.concurrent.Callable.class)))
                .thenAnswer(invocation -> {
                    java.util.concurrent.Callable<?> callable = (java.util.concurrent.Callable<?>) invocation.getArguments()[1];
                    return callable.call();
                });
    }

    @Test
    public void getOrCreateReturnsExistingUnionId() {
        OpenUnionEntity stored = new OpenUnionEntity();
        stored.setUnionId("u_existing");
        Mockito.when(openUnionDao.getByAccountAndUser("acc1", "user1")).thenReturn(stored);

        String unionId = openUnionService.getOrCreate("acc1", "user1");

        Assert.assertEquals("u_existing", unionId);
        Mockito.verify(openUnionDao, Mockito.never()).insert(Mockito.any(OpenUnionEntity.class));
    }

    @Test
    public void getOrCreateInsertsNewUnionRow() {
        Mockito.when(openUnionDao.getByAccountAndUser("acc1", "user1")).thenReturn(null);

        String unionId = openUnionService.getOrCreate("acc1", "user1");

        Assert.assertTrue(unionId.startsWith("u_"));
        Mockito.verify(openUnionDao).insert(Mockito.any(OpenUnionEntity.class));
    }
}
