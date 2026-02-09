package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.annotation.Endpoint;
import cn.org.autumn.annotation.SkipInterceptor;
import cn.org.autumn.config.CacheConfig;
import cn.org.autumn.config.EhCacheManager;
import cn.org.autumn.model.Response;
import cn.org.autumn.modules.sys.service.SysUserRoleService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.service.CacheService;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.ehcache.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 缓存管理控制器
 * 提供缓存管理的API接口
 *
 * @author Autumn
 */
@Slf4j
@RestController
@RequestMapping("/sys/cache")
@Endpoint(hidden = true)
@SkipInterceptor
public class CacheController {

    @Autowired
    private CacheService cacheService;

    @Autowired
    private EhCacheManager ehCacheManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private SysUserRoleService sysUserRoleService;

    @Autowired
    private Gson gson;

    /**
     * 检查权限
     */
    private boolean checkPermission() {
        return ShiroUtils.isLogin() && sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid());
    }

    /**
     * 获取本地缓存中的所有键（字符串形式）
     */
    private Set<String> getLocalCacheKeys(String cacheName) {
        Set<String> localKeys = new LinkedHashSet<>();
        try {
            Cache<?, ?> cache = ehCacheManager.getCache(cacheName);
            if (cache != null) {
                for (Cache.Entry<?, ?> entry : cache) {
                    if (entry.getKey() != null) {
                        localKeys.add(String.valueOf(entry.getKey()));
                    }
                }
            }
        } catch (Exception e) {
            log.debug("遍历本地缓存失败: cacheName={}, error={}", cacheName, e.getMessage());
        }
        return localKeys;
    }

    /**
     * 将通配符模式转换为正则表达式进行匹配
     */
    private boolean matchWildcard(String text, String pattern) {
        if (pattern == null || pattern.isEmpty() || "*".equals(pattern)) {
            return true;
        }
        String regex = "^" + pattern.replace(".", "\\.").replace("*", ".*").replace("?", ".") + "$";
        try {
            return text.matches(regex);
        } catch (Exception e) {
            return text.contains(pattern.replace("*", ""));
        }
    }

    /**
     * 将配置的过期时间转换为秒
     */
    private long convertToSeconds(long time, TimeUnit unit) {
        if (unit == null) {
            unit = TimeUnit.MINUTES;
        }
        return unit.toSeconds(time);
    }

    /**
     * 构建合并的键信息列表
     */
    private Map<String, Object> buildMergedKeysResult(String cacheName, Set<String> localKeys, Set<String> filteredLocalKeys,
                                                       Map<String, Long> redisKeyTTLMap, int page, int size) {
        Map<String, Object> result = new HashMap<>();
        CacheConfig config = ehCacheManager.getConfig(cacheName);
        long localExpireSeconds = config != null ? convertToSeconds(config.getExpire(), config.getUnit()) : -1;
        long redisExpireSeconds = config != null ? convertToSeconds(config.getRedis(), config.getUnit()) : -1;

        // 合并所有键
        Set<String> allKeys = new TreeSet<>();
        allKeys.addAll(filteredLocalKeys);
        allKeys.addAll(redisKeyTTLMap.keySet());

        int total = allKeys.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);

        List<String> sortedKeys = new ArrayList<>(allKeys);
        List<Map<String, Object>> keys = new ArrayList<>();

        for (int i = start; i < end && i < sortedKeys.size(); i++) {
            String key = sortedKeys.get(i);
            Map<String, Object> keyInfo = new HashMap<>();
            keyInfo.put("key", key);
            boolean inLocal = localKeys.contains(key);
            boolean inRedis = redisKeyTTLMap.containsKey(key);
            keyInfo.put("inLocal", inLocal);
            keyInfo.put("inRedis", inRedis);
            keyInfo.put("localExpire", localExpireSeconds);
            keyInfo.put("redisTtl", inRedis ? redisKeyTTLMap.get(key) : -1L);
            keyInfo.put("redisExpire", redisExpireSeconds);

            // 获取值大小（优先从Redis获取）
            if (inRedis) {
                try {
                    String redisKey = "cache:" + cacheName + ":" + key;
                    Object value = redisTemplate.opsForValue().get(redisKey);
                    if (value != null) {
                        String valueStr = gson.toJson(value);
                        keyInfo.put("size", valueStr.length());
                    } else {
                        keyInfo.put("size", 0);
                    }
                } catch (Exception e) {
                    keyInfo.put("size", 0);
                }
            } else {
                keyInfo.put("size", -1);
            }
            keys.add(keyInfo);
        }

        result.put("keys", keys);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("localKeyCount", filteredLocalKeys.size());
        result.put("redisKeyCount", redisKeyTTLMap.size());

        return result;
    }

    /**
     * 获取缓存状态信息
     */
    @GetMapping("/status")
    public Response<Map<String, Object>> getStatus() {
        if (!checkPermission()) {
            return Response.fail(null, "无权限访问");
        }
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("redisEnabled", cacheService.isRedisEnabled());
            status.put("cacheCount", ehCacheManager.getAllNames().size());
            status.put("instanceCount", ehCacheManager.getAllInstanceNames().size());
            return Response.ok(status);
        } catch (Exception e) {
            log.error("获取缓存状态失败: {}", e.getMessage(), e);
            return Response.fail(null, "获取缓存状态失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有缓存列表
     */
    @GetMapping("/list")
    public Response<List<Map<String, Object>>> getCacheList() {
        if (!checkPermission()) {
            return Response.fail(null, "无权限访问");
        }
        try {
            List<Map<String, Object>> cacheList = new ArrayList<>();
            Set<String> cacheNames = ehCacheManager.getAllNames();
            for (String cacheName : cacheNames) {
                Map<String, Object> cacheInfo = new HashMap<>();
                cacheInfo.put("name", cacheName);
                // 获取缓存配置
                CacheConfig config = ehCacheManager.getConfig(cacheName);
                if (config != null) {
                    cacheInfo.put("keyType", config.getKey() != null ? config.getKey().getSimpleName() : "Unknown");
                    cacheInfo.put("valueType", config.getValue() != null ? config.getValue().getSimpleName() : "Unknown");
                    cacheInfo.put("maxEntries", config.getMax());
                    cacheInfo.put("expireTime", config.getExpire());
                    cacheInfo.put("redisTime", config.getRedis());
                    cacheInfo.put("timeUnit", config.getUnit() != null ? config.getUnit().name() : "MINUTES");
                }
                // 获取本地缓存实例和键数量
                Cache<?, ?> cache = ehCacheManager.getCache(cacheName);
                if (cache != null) {
                    Set<String> localKeys = getLocalCacheKeys(cacheName);
                    cacheInfo.put("localKeyCount", localKeys.size());
                    cacheInfo.put("exists", true);
                } else {
                    cacheInfo.put("localKeyCount", 0);
                    cacheInfo.put("exists", false);
                }
                // 检查Redis中的缓存数量
                int redisKeyCount = 0;
                if (cacheService.isRedisEnabled()) {
                    try {
                        String pattern = "cache:" + cacheName + ":*";
                        Set<String> keys = redisTemplate.keys(pattern);
                        redisKeyCount = keys != null ? keys.size() : 0;
                    } catch (Exception e) {
                        log.debug("获取Redis缓存数量失败: {}", e.getMessage());
                    }
                }
                cacheInfo.put("redisKeyCount", redisKeyCount);

                cacheList.add(cacheInfo);
            }
            // 按名称排序
            cacheList.sort(Comparator.comparing(m -> (String) m.get("name")));
            return Response.ok(cacheList);
        } catch (Exception e) {
            log.error("获取缓存列表失败: {}", e.getMessage(), e);
            return Response.fail(null, "获取缓存列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定缓存的键列表（合并本地缓存和Redis缓存）
     */
    @GetMapping("/keys/{name}")
    public Response<Map<String, Object>> getCacheKeys(@PathVariable String name, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        if (!checkPermission()) {
            return Response.fail(null, "无权限访问");
        }
        try {
            // 获取本地缓存键
            Set<String> localKeys = getLocalCacheKeys(name);

            // 获取Redis缓存键及TTL
            Map<String, Long> redisKeyTTLMap = new LinkedHashMap<>();
            if (cacheService.isRedisEnabled()) {
                try {
                    String pattern = "cache:" + name + ":*";
                    Set<String> redisKeys = redisTemplate.keys(pattern);
                    if (redisKeys != null) {
                        for (String rk : redisKeys) {
                            String key = rk.substring(("cache:" + name + ":").length());
                            Long ttl = redisTemplate.getExpire(rk);
                            redisKeyTTLMap.put(key, ttl != null ? ttl : -1L);
                        }
                    }
                } catch (Exception e) {
                    log.error("获取Redis键列表失败: {}", e.getMessage(), e);
                }
            }

            Map<String, Object> result = buildMergedKeysResult(name, localKeys, localKeys, redisKeyTTLMap, page, size);
            return Response.ok(result);
        } catch (Exception e) {
            log.error("获取缓存键列表失败: {}", e.getMessage(), e);
            return Response.fail(null, "获取缓存键列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取缓存键的值
     */
    @GetMapping("/value/{name}")
    public Response<Map<String, Object>> getCacheValue(@PathVariable String name, @RequestParam String key) {
        if (!checkPermission()) {
            return Response.fail(null, "无权限访问");
        }
        try {
            Map<String, Object> result = new HashMap<>();
            Object value = cacheService.get(name, key);
            result.put("key", key);
            result.put("value", value);
            result.put("valueType", value != null ? value.getClass().getName() : "null");
            result.put("valueJson", value != null ? gson.toJson(value) : "null");
            return Response.ok(result);
        } catch (Exception e) {
            log.error("获取缓存值失败: {}", e.getMessage(), e);
            return Response.fail(null, "获取缓存值失败: " + e.getMessage());
        }
    }

    /**
     * 删除缓存键
     */
    @DeleteMapping("/key/{name}")
    public Response<String> deleteCacheKey(@PathVariable String name, @RequestParam String key) {
        if (!checkPermission()) {
            return Response.fail("无权限访问");
        }
        try {
            cacheService.remove(name, key);
            return Response.ok("删除成功");
        } catch (Exception e) {
            log.error("删除缓存键失败: {}", e.getMessage(), e);
            return Response.fail("删除缓存键失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除缓存键
     */
    @DeleteMapping("/keys/{name}")
    public Response<String> deleteCacheKeys(@PathVariable String name, @RequestBody List<String> keys) {
        if (!checkPermission()) {
            return Response.fail("无权限访问");
        }
        try {
            int count = 0;
            for (String key : keys) {
                try {
                    cacheService.remove(name, key);
                    count++;
                } catch (Exception e) {
                    log.warn("删除缓存键失败: cacheName={}, key={}, error={}", name, key, e.getMessage());
                }
            }
            return Response.ok("成功删除 " + count + " 个键");
        } catch (Exception e) {
            log.error("批量删除缓存键失败: {}", e.getMessage(), e);
            return Response.fail("批量删除缓存键失败: " + e.getMessage());
        }
    }

    /**
     * 清空指定缓存
     */
    @DeleteMapping("/clear/{name}")
    public Response<String> clearCache(@PathVariable String name) {
        if (!checkPermission()) {
            return Response.fail("无权限访问");
        }
        try {
            cacheService.clear(name);
            return Response.ok("清空缓存成功");
        } catch (Exception e) {
            log.error("清空缓存失败: {}", e.getMessage(), e);
            return Response.fail("清空缓存失败: " + e.getMessage());
        }
    }

    /**
     * 清空所有缓存
     */
    @DeleteMapping("/clear/all")
    public Response<String> clearAllCaches() {
        if (!checkPermission()) {
            return Response.fail("无权限访问");
        }
        try {
            cacheService.clear();
            return Response.ok("清空所有缓存成功");
        } catch (Exception e) {
            log.error("清空所有缓存失败: {}", e.getMessage(), e);
            return Response.fail("清空所有缓存失败: " + e.getMessage());
        }
    }

    /**
     * 搜索缓存键（同时搜索本地缓存和Redis缓存）
     */
    @GetMapping("/search/{name}")
    public Response<Map<String, Object>> searchCacheKeys(@PathVariable String name, @RequestParam String pattern, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        if (!checkPermission()) {
            return Response.fail(null, "无权限访问");
        }
        try {
            // 获取本地缓存键并按模式过滤
            Set<String> allLocalKeys = getLocalCacheKeys(name);
            Set<String> filteredLocalKeys = new LinkedHashSet<>();
            for (String key : allLocalKeys) {
                if (matchWildcard(key, pattern)) {
                    filteredLocalKeys.add(key);
                }
            }

            // 从Redis搜索键
            Map<String, Long> redisKeyTTLMap = new LinkedHashMap<>();
            if (cacheService.isRedisEnabled()) {
                try {
                    String redisPattern = "cache:" + name + ":" + pattern;
                    Set<String> redisKeys = redisTemplate.keys(redisPattern);
                    if (redisKeys != null) {
                        for (String rk : redisKeys) {
                            String key = rk.substring(("cache:" + name + ":").length());
                            Long ttl = redisTemplate.getExpire(rk);
                            redisKeyTTLMap.put(key, ttl != null ? ttl : -1L);
                        }
                    }
                } catch (Exception e) {
                    log.error("搜索Redis键失败: {}", e.getMessage(), e);
                }
            }

            Map<String, Object> result = buildMergedKeysResult(name, allLocalKeys, filteredLocalKeys, redisKeyTTLMap, page, size);
            return Response.ok(result);
        } catch (Exception e) {
            log.error("搜索缓存键失败: {}", e.getMessage(), e);
            return Response.fail(null, "搜索缓存键失败: " + e.getMessage());
        }
    }
}
