package cn.org.autumn.modules.wall.controller;

import cn.org.autumn.modules.sys.service.SysUserRoleService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.wall.entity.IpBlackEntity;
import cn.org.autumn.modules.wall.entity.IpWhiteEntity;
import cn.org.autumn.modules.wall.service.IpBlackService;
import cn.org.autumn.modules.wall.service.IpWhiteService;
import cn.org.autumn.modules.wall.service.ShieldService;
import cn.org.autumn.site.WallFactory;
import cn.org.autumn.utils.IPUtils;
import cn.org.autumn.utils.PageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("wall/api")
public class WallController {

    @Autowired
    WallFactory wallFactory;

    @Autowired
    IpBlackService ipBlackService;

    @Autowired
    IpWhiteService ipWhiteService;

    @Autowired
    ShieldService shieldService;

    @Autowired
    SysUserRoleService sysUserRoleService;

    // 1. 防火墙开关
    @GetMapping("/firewallOpen")
    public Map<String, Object> getFirewallOpen() throws Exception {
        boolean admin = sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid());
        if (!admin)
            throw new Exception("系统错误");
        Map<String, Object> map = new HashMap<>();
        map.put("firewallOpen", wallFactory.getFirewallOpen());
        return map;
    }

    @PostMapping("/firewallOpen")
    public Map<String, Object> setFirewallOpen(@RequestParam boolean open) throws Exception {
        boolean admin = sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid());
        if (!admin)
            throw new Exception("系统错误");
        wallFactory.setFirewallOpen(open);
        return getFirewallOpen();
    }

    // 2. 黑名单阈值
    @GetMapping("/firewallCount")
    public Map<String, Object> getFirewallCount() throws Exception {
        boolean admin = sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid());
        if (!admin)
            throw new Exception("系统错误");
        Map<String, Object> map = new HashMap<>();
        map.put("firewallCount", ipBlackService.getFirewallCount());
        return map;
    }

    @PostMapping("/firewallCount")
    public Map<String, Object> setFirewallCount(@RequestParam int count) throws Exception {
        boolean admin = sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid());
        if (!admin)
            throw new Exception("系统错误");
        ipBlackService.setFirewallCount(count);
        return getFirewallCount();
    }

    // 3. Shield print/attack
    @GetMapping("/shieldStatus")
    public Map<String, Object> getShieldStatus() throws Exception {
        boolean admin = sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid());
        if (!admin)
            throw new Exception("系统错误");
        Map<String, Object> map = new HashMap<>();
        map.put("print", ShieldService.getPrint());
        map.put("attack", ShieldService.getAttack());
        return map;
    }

    @PostMapping("/shieldPrint")
    public Map<String, Object> setShieldPrint(@RequestParam boolean print) throws Exception {
        boolean admin = sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid());
        if (!admin)
            throw new Exception("系统错误");
        ShieldService.setPrint(print);
        return getShieldStatus();
    }

    @PostMapping("/shieldAttack")
    public Map<String, Object> setShieldAttack(@RequestParam boolean attack) throws Exception {
        boolean admin = sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid());
        if (!admin)
            throw new Exception("系统错误");
        ShieldService.setAttack(attack);
        return getShieldStatus();
    }

    @PostMapping("/shieldEnable")
    public Map<String, Object> enableShield() throws Exception {
        boolean admin = sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid());
        if (!admin)
            throw new Exception("系统错误");
        shieldService.enable();
        return getShieldStatus();
    }

    @PostMapping("/shieldDisable")
    public Map<String, Object> disableShield() throws Exception {
        boolean admin = sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid());
        if (!admin)
            throw new Exception("系统错误");
        shieldService.disable();
        return getShieldStatus();
    }

    // 盾牌启用/禁用切换
    @PostMapping("/shield/toggle")
    public Map<String, Object> toggleShieldEnable() throws Exception {
        boolean admin = sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid());
        if (!admin)
            throw new Exception("系统错误");
        boolean enable = shieldService.toggleEnable();
        return Collections.singletonMap("enable", enable);
    }

    @GetMapping("/shield/enable")
    public Map<String, Object> getShieldEnable() throws Exception {
        boolean admin = sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid());
        if (!admin)
            throw new Exception("系统错误");
        boolean enable = shieldService.getEnable();
        return Collections.singletonMap("enable", enable);
    }

    // 4. 黑名单管理
    @GetMapping("/ipblack/list")
    public Map<String, Object> getIpBlackList(@RequestParam Map<String, Object> params) throws Exception {
        boolean admin = sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid());
        if (!admin)
            throw new Exception("系统错误");
        
        // 使用分页查询
        PageUtils page = ipBlackService.queryPage(params);
        
        // 转换为layui table需要的格式
        List<Map<String, Object>> data = page.getList().stream()
                .map(entity -> {
                    IpBlackEntity ipEntity = (IpBlackEntity) entity;
                    Map<String, Object> item = new HashMap<>();
                    item.put("ip", ipEntity.getIp() != null ? ipEntity.getIp() : "");
                    item.put("count", ipEntity.getCount() != null ? ipEntity.getCount() : 0);
                    item.put("today", ipEntity.getToday() != null ? ipEntity.getToday() : 0);
                    item.put("tag", ipEntity.getTag() != null ? ipEntity.getTag() : "");
                    item.put("description", ipEntity.getDescription() != null ? ipEntity.getDescription() : "");
                    item.put("available", ipEntity.getAvailable() != null ? ipEntity.getAvailable() : 0);
                    item.put("createTime", ipEntity.getCreateTime() != null ? ipEntity.getCreateTime().getTime() : null);
                    return item;
                })
                .collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("msg", "");
        result.put("count", page.getTotalCount());
        result.put("data", data);
        return result;
    }

    @PostMapping("/ipblack/add")
    public Map<String, Object> addIpBlack(@RequestParam String ip) throws Exception {
        boolean admin = sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid());
        if (!admin)
            throw new Exception("系统错误");
        IpBlackEntity entity = ipBlackService.create(ip, "手动添加", "手动添加");
        if (null == entity)
            return Collections.singletonMap("fail", false);
        return Collections.singletonMap("success", true);
    }

    @PostMapping("/ipblack/remove")
    public Map<String, Object> removeIpBlack(@RequestParam String ip) throws Exception {
        boolean admin = sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid());
        if (!admin)
            throw new Exception("系统错误");
        ipBlackService.removeByIp(ip);
        return Collections.singletonMap("success", true);
    }

    // 5. 白名单管理
    @GetMapping("/ipwhite/list")
    public Map<String, Object> getIpWhiteList(@RequestParam Map<String, Object> params) throws Exception {
        boolean admin = sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid());
        if (!admin)
            throw new Exception("系统错误");
        
        // 使用分页查询
        PageUtils page = ipWhiteService.queryPage(params);
        
        // 转换为layui table需要的格式
        List<Map<String, Object>> data = page.getList().stream()
                .map(entity -> {
                    IpWhiteEntity ipEntity = (IpWhiteEntity) entity;
                    Map<String, Object> item = new HashMap<>();
                    item.put("ip", ipEntity.getIp() != null ? ipEntity.getIp() : "");
                    item.put("count", ipEntity.getCount() != null ? ipEntity.getCount() : 0);
                    item.put("today", ipEntity.getToday() != null ? ipEntity.getToday() : 0);
                    item.put("tag", ipEntity.getTag() != null ? ipEntity.getTag() : "");
                    item.put("description", ipEntity.getDescription() != null ? ipEntity.getDescription() : "");
                    item.put("forbidden", ipEntity.getForbidden() != null ? ipEntity.getForbidden() : 0);
                    item.put("createTime", ipEntity.getCreateTime() != null ? ipEntity.getCreateTime().getTime() : null);
                    return item;
                })
                .collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("msg", "");
        result.put("count", page.getTotalCount());
        result.put("data", data);
        return result;
    }

    @PostMapping("/ipwhite/add")
    public Map<String, Object> addIpWhite(@RequestParam String ip) throws Exception {
        IpWhiteEntity entity = ipWhiteService.create(ip, "手动添加", "手动添加");
        if (null == entity)
            return Collections.singletonMap("fail", false);
        return Collections.singletonMap("success", true);
    }

    @PostMapping("/ipwhite/remove")
    public Map<String, Object> removeIpWhite(@RequestParam String ip) throws Exception {
        boolean admin = sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid());
        if (!admin)
            throw new Exception("系统错误");
        ipWhiteService.removeByIp(ip);
        return Collections.singletonMap("success", true);
    }

    // 6. 获取当前访问IP
    @GetMapping("/currentIp")
    public Map<String, Object> getCurrentIp(HttpServletRequest request) throws Exception {
        boolean admin = sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid());
        if (!admin)
            throw new Exception("系统错误");
        String ip = IPUtils.getIp(request);
        return Collections.singletonMap("ip", ip);
    }
}
