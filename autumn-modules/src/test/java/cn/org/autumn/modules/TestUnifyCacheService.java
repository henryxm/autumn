package cn.org.autumn.modules;

import static org.junit.Assert.*;

import cn.org.autumn.modules.client.entity.WebOauthCombineEntity;
import cn.org.autumn.modules.client.service.WebOauthCombineService;
import cn.org.autumn.modules.lan.entity.LanguageEntity;
import cn.org.autumn.utils.Uuid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * ModuleService 缓存功能测试用例
 * 
 * 测试内容：
 * 1. WebOauthCombineEntity - 单一 key 缓存测试（使用 uuid 字段）
 * 2. LanguageEntity - 复合 key 缓存测试（使用 name 和 tag 字段）
 * 3. getCache 方法测试
 * 4. 删除缓存功能测试（updateById, deleteById等）
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
@Ignore("需要数据库/缓存等集成环境，默认构建环境跳过")
public class TestUnifyCacheService {

    @Autowired
    private WebOauthCombineService webOauthCombineService;

    @Autowired
    private TestLanguageService testLanguageService;

    private final Random random = new Random();

    /**
     * 生成随机字符串
     */
    private String randomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }


    /**
     * 测试 WebOauthCombineEntity 单一 key 缓存功能
     */
    @Test
    public void testWebOauthCombineEntitySingleKeyCache() {
        log.info("=== Starting WebOauthCombineEntity single-key cache test ===");
        
        // 1. 创建测试数据
        WebOauthCombineEntity entity = new WebOauthCombineEntity();
        String uuid = Uuid.uuid();
        entity.setUuid(uuid);
        entity.setClientId("test_client_" + randomString(10));
        entity.setWebAuthenticationUuid(Uuid.uuid());
        entity.setClientDetailsUuid(Uuid.uuid());
        entity.setCreateTime(new Date());
        entity.setUpdateTime(new Date());
        
        // 2. 插入数据
        boolean inserted = webOauthCombineService.insert(entity);
        assertTrue("插入数据失败", inserted);
        log.info("Insert succeeded, uuid: {}", uuid);
        
        // 3. 测试 getCache - 第一次查询（应该从数据库查询并缓存）
        long startTime1 = System.currentTimeMillis();
        WebOauthCombineEntity cached1 = webOauthCombineService.getCache(uuid);
        long time1 = System.currentTimeMillis() - startTime1;
        assertNotNull("第一次查询应该返回数据", cached1);
        assertEquals("uuid 应该匹配", uuid, cached1.getUuid());
        log.info("First query (database) took: {} ms", time1);
        
        // 4. 测试 getCache - 第二次查询（应该从缓存获取）
        long startTime2 = System.currentTimeMillis();
        WebOauthCombineEntity cached2 = webOauthCombineService.getCache(uuid);
        long time2 = System.currentTimeMillis() - startTime2;
        assertNotNull("第二次查询应该返回数据", cached2);
        assertEquals("uuid 应该匹配", uuid, cached2.getUuid());
        log.info("Second query (cache) took: {} ms", time2);
        
        // 5. 验证缓存生效（第二次查询应该更快）
        assertTrue("缓存查询应该比数据库查询快", time2 < time1 || time2 < 10);
        
        // 6. 测试更新后缓存删除
        cached2.setClientId("updated_client_" + randomString(10));
        boolean updated = webOauthCombineService.updateById(cached2);
        assertTrue("更新应该成功", updated);
        log.info("Update succeeded");
        
        // 7. 验证更新后缓存已删除（再次查询应该从数据库获取最新数据）
        WebOauthCombineEntity cached3 = webOauthCombineService.getCache(uuid);
        assertNotNull("更新后查询应该返回数据", cached3);
        assertEquals("更新后的 clientId 应该匹配", cached2.getClientId(), cached3.getClientId());
        log.info("Cache evicted after update; reloaded from database");
        
        // 8. 测试删除后缓存删除
        boolean deleted = webOauthCombineService.deleteById(cached3.getId());
        assertTrue("删除应该成功", deleted);
        log.info("Delete succeeded");
        
        // 9. 验证删除后缓存已删除
        WebOauthCombineEntity cached4 = webOauthCombineService.getCache(uuid);
        assertNull("删除后查询应该返回 null", cached4);
        log.info("Cache evicted after delete");
        
        log.info("=== WebOauthCombineEntity single-key cache test completed ===");
    }

    /**
     * 测试 LanguageEntity 复合 key 缓存功能
     */
    @Test
    public void testLanguageEntityCompositeKeyCache() {
        log.info("=== Starting LanguageEntity composite-key cache test ===");
        
        // 1. 创建测试数据
        LanguageEntity entity = new LanguageEntity();
        String name = "test_lang_" + randomString(10);
        String tag = "test_tag_" + randomString(8);
        entity.setName(name);
        entity.setTag(tag);
        entity.setZhCn("测试中文");
        entity.setEnUs("Test English");
        entity.setFix("");
        
        // 2. 插入数据
        boolean inserted = testLanguageService.insert(entity);
        assertTrue("插入数据失败", inserted);
        log.info("Insert succeeded, name: {}, tag: {}", name, tag);
        
        // 3. 测试 getCache - 第一次查询（应该从数据库查询并缓存）
        // 使用可变参数：getCache(name, tag)
        long startTime1 = System.currentTimeMillis();
        LanguageEntity cached1 = testLanguageService.getCache(name, tag);
        long time1 = System.currentTimeMillis() - startTime1;
        assertNotNull("第一次查询应该返回数据", cached1);
        assertEquals("name 应该匹配", name, cached1.getName());
        assertEquals("tag 应该匹配", tag, cached1.getTag());
        log.info("First query (database) took: {} ms", time1);
        
        // 4. 测试 getCache - 第二次查询（应该从缓存获取）
        long startTime2 = System.currentTimeMillis();
        LanguageEntity cached2 = testLanguageService.getCache(name, tag);
        long time2 = System.currentTimeMillis() - startTime2;
        assertNotNull("第二次查询应该返回数据", cached2);
        assertEquals("name 应该匹配", name, cached2.getName());
        assertEquals("tag 应该匹配", tag, cached2.getTag());
        log.info("Second query (cache) took: {} ms", time2);
        
        // 5. 验证缓存生效（第二次查询应该更快）
        assertTrue("缓存查询应该比数据库查询快", time2 < time1 || time2 < 10);
        
        // 6. 测试使用不同的参数顺序（应该返回 null，因为顺序不对）
        LanguageEntity cachedWrongOrder = testLanguageService.getCache(tag, name);
        assertNull("错误的参数顺序应该返回 null", cachedWrongOrder);
        log.info("Wrong parameter order returned null (expected)");
        
        // 7. 测试更新后缓存删除
        cached2.setZhCn("更新后的中文");
        cached2.setEnUs("Updated English");
        boolean updated = testLanguageService.updateById(cached2);
        assertTrue("更新应该成功", updated);
        log.info("Update succeeded");
        
        // 8. 验证更新后缓存已删除（再次查询应该从数据库获取最新数据）
        LanguageEntity cached3 = testLanguageService.getCache(name, tag);
        assertNotNull("更新后查询应该返回数据", cached3);
        assertEquals("更新后的 zhCn 应该匹配", "更新后的中文", cached3.getZhCn());
        assertEquals("更新后的 enUs 应该匹配", "Updated English", cached3.getEnUs());
        log.info("Cache evicted after update; reloaded from database");
        
        // 9. 测试删除后缓存删除
        boolean deleted = testLanguageService.deleteById(cached3.getId());
        assertTrue("删除应该成功", deleted);
        log.info("Delete succeeded");
        
        // 10. 验证删除后缓存已删除
        LanguageEntity cached4 = testLanguageService.getCache(name, tag);
        assertNull("删除后查询应该返回 null", cached4);
        log.info("Cache evicted after delete");
        
        log.info("=== LanguageEntity composite-key cache test completed ===");
    }

    /**
     * 测试批量操作缓存删除
     */
    @Test
    public void testBatchOperationCacheRemoval() {
        log.info("=== Starting batch operation cache eviction test ===");
        
        // 1. 创建多个测试数据
        String[] uuids = new String[3];
        WebOauthCombineEntity[] entities = new WebOauthCombineEntity[3];
        
        for (int i = 0; i < 3; i++) {
            WebOauthCombineEntity entity = new WebOauthCombineEntity();
            String uuid = Uuid.uuid();
            uuids[i] = uuid;
            entity.setUuid(uuid);
            entity.setClientId("batch_test_" + i + "_" + randomString(10));
            entity.setWebAuthenticationUuid(Uuid.uuid());
            entity.setClientDetailsUuid(Uuid.uuid());
            entity.setCreateTime(new Date());
            entity.setUpdateTime(new Date());
            entities[i] = entity;
            
            webOauthCombineService.insert(entity);
            log.info("Inserted record {}: uuid = {}", i, uuid);
        }
        
        // 2. 预热缓存
        for (String uuid : uuids) {
            webOauthCombineService.getCache(uuid);
        }
        log.info("Cache warm-up completed");
        
        // 3. 测试批量更新后缓存删除
        for (WebOauthCombineEntity entity : entities) {
            entity.setClientId("updated_batch_" + randomString(10));
        }
        boolean batchUpdated = webOauthCombineService.updateBatchById(Arrays.asList(entities));
        assertTrue("批量更新应该成功", batchUpdated);
        log.info("Batch update completed");
        
        // 4. 验证批量更新后缓存已删除
        for (int i = 0; i < uuids.length; i++) {
            WebOauthCombineEntity cached = webOauthCombineService.getCache(uuids[i]);
            assertNotNull("批量更新后查询应该返回数据", cached);
            assertEquals("更新后的 clientId 应该匹配", entities[i].getClientId(), cached.getClientId());
        }
        log.info("Cache evicted after batch update; reloaded from database");
        
        // 5. 测试批量删除后缓存删除
        List<Long> ids = new ArrayList<>();
        for (WebOauthCombineEntity entity : entities) {
            ids.add(entity.getId());
        }
        boolean batchDeleted = webOauthCombineService.deleteBatchIds(ids);
        assertTrue("批量删除应该成功", batchDeleted);
        log.info("Batch delete completed");
        
        // 6. 验证批量删除后缓存已删除
        for (String uuid : uuids) {
            WebOauthCombineEntity cached = webOauthCombineService.getCache(uuid);
            assertNull("批量删除后查询应该返回 null", cached);
        }
        log.info("Cache evicted after batch delete");
        
        log.info("=== Batch operation cache eviction test completed ===");
    }

    /**
     * 测试复合 key 缓存的不同参数组合
     */
    @Test
    public void testLanguageEntityCompositeKeyVariations() {
        log.info("=== Starting composite-key cache parameter variation test ===");
        
        // 1. 创建多个测试数据（相同的 name，不同的 tag）
        String name = "shared_name_" + randomString(10);
        String[] tags = {"tag1", "tag2", "tag3"};
        LanguageEntity[] entities = new LanguageEntity[tags.length];
        
        for (int i = 0; i < tags.length; i++) {
            LanguageEntity entity = new LanguageEntity();
            entity.setName(name);
            entity.setTag(tags[i]);
            entity.setZhCn("中文" + i);
            entity.setEnUs("English" + i);
            entity.setFix("");
            entities[i] = entity;
            
            testLanguageService.insert(entity);
            log.info("Inserted record {}: name = {}, tag = {}", i, name, tags[i]);
        }
        
        // 2. 测试不同的 tag 应该返回不同的数据
        for (int i = 0; i < tags.length; i++) {
            LanguageEntity cached = testLanguageService.getCache(name, tags[i]);
            assertNotNull("查询应该返回数据", cached);
            assertEquals("name 应该匹配", name, cached.getName());
            assertEquals("tag 应该匹配", tags[i], cached.getTag());
            assertEquals("zhCn 应该匹配", "中文" + i, cached.getZhCn());
            log.info("Query name={}, tag={} succeeded", name, tags[i]);
        }
        
        // 3. 测试不存在的组合应该返回 null
        LanguageEntity notExist = testLanguageService.getCache(name, "non_exist_tag");
        assertNull("不存在的组合应该返回 null", notExist);
        log.info("Non-existent combination returned null (expected)");
        
        // 4. 清理测试数据
        for (LanguageEntity entity : entities) {
            testLanguageService.deleteById(entity.getId());
        }
        
        log.info("=== Composite-key cache parameter variation test completed ===");
    }

    /**
     * 测试缓存 key 构建的正确性
     */
    @Test
    public void testCacheKeyBuilding() {
        log.info("=== Starting cache key construction test ===");
        
        // 1. 测试单一 key
        String uuid1 = Uuid.uuid();
        String uuid2 = Uuid.uuid();
        
        WebOauthCombineEntity entity1 = new WebOauthCombineEntity();
        entity1.setUuid(uuid1);
        entity1.setClientId("test1_" + randomString(10));
        entity1.setWebAuthenticationUuid(Uuid.uuid());
        entity1.setClientDetailsUuid(Uuid.uuid());
        entity1.setCreateTime(new Date());
        entity1.setUpdateTime(new Date());
        webOauthCombineService.insert(entity1);
        
        WebOauthCombineEntity entity2 = new WebOauthCombineEntity();
        entity2.setUuid(uuid2);
        entity2.setClientId("test2_" + randomString(10));
        entity2.setWebAuthenticationUuid(Uuid.uuid());
        entity2.setClientDetailsUuid(Uuid.uuid());
        entity2.setCreateTime(new Date());
        entity2.setUpdateTime(new Date());
        webOauthCombineService.insert(entity2);
        
        // 2. 验证不同的 key 返回不同的数据
        WebOauthCombineEntity cached1 = webOauthCombineService.getCache(uuid1);
        WebOauthCombineEntity cached2 = webOauthCombineService.getCache(uuid2);
        
        assertNotNull("uuid1 应该返回数据", cached1);
        assertNotNull("uuid2 应该返回数据", cached2);
        assertNotEquals("不同的 key 应该返回不同的数据", cached1.getUuid(), cached2.getUuid());
        assertEquals("uuid1 应该匹配", uuid1, cached1.getUuid());
        assertEquals("uuid2 应该匹配", uuid2, cached2.getUuid());
        log.debug("Single-key cache construction is correct");
        
        // 3. 测试复合 key
        String name = "key_test_" + randomString(10);
        String tag1 = "tag_a";
        String tag2 = "tag_b";
        
        LanguageEntity lang1 = new LanguageEntity();
        lang1.setName(name);
        lang1.setTag(tag1);
        lang1.setZhCn("中文A");
        lang1.setEnUs("EnglishA");
        lang1.setFix("");
        testLanguageService.insert(lang1);
        
        LanguageEntity lang2 = new LanguageEntity();
        lang2.setName(name);
        lang2.setTag(tag2);
        lang2.setZhCn("中文B");
        lang2.setEnUs("EnglishB");
        lang2.setFix("");
        testLanguageService.insert(lang2);
        
        // 4. 验证不同的复合 key 返回不同的数据
        LanguageEntity cachedLang1 = testLanguageService.getCache(name, tag1);
        LanguageEntity cachedLang2 = testLanguageService.getCache(name, tag2);
        
        assertNotNull("name+tag1 应该返回数据", cachedLang1);
        assertNotNull("name+tag2 应该返回数据", cachedLang2);
        assertEquals("name 应该相同", name, cachedLang1.getName());
        assertEquals("name 应该相同", name, cachedLang2.getName());
        assertNotEquals("tag 应该不同", cachedLang1.getTag(), cachedLang2.getTag());
        assertNotEquals("zhCn 应该不同", cachedLang1.getZhCn(), cachedLang2.getZhCn());
        log.debug("Composite-key cache construction is correct");
        
        // 5. 清理测试数据
        webOauthCombineService.deleteById(entity1.getId());
        webOauthCombineService.deleteById(entity2.getId());
        testLanguageService.deleteById(lang1.getId());
        testLanguageService.deleteById(lang2.getId());
        
        log.debug("=== Cache key construction test completed ===");
    }
}
