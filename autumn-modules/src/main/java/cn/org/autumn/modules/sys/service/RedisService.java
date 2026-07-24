package cn.org.autumn.modules.sys.service;

import cn.org.autumn.utils.RedisExpireUtil;
import com.alibaba.fastjson2.JSON;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis 运维管理。禁止使用阻塞型 {@code KEYS} 全量扫描：键多时会打满 Redis 与应用 CPU，
 * 拖垮同实例上的会话/缓存等所有 Redis 访问。列表与按模式删除一律走 {@code SCAN}，并设扫描上限。
 */
@Slf4j
@Service
public class RedisService {

    /** 单次 SCAN 游标建议数量（提示值，非硬上限） */
    private static final int SCAN_COUNT_HINT = 200;

    /** 列表扫描安全上限：低于此值时仍「全量收集 + 排序 + 分页」，与旧 KEYS 流程一致 */
    private static final int MAX_SCAN_MATCHES = 5_000;

    /** 按模式删除单批大小（SCAN 分批删，不设总上限，语义与旧版一致） */
    private static final int DELETE_BATCH_SIZE = 100;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    private boolean redisReady() {
        return redisTemplate != null && stringRedisTemplate != null;
    }

    /**
     * 获取当前数据库摘要。不支持库切换；大小取 INFO used_memory，禁止全键估值。
     */
    public List<Map<String, Object>> getDatabases() {
        if (!redisReady()) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> databases = new ArrayList<>();
        Map<String, Object> dbInfo = new HashMap<>();
        dbInfo.put("database", 0);
        dbInfo.put("name", "当前数据库");
        dbInfo.put("keyCount", getKeyCount());
        dbInfo.put("size", getUsedMemoryBytes());
        databases.add(dbInfo);
        return databases;
    }

    /**
     * 分页列出键。优先复刻旧行为（收集匹配键 → 排序 → 分页）；键过多时 SCAN 截断并标记 truncated。
     */
    public Map<String, Object> getKeys(int database, String pattern, int page, int size) {
        Map<String, Object> result = new HashMap<>();
        if (!redisReady()) {
            result.put("total", 0);
            result.put("keys", new ArrayList<>());
            result.put("page", page);
            result.put("size", size);
            result.put("truncated", false);
            return result;
        }
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        String match = normalizePattern(pattern);

        List<String> pageKeys = new ArrayList<>();
        int[] meta = scanForPage(match, safePage, safeSize, pageKeys);

        List<Map<String, Object>> keyDetails = new ArrayList<>();
        for (String key : pageKeys) {
            keyDetails.add(getKeyInfo(key));
        }

        result.put("total", meta[0]);
        result.put("truncated", meta[1] == 1);
        result.put("keys", keyDetails);
        result.put("page", safePage);
        result.put("size", safeSize);
        return result;
    }

    /**
     * @return int[0]=total, int[1]=truncated(1/0)
     */
    private int[] scanForPage(String match, int page, int size, List<String> pageKeys) {
        // pattern=* 且键数不大：与旧版 KEYS* + sort + page 一致
        if ("*".equals(match)) {
            long dbSize = getKeyCount();
            if (dbSize <= MAX_SCAN_MATCHES) {
                List<String> all = scanCollect(match, MAX_SCAN_MATCHES + 1);
                boolean truncated = all.size() > MAX_SCAN_MATCHES;
                if (!truncated) {
                    Collections.sort(all);
                    fillPage(all, page, size, pageKeys);
                    return new int[]{(int) Math.min(Math.max(dbSize, all.size()), Integer.MAX_VALUE), 0};
                }
            }
            // 大键空间：仅 SCAN 填当前页，total 用 DBSIZE（分页可用，顺序不再保证字典序）
            fillPageBySkipScan(match, page, size, pageKeys);
            return new int[]{(int) Math.min(getKeyCount(), Integer.MAX_VALUE), 1};
        }

        // 指定 pattern：尽量收集全量后排序分页；触顶则截断
        List<String> all = scanCollect(match, MAX_SCAN_MATCHES + 1);
        boolean truncated = all.size() > MAX_SCAN_MATCHES;
        if (truncated) {
            all = new ArrayList<>(all.subList(0, MAX_SCAN_MATCHES));
        }
        Collections.sort(all);
        fillPage(all, page, size, pageKeys);
        return new int[]{all.size(), truncated ? 1 : 0};
    }

