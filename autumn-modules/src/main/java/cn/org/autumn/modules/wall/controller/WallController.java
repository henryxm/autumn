package cn.org.autumn.modules.wall.controller;

import cn.org.autumn.exception.AException;
import cn.org.autumn.model.SecurityOpsConfig;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.sys.support.AdminAccessGuard;
import cn.org.autumn.modules.wall.entity.IpBlackEntity;
import cn.org.autumn.modules.wall.entity.IpWhiteEntity;
import cn.org.autumn.modules.wall.service.IpBlackService;
import cn.org.autumn.modules.wall.service.IpWhiteService;
import cn.org.autumn.modules.wall.service.ShieldService;
import cn.org.autumn.site.WallFactory;
import cn.org.autumn.utils.IPUtils;
import cn.org.autumn.utils.PageUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
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
    AdminAccessGuard adminAccessGuard;

    @Autowired
    SysConfigService sysConfigService;

    private void requireOps(HttpServletRequest request, String method) {
        adminAccessGuard.requireOpsAdmin(request, getClass(), method);
    }

    private void assertClearToken(HttpServletRequest request) {
        SecurityOpsConfig ops = sysConfigService.getSecurityOpsConfig();
        String configured = ops != null ? ops.getClearToken() : null;
        String token = request.getHeader("x-clear-authentication");
        if (StringUtils.isBlank(configured) || StringUtils.isBlank(token) || !configured.equals(token)) {
            throw new AException("需要运维 clear Token 确认", 403);
        }
    }

    // 1. 防火墙开关
    @GetMapping("/firewallOpen")
    public Map<String, Object> getFirewallOpen(HttpServletRequest request) {
        requireOps(request, "firewallOpen");
        Map<String, Object> map = new HashMap<>();
        map.put("firewallOpen", wallFactory.getFirewallOpen());
        return map;
    }

    @PostMapping("/firewallOpen")
    public Map<String, Object> setFirewallOpen(@RequestParam boolean open, HttpServletRequest request) {
        requireOps(request, "firewallOpen");
        wallFactory.setFirewallOpen(open);
        adminAccessGuard.auditWithIp(request, "wall.firewallOpen", "open=" + open);
        return getFirewallOpen(request);
    }

    // 2. 黑名单阈值
    @GetMapping("/firewallCount")
    public Map<String, Object> getFirewallCount(HttpServletRequest request) {
        requireOps(request, "firewallCount");
        Map<String, Object> map = new HashMap<>();
        map.put("firewallCount", ipBlackService.getFirewallCount());
        return map;
    }

    @PostMapping("/firewallCount")
    public Map<String, Object> setFirewallCount(@RequestParam int count, HttpServletRequest request) {
        requireOps(request, "firewallCount");
        ipBlackService.setFirewallCount(count);
        adminAccessGuard.auditWithIp(request, "wall.firewallCount", "count=" + count);
        return getFirewallCount(request);
    }

    // 3. Shield print/attack
    @GetMapping("/shieldStatus")
    public Map<String, Object> getShieldStatus(HttpServletRequest request) {
        requireOps(request, "shieldStatus");
        Map<String, Object> map = new HashMap<>();
        map.put("print", ShieldService.getPrint());
        map.put("attack", ShieldService.getAttack());
        return map;
    }

    @PostMapping("/shieldPrint")
    public Map<String, Object> setShieldPrint(@RequestParam boolean print, HttpServletRequest request) {
        requireOps(request, "shieldPrint");
        ShieldService.setPrint(print);
        adminAccessGuard.auditWithIp(request, "wall.shieldPrint", "print=" + print);
        return getShieldStatus(request);
    }

    @PostMapping("/shieldAttack")
    public Map<String, Object> setShieldAttack(@RequestParam boolean attack, HttpServletRequest request) {
        requireOps(request, "shieldAttack");
        ShieldService.setAttack(attack);
        adminAccessGuard.auditWithIp(request, "wall.shieldAttack", "attack=" + attack);
        return getShieldStatus(request);
    }

    @PostMapping("/shieldEnable")
    public Map<String, Object> enableShield(HttpServletRequest request) {
        requireOps(request, "shieldEnable");
        shieldService.enable();
        adminAccessGuard.auditWithIp(request, "wall.shieldEnable", "");
        return getShieldStatus(request);
    }

    @PostMapping("/shieldDisable")
    public Map<String, Object> disableShield(HttpServletRequest request) {
        requireOps(request, "shieldDisable");
        shieldService.disable();
        adminAccessGuard.auditWithIp(request, "wall.shieldDisable", "");
        return getShieldStatus(request);
    }

    @PostMapping("/shield/toggle")
    public Map<String, Object> toggleShieldEnable(HttpServletRequest request) {
        requireOps(request, "shieldToggle");
        boolean enable = shieldService.toggleEnable();
        adminAccessGuard.auditWithIp(request, "wall.shieldToggle", "enable=" + enable);
        return Collections.singletonMap("enable", enable);
    }

    @GetMapping("/shield/enable")
    public Map<String, Object> getShieldEnable(HttpServletRequest request) {
        requireOps(request, "shieldEnableGet");
        boolean enable = shieldService.getEnable();
        return Collections.singletonMap("enable", enable);
    }

    // 4. 黑名单管理
    @GetMapping("/ipblack/list")
    public Map<String, Object> getIpBlackList(@RequestParam Map<String, Object> params, HttpServletRequest request) {
        requireOps(request, "ipblackList");
        PageUtils page = ipBlackService.queryPage(params);
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
    public Map<String, Object> addIpBlack(@RequestParam String ip, HttpServletRequest request) {
        requireOps(request, "ipblackAdd");
        IpBlackEntity entity = ipBlackService.create(ip, "手动添加", "手动添加");
        adminAccessGuard.auditWithIp(request, "wall.ipblack.add", "ip=" + ip);
        if (null == entity)
            return Collections.singletonMap("fail", false);
        return Collections.singletonMap("success", true);
    }

    @PostMapping("/ipblack/remove")
    public Map<String, Object> removeIpBlack(@RequestParam String ip, HttpServletRequest request) {
        requireOps(request, "ipblackRemove");
        ipBlackService.removeByIp(ip);
        adminAccessGuard.auditWithIp(request, "wall.ipblack.remove", "ip=" + ip);
        return Collections.singletonMap("success", true);
    }

    // 5. 白名单管理
    @GetMapping("/ipwhite/list")
    public Map<String, Object> getIpWhiteList(@RequestParam Map<String, Object> params, HttpServletRequest request) {
        requireOps(request, "ipwhiteList");
        PageUtils page = ipWhiteService.queryPage(params);
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
    public Map<String, Object> addIpWhite(@RequestParam String ip, HttpServletRequest request) {
        requireOps(request, "ipwhiteAdd");
        IpWhiteEntity entity = ipWhiteService.create(ip, "手动添加", "手动添加");
        adminAccessGuard.auditWithIp(request, "wall.ipwhite.add", "ip=" + ip);
        if (null == entity)
            return Collections.singletonMap("fail", false);
        return Collections.singletonMap("success", true);
    }

    @PostMapping("/ipwhite/remove")
    public Map<String, Object> removeIpWhite(@RequestParam String ip, HttpServletRequest request) {
        requireOps(request, "ipwhiteRemove");
        String current = IPUtils.getIp(request);
        if (StringUtils.isNotBlank(ip) && ip.equals(current)) {
            assertClearToken(request);
        }
        ipWhiteService.removeByIp(ip);
        adminAccessGuard.auditWithIp(request, "wall.ipwhite.remove", "ip=" + ip);
        return Collections.singletonMap("success", true);
    }

    // 6. 获取当前访问IP
    @GetMapping("/currentIp")
    public Map<String, Object> getCurrentIp(HttpServletRequest request) {
        requireOps(request, "currentIp");
        String ip = IPUtils.getIp(request);
        return Collections.singletonMap("ip", ip);
    }
}
