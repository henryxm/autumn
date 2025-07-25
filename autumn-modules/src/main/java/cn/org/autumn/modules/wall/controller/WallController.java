package cn.org.autumn.modules.wall.controller;

import cn.org.autumn.modules.wall.entity.IpBlackEntity;
import cn.org.autumn.modules.wall.entity.IpWhiteEntity;
import cn.org.autumn.modules.wall.service.IpBlackService;
import cn.org.autumn.modules.wall.service.IpWhiteService;
import cn.org.autumn.modules.wall.service.ShieldService;
import cn.org.autumn.site.WallFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

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

    // 1. 防火墙开关
    @GetMapping("/firewallOpen")
    public Map<String, Object> getFirewallOpen() {
        Map<String, Object> map = new HashMap<>();
        map.put("firewallOpen", wallFactory.getFirewallOpen());
        return map;
    }

    @PostMapping("/firewallOpen")
    public Map<String, Object> setFirewallOpen(@RequestParam boolean open) {
        wallFactory.setFirewallOpen(open);
        return getFirewallOpen();
    }

    // 2. 黑名单阈值
    @GetMapping("/firewallCount")
    public Map<String, Object> getFirewallCount() {
        Map<String, Object> map = new HashMap<>();
        map.put("firewallCount", ipBlackService.getFirewallCount());
        return map;
    }

    @PostMapping("/firewallCount")
    public Map<String, Object> setFirewallCount(@RequestParam int count) {
        ipBlackService.setFirewallCount(count);
        return getFirewallCount();
    }

    // 3. Shield print/attack
    @GetMapping("/shieldStatus")
    public Map<String, Object> getShieldStatus() {
        Map<String, Object> map = new HashMap<>();
        map.put("print", ShieldService.getPrint());
        map.put("attack", ShieldService.getAttack());
        return map;
    }

    @PostMapping("/shieldPrint")
    public Map<String, Object> setShieldPrint(@RequestParam boolean print) {
        ShieldService.setPrint(print);
        return getShieldStatus();
    }

    @PostMapping("/shieldAttack")
    public Map<String, Object> setShieldAttack(@RequestParam boolean attack) {
        ShieldService.setAttack(attack);
        return getShieldStatus();
    }

    @PostMapping("/shieldEnable")
    public Map<String, Object> enableShield() {
        shieldService.enable();
        return getShieldStatus();
    }

    @PostMapping("/shieldDisable")
    public Map<String, Object> disableShield() {
        shieldService.disable();
        return getShieldStatus();
    }

    // 盾牌启用/禁用切换
    @PostMapping("/shield/toggle")
    public Map<String, Object> toggleShieldEnable() {
        boolean enable = shieldService.toggleEnable();
        return Collections.singletonMap("enable", enable);
    }

    @GetMapping("/shield/enable")
    public Map<String, Object> getShieldEnable() {
        boolean enable = shieldService.getEnable();
        return Collections.singletonMap("enable", enable);
    }

    // 4. 黑名单管理
    @GetMapping("/ipblack/list")
    public Set<String> getIpBlackList() {
        return ipBlackService.getIpBlackList();
    }

    @PostMapping("/ipblack/add")
    public Map<String, Object> addIpBlack(@RequestParam String ip) {
        IpBlackEntity entity = ipBlackService.create(ip, "手动添加", "手动添加");
        if (null == entity)
            return Collections.singletonMap("fail", false);
        return Collections.singletonMap("success", true);
    }

    @PostMapping("/ipblack/remove")
    public Map<String, Object> removeIpBlack(@RequestParam String ip) {
        ipBlackService.removeByIp(ip);
        return Collections.singletonMap("success", true);
    }

    // 5. 白名单管理
    @GetMapping("/ipwhite/list")
    public List<String> getIpWhiteList() {
        return ipWhiteService.getIpWhiteList();
    }

    @PostMapping("/ipwhite/add")
    public Map<String, Object> addIpWhite(@RequestParam String ip) {
        IpWhiteEntity entity = ipWhiteService.create(ip, "手动添加", "手动添加");
        if (null == entity)
            return Collections.singletonMap("fail", false);
        return Collections.singletonMap("success", true);
    }

    @PostMapping("/ipwhite/remove")
    public Map<String, Object> removeIpWhite(@RequestParam String ip) {
        ipWhiteService.removeByIp(ip);
        return Collections.singletonMap("success", true);
    }
}
