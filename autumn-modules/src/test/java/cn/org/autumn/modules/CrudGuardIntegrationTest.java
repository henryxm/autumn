package cn.org.autumn.modules;

import static org.junit.Assert.*;

import cn.org.autumn.database.CrudGuard;
import cn.org.autumn.exception.AException;
import cn.org.autumn.modules.lan.entity.LanguageEntity;
import cn.org.autumn.modules.sys.service.CrudGuardService;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * 全局 databaseWrite 开关与 MyBatis 拦截器、缓存失效的集成验证。
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
@Ignore("需要数据库/缓存等集成环境，默认构建环境跳过")
public class CrudGuardIntegrationTest {

    @Autowired
    private CrudGuard crudGuard;

    @Autowired
    private CrudGuardService crudGuardService;

    @Autowired
    private TestLanguageService testLanguageService;

    private final Random random = new Random();

    private boolean savedDatabaseWrite;
    private boolean savedLocalWrite;
    private String savedDescription;

    @Before
    public void rememberGuardState() {
        savedDatabaseWrite = crudGuard.global();
        savedLocalWrite = crudGuard.local();
        savedDescription = crudGuard.hint();
    }

    @After
    public void restoreGuardState() {
        crudGuard.apply(savedDatabaseWrite, savedLocalWrite, savedDescription);
        crudGuard.clear();
    }

    @Test
    public void insertBlockedWhenDatabaseWriteOff() {
        crudGuard.apply(false, true, "集成测试只读");
        LanguageEntity entity = newLanguageEntity();
        try {
            testLanguageService.insert(entity);
            fail("databaseWrite=false 时 insert 应被拦截");
        } catch (AException e) {
            assertEquals(834, e.getCode());
        }
    }

    @Test
    public void updateDoesNotInvalidateCacheWhenWriteBlocked() {
        LanguageEntity entity = newLanguageEntity();
        assertTrue("预置数据插入应成功", testLanguageService.insert(entity));

        LanguageEntity cached = testLanguageService.getCache(entity.getName(), entity.getTag());
        assertNotNull("预热缓存应命中", cached);
        String originalZhCn = cached.getZhCn();

        crudGuard.apply(false, true, "集成测试只读");
        entity.setZhCn("应被拒绝的更新");
        try {
            testLanguageService.updateById(entity);
            fail("databaseWrite=false 时 update 应被拦截");
        } catch (AException e) {
            assertEquals(834, e.getCode());
        }

        LanguageEntity stillCached = testLanguageService.getCache(entity.getName(), entity.getTag());
        assertNotNull("拦截后缓存不应被误清", stillCached);
        assertEquals("缓存内容应保持不变", originalZhCn, stillCached.getZhCn());
    }

    @Test
    public void guardServiceSnapshotMatchesRuntime() {
        crudGuard.apply(false, true, "快照测试");
        assertFalse(crudGuardService.snapshot().isDatabaseWrite());
        assertTrue(crudGuardService.snapshot().isLocalWrite());
    }

    private LanguageEntity newLanguageEntity() {
        LanguageEntity entity = new LanguageEntity();
        entity.setName("crud_guard_" + randomString(8));
        entity.setTag("t_" + randomString(6));
        entity.setZhCn("中文");
        entity.setEnUs("English");
        entity.setFix("");
        return entity;
    }

    private String randomString(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
