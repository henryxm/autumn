package cn.org.autumn.modules;

import cn.org.autumn.modules.sys.service.RedisService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class RedisServiceTest {

    @Autowired
    private RedisService redisService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    public void testGetDatabases() {
        System.out.println("=== 测试获取数据库列表 ===");
        List<Map<String, Object>> databases = redisService.getDatabases();
        for (Map<String, Object> db : databases) {
            System.out.println("数据库: " + db.get("name") + 
                             ", 键数量: " + db.get("keyCount") + 
                             ", 大小: " + db.get("size"));
        }
    }

    @Test
    public void testGetKeys() {
        System.out.println("=== 测试获取键列表 ===");
        Map<String, Object> keysData = redisService.getKeys(0, "*", 1, 10);
        System.out.println("总键数: " + keysData.get("total"));
        List<Map<String, Object>> keys = (List<Map<String, Object>>) keysData.get("keys");
        for (Map<String, Object> key : keys) {
            System.out.println("键: " + key.get("key") + 
                             ", 类型: " + key.get("type") + 
                             ", 大小: " + key.get("size"));
        }
    }

    @Test
    public void testConnectionStatus() {
        System.out.println("=== 测试连接状态 ===");
        boolean connected = redisService.isConnected();
        System.out.println("Redis连接状态: " + connected);
        
        Map<String, Object> serverInfo = redisService.getServerInfo();
        System.out.println("服务器信息: " + serverInfo);
    }

    @Test
    public void testSetAndGetKey() {
        System.out.println("=== 测试设置和获取键 ===");
        String testKey = "test:key:123";
        String testValue = "Hello Redis!";
        
        // 设置键值
        redisTemplate.opsForValue().set(testKey, testValue);
        System.out.println("设置键: " + testKey + " = " + testValue);
        
        // 获取键值
        Object value = redisService.getKeyValue(testKey);
        System.out.println("获取键值: " + value);
        
        // 获取键信息
        Map<String, Object> keyInfo = redisService.getKeyInfo(testKey);
        System.out.println("键信息: " + keyInfo);
        
        // 清理测试数据
        redisService.deleteKey(testKey);
        System.out.println("删除测试键: " + testKey);
    }
} 