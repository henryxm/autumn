package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.modules.sys.service.RedisService;
import cn.org.autumn.modules.sys.service.SysUserRoleService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.model.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/sys/redis")
public class RedisController {

    @Autowired
    private RedisService redisService;

    @Autowired
    private SysUserRoleService sysUserRoleService;

    /**
     * 获取所有数据库信息
     */
    @GetMapping("/databases")
    public Response<List<Map<String, Object>>> getDatabases() {
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid())) {
            return Response.fail(null, "无权限访问");
        }
        
        try {
            List<Map<String, Object>> databases = redisService.getDatabases();
            return Response.ok(databases);
        } catch (Exception e) {
            return Response.fail(null, "获取数据库信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定数据库的键列表
     */
    @GetMapping("/keys/{database}")
    public Response<Map<String, Object>> getKeys(@PathVariable int database,
                           @RequestParam(defaultValue = "*") String pattern,
                           @RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "20") int size) {
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid())) {
            return Response.fail(null, "无权限访问");
        }
        
        try {
            Map<String, Object> result = redisService.getKeys(database, pattern, page, size);
            return Response.ok(result);
        } catch (Exception e) {
            return Response.fail(null, "获取键列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取键的详细信息
     */
    @GetMapping("/key/{key}")
    public Response<Map<String, Object>> getKeyInfo(@PathVariable String key) {
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid())) {
            return Response.fail(null, "无权限访问");
        }
        
        try {
            Map<String, Object> keyInfo = redisService.getKeyInfo(key);
            return Response.ok(keyInfo);
        } catch (Exception e) {
            return Response.fail(null, "获取键信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取键的值
     */
    @GetMapping("/value/{key}")
    public Response<Object> getKeyValue(@PathVariable String key) {
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid())) {
            return Response.fail(null, "无权限访问");
        }
        
        try {
            Object value = redisService.getKeyValue(key);
            return Response.ok(value);
        } catch (Exception e) {
            return Response.fail(null, "获取键值失败: " + e.getMessage());
        }
    }

    /**
     * 删除单个键
     */
    @DeleteMapping("/key/{key}")
    public Response<String> deleteKey(@PathVariable String key) {
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid())) {
            return Response.fail("无权限访问");
        }
        
        try {
            boolean result = redisService.deleteKey(key);
            if (result) {
                return Response.ok("删除成功");
            } else {
                return Response.fail("删除失败");
            }
        } catch (Exception e) {
            return Response.fail("删除键失败: " + e.getMessage());
        }
    }

    /**
     * 删除多个键
     */
    @DeleteMapping("/keys")
    public Response<String> deleteKeys(@RequestBody List<String> keys) {
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid())) {
            return Response.fail("无权限访问");
        }
        
        try {
            long count = redisService.deleteKeys(keys);
            return Response.ok("成功删除 " + count + " 个键");
        } catch (Exception e) {
            return Response.fail("删除键失败: " + e.getMessage());
        }
    }

    /**
     * 清空指定数据库
     */
    @DeleteMapping("/database/{database}")
    public Response<String> clearDatabase(@PathVariable int database) {
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid())) {
            return Response.fail("无权限访问");
        }
        
        try {
            boolean result = redisService.clearDatabase(database);
            if (result) {
                return Response.ok("清空数据库 " + database + " 成功");
            } else {
                return Response.fail("清空数据库失败");
            }
        } catch (Exception e) {
            return Response.fail("清空数据库失败: " + e.getMessage());
        }
    }

    /**
     * 清空所有数据库
     */
    @DeleteMapping("/all")
    public Response<String> clearAllDatabases() {
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid())) {
            return Response.fail("无权限访问");
        }
        
        try {
            boolean result = redisService.clearAllDatabases();
            if (result) {
                return Response.ok("清空所有数据库成功");
            } else {
                return Response.fail("清空所有数据库失败");
            }
        } catch (Exception e) {
            return Response.fail("清空所有数据库失败: " + e.getMessage());
        }
    }

    /**
     * 设置键的过期时间
     */
    @PostMapping("/expire/{key}")
    public Response<String> setKeyExpire(@PathVariable String key, @RequestParam long seconds) {
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid())) {
            return Response.fail("无权限访问");
        }
        
        try {
            boolean result = redisService.setKeyExpire(key, seconds);
            if (result) {
                return Response.ok("设置过期时间成功");
            } else {
                return Response.fail("设置过期时间失败");
            }
        } catch (Exception e) {
            return Response.fail("设置过期时间失败: " + e.getMessage());
        }
    }

    /**
     * 检查Redis连接状态
     */
    @GetMapping("/status")
    public Response<Map<String, Object>> getStatus() {
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid())) {
            return Response.fail(null, "无权限访问");
        }
        
        try {
            boolean connected = redisService.isConnected();
            Map<String, Object> serverInfo = redisService.getServerInfo();
            Map<String, Object> result = new HashMap<>();
            result.put("connected", connected);
            result.put("serverInfo", serverInfo);
            return Response.ok(result);
        } catch (Exception e) {
            return Response.fail(null, "获取状态失败: " + e.getMessage());
        }
    }
} 