    private List<String> scanCollect(String match, int limit) {
        List<String> keys = new ArrayList<>();
        ScanOptions options = ScanOptions.scanOptions().match(match).count(SCAN_COUNT_HINT).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext() && keys.size() < limit) {
                keys.add(cursor.next());
            }
        } catch (Exception e) {
            log.warn("Redis SCAN failed, pattern={}, cause={}", match, e.getMessage());
        }
        return keys;
    }

    private void fillPage(List<String> sortedKeys, int page, int size, List<String> pageKeys) {
        int skip = (page - 1) * size;
        if (skip >= sortedKeys.size()) {
            return;
        }
        int end = Math.min(skip + size, sortedKeys.size());
        pageKeys.addAll(sortedKeys.subList(skip, end));
    }

    private void fillPageBySkipScan(String match, int page, int size, List<String> pageKeys) {
        int skip = (page - 1) * size;
        int index = 0;
        ScanOptions options = ScanOptions.scanOptions().match(match).count(SCAN_COUNT_HINT).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                if (index >= skip && pageKeys.size() < size) {
                    pageKeys.add(key);
                }
                index++;
                if (pageKeys.size() >= size) {
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("Redis SCAN page failed, pattern={}, cause={}", match, e.getMessage());
        }
    }

    public Map<String, Object> getKeyInfo(String key) {
        if (!redisReady()) {
            Map<String, Object> keyInfo = new HashMap<>();
            keyInfo.put("key", key);
            keyInfo.put("error", "Redis 未启用");
            return keyInfo;
        }
        Map<String, Object> keyInfo = new HashMap<>();
        keyInfo.put("key", key);
        try {
            String type = redisTemplate.type(key).name();
            keyInfo.put("type", type);
            keyInfo.put("ttl", redisTemplate.getExpire(key));
            keyInfo.put("size", getKeySize(key, type));
        } catch (Exception e) {
            keyInfo.put("type", "UNKNOWN");
            keyInfo.put("ttl", null);
            keyInfo.put("size", 0);
            keyInfo.put("error", e.getMessage());
        }
        return keyInfo;
    }

    public boolean deleteKey(String key) {
        if (!redisReady()) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redisTemplate.delete(key));
        } catch (Exception e) {
            log.warn("删除键失败: {}", e.getMessage());
            return false;
        }
    }

    public long deleteKeys(List<String> keys) {
        if (!redisReady() || keys == null || keys.isEmpty()) {
            return 0;
        }
        try {
            Long n = redisTemplate.delete(keys);
            return n != null ? n : 0;
        } catch (Exception e) {
            log.warn("删除多个键失败: {}", e.getMessage());
            return 0;
        }
    }

    public boolean clearDatabase(int database) {
        if (!redisReady()) {
            return false;
        }
        try {
            withConnection(conn -> {
                conn.serverCommands().flushDb();
                return null;
            });
            return true;
        } catch (Exception e) {
            log.warn("清空数据库失败: {}", e.getMessage());
            return false;
        }
    }

    public boolean clearAllDatabases() {
        if (!redisReady()) {
            return false;
        }
        try {
            withConnection(conn -> {
                conn.serverCommands().flushAll();
                return null;
            });
            return true;
        } catch (Exception e) {
            log.warn("清空所有数据库失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取键的值，兼容 JDK 序列化、字符串、JSON 等多种类型。
     */
    public Object getKeyValue(String key) {
        if (!redisReady()) {
            return "Redis 未启用";
        }
        try {
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

    private Object getStringValue(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                if (value instanceof String || value instanceof Number || value instanceof Boolean) {
                    return value;
                }
                if (value instanceof byte[]) {
                    String str = new String((byte[]) value, StandardCharsets.UTF_8);
                    if ((str.startsWith("{") && str.endsWith("}")) || (str.startsWith("[") && str.endsWith("]"))) {
                        try {
                            return JSON.parse(str);
                        } catch (Exception ignore) {
                        }
                    }
                    return str;
                }
                return value;
            }
        } catch (Exception e) {
            // JDK 反序列化失败则尝试字符串读取
        }
        try {
            String str = stringRedisTemplate.opsForValue().get(key);
            if (str == null) {
                return null;
            }
            if ((str.startsWith("{") && str.endsWith("}")) || (str.startsWith("[") && str.endsWith("]"))) {
                try {
                    return JSON.parse(str);
                } catch (Exception ignore) {
                }
            }
            return str;
        } catch (Exception ex) {
            return "无法识别的字符串数据格式";
        }
    }

    private Object getHashValue(String key) {
        try {
            Map<Object, Object> hash = redisTemplate.opsForHash().entries(key);
            if (hash == null || hash.isEmpty()) {
                return new HashMap<>();
            }
            Map<String, Object> result = new HashMap<>();
            for (Map.Entry<Object, Object> entry : hash.entrySet()) {
                String fieldKey = String.valueOf(entry.getKey());
                Object fieldValue = entry.getValue();
                if (fieldValue instanceof byte[]) {
                    String str = new String((byte[]) fieldValue, StandardCharsets.UTF_8);
                    result.put(fieldKey, tryParseJson(str));
                } else {
                    result.put(fieldKey, fieldValue);
                }
            }
            return result;
        } catch (Exception e) {
            try {
                Map<Object, Object> hash = stringRedisTemplate.opsForHash().entries(key);
                if (hash == null || hash.isEmpty()) {
                    return new HashMap<>();
                }
                Map<String, Object> result = new HashMap<>();
                for (Map.Entry<Object, Object> entry : hash.entrySet()) {
                    result.put(String.valueOf(entry.getKey()), tryParseJson(String.valueOf(entry.getValue())));
                }
                return result;
            } catch (Exception ex) {
                return "获取Hash值失败: " + e.getMessage();
            }
        }
    }

    private Object getListValue(String key) {
        try {
            List<Object> list = redisTemplate.opsForList().range(key, 0, -1);
            if (list == null || list.isEmpty()) {
                return new ArrayList<>();
            }
            List<Object> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof byte[]) {
                    result.add(tryParseJson(new String((byte[]) item, StandardCharsets.UTF_8)));
                } else {
                    result.add(item);
                }
            }
            return result;
        } catch (Exception e) {
            try {
                List<String> list = stringRedisTemplate.opsForList().range(key, 0, -1);
                if (list == null || list.isEmpty()) {
                    return new ArrayList<>();
                }
                List<Object> result = new ArrayList<>();
                for (String item : list) {
                    result.add(tryParseJson(item));
                }
                return result;
            } catch (Exception ex) {
                return "获取List值失败: " + e.getMessage();
            }
        }
    }

    private Object getSetValue(String key) {
        try {
            Set<Object> set = redisTemplate.opsForSet().members(key);
            if (set == null || set.isEmpty()) {
                return new HashSet<>();
            }
            Set<Object> result = new HashSet<>();
            for (Object item : set) {
                if (item instanceof byte[]) {
                    result.add(tryParseJson(new String((byte[]) item, StandardCharsets.UTF_8)));
                } else {
                    result.add(item);
                }
            }
            return result;
        } catch (Exception e) {
            try {
                Set<String> set = stringRedisTemplate.opsForSet().members(key);
                if (set == null || set.isEmpty()) {
                    return new HashSet<>();
                }
                Set<Object> result = new HashSet<>();
                for (String item : set) {
                    result.add(tryParseJson(item));
                }
                return result;
            } catch (Exception ex) {
                return "获取Set值失败: " + e.getMessage();
            }
        }
    }

    private Object getZSetValue(String key) {
        try {
            Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object>> zset =
                    redisTemplate.opsForZSet().rangeWithScores(key, 0, -1);
            if (zset == null || zset.isEmpty()) {
                return new ArrayList<>();
            }
            List<Map<String, Object>> result = new ArrayList<>();
            for (org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object> tuple : zset) {
                Map<String, Object> tupleMap = new HashMap<>();
                Object value = tuple.getValue();
                if (value instanceof byte[]) {
                    tupleMap.put("value", tryParseJson(new String((byte[]) value, StandardCharsets.UTF_8)));
                } else {
                    tupleMap.put("value", value);
                }
                tupleMap.put("score", tuple.getScore());
                result.add(tupleMap);
            }
            return result;
        } catch (Exception e) {
            try {
                Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<String>> zset =
                        stringRedisTemplate.opsForZSet().rangeWithScores(key, 0, -1);
                if (zset == null || zset.isEmpty()) {
                    return new ArrayList<>();
                }
                List<Map<String, Object>> result = new ArrayList<>();
                for (org.springframework.data.redis.core.ZSetOperations.TypedTuple<String> tuple : zset) {
                    Map<String, Object> tupleMap = new HashMap<>();
                    tupleMap.put("value", tryParseJson(tuple.getValue()));
                    tupleMap.put("score", tuple.getScore());
                    result.add(tupleMap);
                }
                return result;
            } catch (Exception ex) {
                return "获取ZSet值失败: " + e.getMessage();
            }
        }
    }

    private static Object tryParseJson(String str) {
        if (str == null) {
            return null;
        }
        if ((str.startsWith("{") && str.endsWith("}")) || (str.startsWith("[") && str.endsWith("]"))) {
            try {
                return JSON.parse(str);
            } catch (Exception ignore) {
            }
        }
        return str;
    }

    public boolean setKeyExpire(String key, long seconds) {
        if (!redisReady()) {
            return false;
        }
        try {
            return RedisExpireUtil.expire(redisTemplate, key, seconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("设置过期时间失败: {}", e.getMessage());
            return false;
        }
    }

    private long getKeyCount() {
        try {
            Long n = withConnection(conn -> conn.serverCommands().dbSize());
            return n != null ? n : 0;
        } catch (Exception e) {
            log.warn("获取键数量失败: {}", e.getMessage());
            return 0;
        }
    }

    /** 使用 INFO used_memory，禁止对全库 KEYS + GET 估值。 */
    private long getUsedMemoryBytes() {
        try {
            Properties properties = withConnection(conn -> conn.serverCommands().info("memory"));
            if (properties == null) {
                return 0;
            }
            String used = properties.getProperty("used_memory");
            if (used == null || used.isEmpty()) {
                return 0;
            }
            return Long.parseLong(used.trim());
        } catch (Exception e) {
            log.warn("获取 used_memory 失败: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 键大小：STRING 用 STRLEN，避免 GET 反序列化大对象（如 Shiro Session）打满 CPU。
     */
    private long getKeySize(String key, String type) {
        try {
            switch (type) {
                case "STRING":
                    Long len = withConnection(conn -> conn.stringCommands().strLen(keyBytes(key)));
                    return len != null ? len : 0;
                case "HASH":
                    Long h = redisTemplate.opsForHash().size(key);
                    return h != null ? h : 0;
                case "LIST":
                    Long l = redisTemplate.opsForList().size(key);
                    return l != null ? l : 0;
                case "SET":
                    Long s = redisTemplate.opsForSet().size(key);
                    return s != null ? s : 0;
                case "ZSET":
                    Long z = redisTemplate.opsForZSet().size(key);
                    return z != null ? z : 0;
                default:
                    return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 按模式删除：SCAN 分批删除，删尽匹配键（与旧版语义一致）；禁止 pattern=*。
     */
    public long deleteKeysByPattern(String pattern, int database) {
        if (!redisReady()) {
            return 0;
        }
        String match = normalizePattern(pattern);
        if ("*".equals(match)) {
            log.warn("拒绝按 * 删除全部键");
            return 0;
        }
        long deleted = 0;
        List<String> batch = new ArrayList<>(DELETE_BATCH_SIZE);
        ScanOptions options = ScanOptions.scanOptions().match(match).count(SCAN_COUNT_HINT).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                batch.add(cursor.next());
                if (batch.size() >= DELETE_BATCH_SIZE) {
                    deleted += deleteKeys(batch);
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                deleted += deleteKeys(batch);
            }
        } catch (Exception e) {
            log.warn("按模式删除失败: {}", e.getMessage());
        }
        return deleted;
    }

    public boolean isConnected() {
        if (!redisReady()) {
            return false;
        }
        try {
            String pong = withConnection(conn -> conn.ping());
            return "PONG".equalsIgnoreCase(pong);
        } catch (Exception e) {
            return false;
        }
    }

    public Map<String, Object> getServerInfo() {
        Map<String, Object> info = new HashMap<>();
        if (!redisReady()) {
            info.put("connected", false);
            info.put("error", "Redis 未启用");
            return info;
        }
        try {
            Properties properties = withConnection(conn -> conn.serverCommands().info());
            if (properties != null) {
                info.put("version", properties.getProperty("redis_version"));
                info.put("uptime", properties.getProperty("uptime_in_seconds"));
                info.put("connected_clients", properties.getProperty("connected_clients"));
                info.put("used_memory", properties.getProperty("used_memory_human"));
            }
            info.put("connected", true);
            info.put("note", "当前使用连接池模式，不支持数据库切换；列表接口使用 SCAN 且有扫描上限");
        } catch (Exception e) {
            info.put("connected", false);
            info.put("error", e.getMessage());
        }
        return info;
    }

    private static String normalizePattern(String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            return "*";
        }
        return pattern.trim();
    }

    private byte[] keyBytes(String key) {
        return key.getBytes(StandardCharsets.UTF_8);
    }

    private <T> T withConnection(Function<RedisConnection, T> action) {
        RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
        if (factory == null) {
            throw new IllegalStateException("RedisConnectionFactory 不可用");
        }
        try (RedisConnection conn = factory.getConnection()) {
            return action.apply(conn);
        }
    }
}
