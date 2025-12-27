package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.annotation.Endpoint;
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
import java.util.stream.Collectors;

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
                // 获取缓存实例
                Cache<?, ?> cache = ehCacheManager.getCache(cacheName);
                if (cache != null) {
                    // 尝试获取缓存大小（EhCache 3.x 不直接支持，这里返回-1表示不支持）
                    cacheInfo.put("size", -1);
                    cacheInfo.put("exists", true);
                } else {
                    cacheInfo.put("size", 0);
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
     * 获取指定缓存的键列表
     */
    @GetMapping("/keys/{name}")
    public Response<Map<String, Object>> getCacheKeys(@PathVariable String name, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        if (!checkPermission()) {
            return Response.fail(null, "无权限访问");
        }
        try {
            Map<String, Object> result = new HashMap<>();
            List<Map<String, Object>> keys = new ArrayList<>();
            // 从Redis获取键列表
            if (cacheService.isRedisEnabled()) {
                try {
                    String pattern = "cache:" + name + ":*";
                    Set<String> redisKeys = redisTemplate.keys(pattern);
                    if (redisKeys != null) {
                        int total = redisKeys.size();
                        int start = (page - 1) * size;
                        int end = Math.min(start + size, total);
                        List<String> sortedKeys = redisKeys.stream().sorted().collect(Collectors.toList());
                        for (int i = start; i < end && i < sortedKeys.size(); i++) {
                            String redisKey = sortedKeys.get(i);
                            String key = redisKey.substring(("cache:" + name + ":").length());
                            Map<String, Object> keyInfo = new HashMap<>();
                            keyInfo.put("key", key);
                            keyInfo.put("redisKey", redisKey);
                            // 获取键的TTL
                            Long ttl = redisTemplate.getExpire(redisKey);
                            keyInfo.put("ttl", ttl != null ? ttl : -1);
                            // 获取值的大小（估算）
                            try {
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

                            keys.add(keyInfo);
                        }
                        result.put("total", total);
                        result.put("page", page);
                        result.put("size", size);
                    } else {
                        result.put("total", 0);
                        result.put("page", page);
                        result.put("size", size);
                    }
                } catch (Exception e) {
                    log.error("获取Redis键列表失败: {}", e.getMessage(), e);
                    result.put("total", 0);
                    result.put("page", page);
                    result.put("size", size);
                }
            } else {
                result.put("total", 0);
                result.put("page", page);
                result.put("size", size);
            }
            result.put("keys", keys);
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
     * 搜索缓存键
     */
    @GetMapping("/search/{name}")
    public Response<Map<String, Object>> searchCacheKeys(@PathVariable String name, @RequestParam String pattern, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        if (!checkPermission()) {
            return Response.fail(null, "无权限访问");
        }
        try {
            Map<String, Object> result = new HashMap<>();
            List<Map<String, Object>> keys = new ArrayList<>();
            // 从Redis搜索键
            if (cacheService.isRedisEnabled()) {
                try {
                    // 将通配符模式转换为Redis模式
                    String redisPattern = "cache:" + name + ":" + pattern.replace("*", "*");
                    Set<String> redisKeys = redisTemplate.keys(redisPattern);
                    if (redisKeys != null) {
                        int total = redisKeys.size();
                        int start = (page - 1) * size;
                        int end = Math.min(start + size, total);
                        List<String> sortedKeys = redisKeys.stream().sorted().collect(Collectors.toList());
                        for (int i = start; i < end && i < sortedKeys.size(); i++) {
                            String redisKey = sortedKeys.get(i);
                            String key = redisKey.substring(("cache:" + name + ":").length());
                            Map<String, Object> keyInfo = new HashMap<>();
                            keyInfo.put("key", key);
                            keyInfo.put("redisKey", redisKey);
                            Long ttl = redisTemplate.getExpire(redisKey);
                            keyInfo.put("ttl", ttl != null ? ttl : -1);
                            try {
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

                            keys.add(keyInfo);
                        }
                        result.put("total", total);
                        result.put("page", page);
                        result.put("size", size);
                    } else {
                        result.put("total", 0);
                        result.put("page", page);
                        result.put("size", size);
                    }
                } catch (Exception e) {
                    log.error("搜索Redis键失败: {}", e.getMessage(), e);
                    result.put("total", 0);
                    result.put("page", page);
                    result.put("size", size);
                }
            } else {
                result.put("total", 0);
                result.put("page", page);
                result.put("size", size);
            }
            result.put("keys", keys);
            return Response.ok(result);
        } catch (Exception e) {
            log.error("搜索缓存键失败: {}", e.getMessage(), e);
            return Response.fail(null, "搜索缓存键失败: " + e.getMessage());
        }
    }
}
