package cn.org.autumn.modules.sys.service;

import cn.org.autumn.utils.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.alibaba.fastjson.JSON;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class RedisService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisUtils redisUtils;

    /**
     * 获取所有数据库信息
     * 注意：由于使用连接池，不支持数据库切换，只返回当前数据库信息
     */
    public List<Map<String, Object>> getDatabases() {
        List<Map<String, Object>> databases = new ArrayList<>();
        
        // 由于使用连接池，不支持数据库切换，只返回当前数据库信息
        Map<String, Object> dbInfo = new HashMap<>();
        dbInfo.put("database", 0);
        dbInfo.put("name", "当前数据库");
        dbInfo.put("keyCount", getKeyCount());
        dbInfo.put("size", getDatabaseSize());
        databases.add(dbInfo);
        
        return databases;
    }

    /**
     * 获取当前数据库的键列表
     */
    public Map<String, Object> getKeys(int database, String pattern, int page, int size) {
        Map<String, Object> result = new HashMap<>();
        
        // 由于使用连接池，不支持数据库切换，忽略database参数
        Set<String> keys;
        if (pattern == null || pattern.trim().isEmpty()) {
            keys = redisTemplate.keys("*");
        } else {
            keys = redisTemplate.keys(pattern);
        }
        
        if (keys == null) {
            keys = new HashSet<>();
        }
        
        List<String> keyList = new ArrayList<>(keys);
        Collections.sort(keyList);
        
        // 分页
        int total = keyList.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);
        
        List<String> pageKeys = keyList.subList(start, end);
        
        // 获取键的详细信息
        List<Map<String, Object>> keyDetails = new ArrayList<>();
        for (String key : pageKeys) {
            Map<String, Object> keyInfo = getKeyInfo(key);
            keyDetails.add(keyInfo);
        }
        
        result.put("total", total);
        result.put("keys", keyDetails);
        result.put("page", page);
        result.put("size", size);
        
        return result;
    }

    /**
     * 获取键的详细信息
     */
    public Map<String, Object> getKeyInfo(String key) {
        Map<String, Object> keyInfo = new HashMap<>();
        keyInfo.put("key", key);
        
        try {
            // 获取键的类型
            String type = redisTemplate.type(key).name();
            keyInfo.put("type", type);
            
            // 获取过期时间
            Long ttl = redisTemplate.getExpire(key);
            keyInfo.put("ttl", ttl);
            
            // 获取值的大小
            long size = getKeySize(key, type);
            keyInfo.put("size", size);
        } catch (Exception e) {
            keyInfo.put("type", "UNKNOWN");
            keyInfo.put("ttl", null);
            keyInfo.put("size", 0);
            keyInfo.put("error", e.getMessage());
        }
        
        return keyInfo;
    }

    /**
     * 删除指定的键
     */
    public boolean deleteKey(String key) {
        try {
            return redisTemplate.delete(key);
        } catch (Exception e) {
            System.err.println("删除键失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 删除多个键
     */
    public long deleteKeys(List<String> keys) {
        try {
            return redisTemplate.delete(keys);
        } catch (Exception e) {
            System.err.println("删除多个键失败: " + e.getMessage());
            return 0;
        }
    }

    /**
     * 清空当前数据库
     */
    public boolean clearDatabase(int database) {
        try {
            // 由于使用连接池，不支持数据库切换，忽略database参数
            redisTemplate.getConnectionFactory().getConnection().flushDb();
            return true;
        } catch (Exception e) {
            System.err.println("清空数据库失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 清空所有数据库
     */
    public boolean clearAllDatabases() {
        try {
            redisTemplate.getConnectionFactory().getConnection().flushAll();
            return true;
        } catch (Exception e) {
            System.err.println("清空所有数据库失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取键的值，兼容JDK序列化、字符串、JSON等多种类型，支持所有Redis数据类型
     */
    public Object getKeyValue(String key) {
        try {
            // 获取键的类型
            String type = redisTemplate.type(key).name();
            
            switch (type) {
                case "STRING":
                    return getStringValue(key);
                case "HASH":
                    return getHashValue(key);
                case "LIST":
                    return getListValue(key);
                case "SET":
                    return getSetValue(key);
                case "ZSET":
                    return getZSetValue(key);
                default:
                    return "未知的数据类型: " + type;
            }
        } catch (Exception e) {
            return "获取键值失败: " + e.getMessage();
        }
    }

    /**
     * 获取字符串类型的值
     */
    private Object getStringValue(String key) {
        // 1. 先尝试JDK反序列化
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                // 如果是常见类型，直接返回
                if (value instanceof String || value instanceof Number || value instanceof Boolean) {
                    return value;
                }
                // 如果是字节数组，尝试转字符串
                if (value instanceof byte[]) {
                    String str = new String((byte[]) value);
                    // 尝试解析为JSON
                    if ((str.startsWith("{") && str.endsWith("}")) || (str.startsWith("[") && str.endsWith("]"))) {
                        try {
                            return JSON.parse(str);
                        } catch (Exception ignore) {}
                    }
                    return str;
                }
                // 其他对象直接返回
                return value;
            }
        } catch (Exception e) {
            // JDK反序列化失败，继续尝试字符串读取
        }
        // 2. 尝试用StringRedisTemplate读取
        try {
            String str = stringRedisTemplate.opsForValue().get(key);
            if (str == null) return null;
            // 尝试解析为JSON对象
            if ((str.startsWith("{") && str.endsWith("}")) || (str.startsWith("[") && str.endsWith("]"))) {
                try {
                    return JSON.parse(str);
                } catch (Exception ignore) {}
            }
            return str;
        } catch (Exception ex) {
            return "无法识别的字符串数据格式";
        }
    }

    /**
     * 获取Hash类型的值
     */
    private Object getHashValue(String key) {
        try {
            Map<Object, Object> hash = redisTemplate.opsForHash().entries(key);
            if (hash == null || hash.isEmpty()) {
                return new HashMap<>();
            }
            
            // 尝试转换为字符串键值对
            Map<String, Object> result = new HashMap<>();
            for (Map.Entry<Object, Object> entry : hash.entrySet()) {
                String fieldKey = String.valueOf(entry.getKey());
                Object fieldValue = entry.getValue();
                
                // 如果值是字节数组，尝试转换为字符串
                if (fieldValue instanceof byte[]) {
                    String str = new String((byte[]) fieldValue);
                    // 尝试解析为JSON
                    if ((str.startsWith("{") && str.endsWith("}")) || (str.startsWith("[") && str.endsWith("]"))) {
                        try {
                            result.put(fieldKey, JSON.parse(str));
                        } catch (Exception ignore) {
                            result.put(fieldKey, str);
                        }
                    } else {
                        result.put(fieldKey, str);
                    }
                } else {
                    result.put(fieldKey, fieldValue);
                }
            }
            return result;
        } catch (Exception e) {
            // 如果RedisTemplate读取失败，尝试用StringRedisTemplate读取
            try {
                Map<Object, Object> hash = stringRedisTemplate.opsForHash().entries(key);
                if (hash == null || hash.isEmpty()) {
                    return new HashMap<>();
                }
                
                Map<String, Object> result = new HashMap<>();
                for (Map.Entry<Object, Object> entry : hash.entrySet()) {
                    String fieldKey = String.valueOf(entry.getKey());
                    String fieldValue = String.valueOf(entry.getValue());
                    
                    // 尝试解析为JSON
                    if ((fieldValue.startsWith("{") && fieldValue.endsWith("}")) || 
                        (fieldValue.startsWith("[") && fieldValue.endsWith("]"))) {
                        try {
                            result.put(fieldKey, JSON.parse(fieldValue));
                        } catch (Exception ignore) {
                            result.put(fieldKey, fieldValue);
                        }
                    } else {
                        result.put(fieldKey, fieldValue);
                    }
                }
                return result;
            } catch (Exception ex) {
                return "获取Hash值失败: " + e.getMessage();
            }
        }
    }

    /**
     * 获取List类型的值
     */
    private Object getListValue(String key) {
        try {
            List<Object> list = redisTemplate.opsForList().range(key, 0, -1);
            if (list == null || list.isEmpty()) {
                return new ArrayList<>();
            }
            
            // 处理列表中的每个元素
            List<Object> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof byte[]) {
                    String str = new String((byte[]) item);
                    // 尝试解析为JSON
                    if ((str.startsWith("{") && str.endsWith("}")) || (str.startsWith("[") && str.endsWith("]"))) {
                        try {
                            result.add(JSON.parse(str));
                        } catch (Exception ignore) {
                            result.add(str);
                        }
                    } else {
                        result.add(str);
                    }
                } else {
                    result.add(item);
                }
            }
            return result;
        } catch (Exception e) {
            // 如果RedisTemplate读取失败，尝试用StringRedisTemplate读取
            try {
                List<String> list = stringRedisTemplate.opsForList().range(key, 0, -1);
                if (list == null || list.isEmpty()) {
                    return new ArrayList<>();
                }
                
                List<Object> result = new ArrayList<>();
                for (String item : list) {
                    // 尝试解析为JSON
                    if ((item.startsWith("{") && item.endsWith("}")) || 
                        (item.startsWith("[") && item.endsWith("]"))) {
                        try {
                            result.add(JSON.parse(item));
                        } catch (Exception ignore) {
                            result.add(item);
                        }
                    } else {
                        result.add(item);
                    }
                }
                return result;
            } catch (Exception ex) {
                return "获取List值失败: " + e.getMessage();
            }
        }
    }

    /**
     * 获取Set类型的值
     */
    private Object getSetValue(String key) {
        try {
            Set<Object> set = redisTemplate.opsForSet().members(key);
            if (set == null || set.isEmpty()) {
                return new HashSet<>();
            }
            
            // 处理集合中的每个元素
            Set<Object> result = new HashSet<>();
            for (Object item : set) {
                if (item instanceof byte[]) {
                    String str = new String((byte[]) item);
                    // 尝试解析为JSON
                    if ((str.startsWith("{") && str.endsWith("}")) || (str.startsWith("[") && str.endsWith("]"))) {
                        try {
                            result.add(JSON.parse(str));
                        } catch (Exception ignore) {
                            result.add(str);
                        }
                    } else {
                        result.add(str);
                    }
                } else {
                    result.add(item);
                }
            }
            return result;
        } catch (Exception e) {
            // 如果RedisTemplate读取失败，尝试用StringRedisTemplate读取
            try {
                Set<String> set = stringRedisTemplate.opsForSet().members(key);
                if (set == null || set.isEmpty()) {
                    return new HashSet<>();
                }
                
                Set<Object> result = new HashSet<>();
                for (String item : set) {
                    // 尝试解析为JSON
                    if ((item.startsWith("{") && item.endsWith("}")) || 
                        (item.startsWith("[") && item.endsWith("]"))) {
                        try {
                            result.add(JSON.parse(item));
                        } catch (Exception ignore) {
                            result.add(item);
                        }
                    } else {
                        result.add(item);
                    }
                }
                return result;
            } catch (Exception ex) {
                return "获取Set值失败: " + e.getMessage();
            }
        }
    }

    /**
     * 获取ZSet类型的值
     */
    private Object getZSetValue(String key) {
        try {
            Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object>> zset = 
                redisTemplate.opsForZSet().rangeWithScores(key, 0, -1);
            if (zset == null || zset.isEmpty()) {
                return new ArrayList<>();
            }
            
            // 处理有序集合中的每个元素
            List<Map<String, Object>> result = new ArrayList<>();
            for (org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object> tuple : zset) {
                Map<String, Object> tupleMap = new HashMap<>();
                Object value = tuple.getValue();
                Double score = tuple.getScore();
                
                // 处理值
                if (value instanceof byte[]) {
                    String str = new String((byte[]) value);
                    // 尝试解析为JSON
                    if ((str.startsWith("{") && str.endsWith("}")) || (str.startsWith("[") && str.endsWith("]"))) {
                        try {
                            tupleMap.put("value", JSON.parse(str));
                        } catch (Exception ignore) {
                            tupleMap.put("value", str);
                        }
                    } else {
                        tupleMap.put("value", str);
                    }
                } else {
                    tupleMap.put("value", value);
                }
                
                tupleMap.put("score", score);
                result.add(tupleMap);
            }
            return result;
        } catch (Exception e) {
            // 如果RedisTemplate读取失败，尝试用StringRedisTemplate读取
            try {
                Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<String>> zset = 
                    stringRedisTemplate.opsForZSet().rangeWithScores(key, 0, -1);
                if (zset == null || zset.isEmpty()) {
                    return new ArrayList<>();
                }
                
                List<Map<String, Object>> result = new ArrayList<>();
                for (org.springframework.data.redis.core.ZSetOperations.TypedTuple<String> tuple : zset) {
                    Map<String, Object> tupleMap = new HashMap<>();
                    String value = tuple.getValue();
                    Double score = tuple.getScore();
                    
                    // 尝试解析为JSON
                    if ((value.startsWith("{") && value.endsWith("}")) || 
                        (value.startsWith("[") && value.endsWith("]"))) {
                        try {
                            tupleMap.put("value", JSON.parse(value));
                        } catch (Exception ignore) {
                            tupleMap.put("value", value);
                        }
                    } else {
                        tupleMap.put("value", value);
                    }
                    
                    tupleMap.put("score", score);
                    result.add(tupleMap);
                }
                return result;
            } catch (Exception ex) {
                return "获取ZSet值失败: " + e.getMessage();
            }
        }
    }

    /**
     * 设置键的过期时间
     */
    public boolean setKeyExpire(String key, long seconds) {
        try {
            return redisTemplate.expire(key, seconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("设置过期时间失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取当前数据库中的键数量
     */
    private long getKeyCount() {
        try {
            return redisTemplate.getConnectionFactory().getConnection().dbSize();
        } catch (Exception e) {
            System.err.println("获取键数量失败: " + e.getMessage());
            return 0;
        }
    }

    /**
     * 获取当前数据库大小（估算）
     */
    private long getDatabaseSize() {
        try {
            Set<String> keys = redisTemplate.keys("*");
            if (keys == null) return 0;
            
            long totalSize = 0;
            for (String key : keys) {
                totalSize += getKeySize(key, redisTemplate.type(key).name());
            }
            return totalSize;
        } catch (Exception e) {
            System.err.println("获取数据库大小失败: " + e.getMessage());
            return 0;
        }
    }

    /**
     * 获取键的大小
     */
    private long getKeySize(String key, String type) {
        try {
            switch (type) {
                case "STRING":
                    Object value = redisTemplate.opsForValue().get(key);
                    return value != null ? value.toString().getBytes().length : 0;
                case "HASH":
                    return redisTemplate.opsForHash().size(key);
                case "LIST":
                    return redisTemplate.opsForList().size(key);
                case "SET":
                    return redisTemplate.opsForSet().size(key);
                case "ZSET":
                    return redisTemplate.opsForZSet().size(key);
                default:
                    return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 检查Redis连接状态
     */
    public boolean isConnected() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取Redis服务器信息
     */
    public Map<String, Object> getServerInfo() {
        Map<String, Object> info = new HashMap<>();
        try {
            Properties properties = redisTemplate.getConnectionFactory().getConnection().info();
            info.put("version", properties.getProperty("redis_version"));
            info.put("uptime", properties.getProperty("uptime_in_seconds"));
            info.put("connected_clients", properties.getProperty("connected_clients"));
            info.put("used_memory", properties.getProperty("used_memory_human"));
            info.put("connected", true);
            info.put("note", "当前使用连接池模式，不支持数据库切换");
        } catch (Exception e) {
            info.put("connected", false);
            info.put("error", e.getMessage());
        }
        return info;
    }
} 