package cn.org.autumn.modules.wall.controller;

import cn.org.autumn.exception.AException;
import cn.org.autumn.model.SecurityOpsConfig;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.sys.support.AdminAccessGuard;
import cn.org.autumn.modules.wall.entity.IpAdminWhiteEntity;
import cn.org.autumn.modules.wall.entity.IpBlackEntity;
import cn.org.autumn.modules.wall.entity.IpWhiteEntity;
import cn.org.autumn.modules.wall.service.IpAdminWhiteService;
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
    IpAdminWhiteService ipAdminWhiteService;

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

    /** 管理 IP 校验开关：默认关；开启后运维/gen 须 IP ∈ wall_ip_admin_white。 */
    @GetMapping("/adminIpEnable")
    public Map<String, Object> getAdminIpEnable(HttpServletRequest request) {
        requireOps(request, "adminIpEnable");
        Map<String, Object> map = new HashMap<>();
        map.put("adminIpEnable", ipAdminWhiteService.isAdminIpEnable());
        return map;
    }

    /**
     * 设置管理 IP 校验。关闭须 clearToken；开启时若当前 IP 未在名单则自动认领，避免自锁。
     */
    @PostMapping("/adminIpEnable")
    public Map<String, Object> setAdminIpEnable(@RequestParam boolean enable, HttpServletRequest request) {
        requireOps(request, "adminIpEnable");
        if (!enable) {
            assertClearToken(request);
        }
        SecurityOpsConfig current = sysConfigService.getSecurityOpsConfig();
        SecurityOpsConfig toSave = copyOpsConfig(current);
        toSave.setAdminIpEnable(enable);
        sysConfigService.saveSecurityOpsConfig(toSave);
        String claimedIp = null;
        if (enable) {
            String ip = IPUtils.getIp(request);
            if (StringUtils.isNotBlank(ip) && !ipAdminWhiteService.isAdminWhite(ip)) {
                IpAdminWhiteEntity entity = ipAdminWhiteService.create(ip, "enable", "开启管理IP校验时认领");
                if (entity == null) {
                    toSave.setAdminIpEnable(false);
                    sysConfigService.saveSecurityOpsConfig(toSave);
                    throw new AException("写入管理IP失败，已回滚开关", 500);
                }
                claimedIp = entity.getIp();
            }
        }
        adminAccessGuard.auditWithIp(request, "wall.adminIpEnable",
                "enable=" + enable + (claimedIp != null ? " claimedIp=" + claimedIp : ""));
        Map<String, Object> map = getAdminIpEnable(request);
        if (claimedIp != null) {
            map.put("claimedIp", claimedIp);
        }
        return map;
    }

    private static SecurityOpsConfig copyOpsConfig(SecurityOpsConfig src) {
        SecurityOpsConfig c = new SecurityOpsConfig();
        if (src == null) {
            return c;
        }
        c.setClearToken(src.getClearToken());
        c.setReinitToken(src.getReinitToken());
        c.setBeanExecuteEnabled(src.isBeanExecuteEnabled());
        c.setWebShellEnabled(src.isWebShellEnabled());
        c.setAdminIpEnable(src.isAdminIpEnable());
        return c;
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

    // 7. 管理级 IP 白名单（运维/gen 门禁；与用户 wall_ip_white 分离）
    @GetMapping("/ipadminwhite/status")
    public Map<String, Object> getIpAdminWhiteStatus(HttpServletRequest request) {
        if (ipAdminWhiteService.isBootstrapPending()) {
            adminAccessGuard.requireSystemAdmin("ipadminwhiteStatus");
        } else {
            requireOps(request, "ipadminwhiteStatus");
        }
        Map<String, Object> map = new HashMap<>();
        map.put("bootstrapPending", ipAdminWhiteService.isBootstrapPending());
        map.put("empty", ipAdminWhiteService.isEmpty());
        map.put("currentIp", IPUtils.getIp(request));
        return map;
    }

    /**
     * wall 页门禁状态（仅超管，不强制管理 IP）：用于未加白时展示临时安全密码门禁。
     */
    @GetMapping("/ipadminwhite/gate")
    public Map<String, Object> getIpAdminWhiteGate(HttpServletRequest request) {
        adminAccessGuard.requireSystemAdmin("ipadminwhiteGate");
        String ip = IPUtils.getIp(request);
        boolean adminIpEnable = ipAdminWhiteService.isAdminIpEnable();
        boolean bootstrapPending = adminIpEnable && ipAdminWhiteService.isBootstrapPending();
        boolean locked = false;
        if (adminIpEnable) {
            try {
                ipAdminWhiteService.checkAdmin(request, getClass(), "ipadminwhiteGate");
                // 校验开启且 IP 放行时，引导态仍须临时密钥认领
                locked = bootstrapPending;
            } catch (AException ignored) {
                locked = true;
            }
        }
        Map<String, Object> map = new HashMap<>();
        map.put("locked", locked);
        map.put("adminIpEnable", adminIpEnable);
        map.put("bootstrapPending", bootstrapPending);
        map.put("empty", ipAdminWhiteService.isEmpty());
        map.put("currentIp", ip);
        map.put("mode", bootstrapPending ? "bootstrap" : "clearToken");
        map.put("hint", bootstrapPending
                ? "请输入启动日志中的一次性临时密钥（AdminIpBootstrap secret=）"
                : "请输入运维临时安全密码（SECURITY_OPS_CONFIG.clearToken）以认领当前 IP");
        return map;
    }

    @PostMapping("/ipadminwhite/bootstrap")
    public Map<String, Object> bootstrapIpAdminWhite(
            @RequestParam(required = false) String secret,
            HttpServletRequest request) {
        adminAccessGuard.requireSystemAdmin("ipadminwhiteBootstrap");
        if (StringUtils.isBlank(secret)) {
            secret = request.getHeader("x-admin-ip-bootstrap");
        }
        IpAdminWhiteEntity entity = ipAdminWhiteService.bootstrapClaim(request, secret);
        adminAccessGuard.auditWithIp(request, "admin_ip_bootstrap", "ip=" + entity.getIp());
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("ip", entity.getIp());
        return result;
    }

    /**
     * 未在管理 IP 白名单时：校验临时安全密码后认领当前 IP。
     * 引导态走 bootstrap 密钥；已有名单时走 clearToken。
     */
    @PostMapping("/ipadminwhite/unlock")
    public Map<String, Object> unlockIpAdminWhite(
            @RequestParam(required = false) String secret,
            HttpServletRequest request) {
        adminAccessGuard.requireSystemAdmin("ipadminwhiteUnlock");
        if (StringUtils.isBlank(secret)) {
            secret = request.getHeader("x-admin-ip-bootstrap");
        }
        if (StringUtils.isBlank(secret)) {
            secret = request.getHeader("x-clear-authentication");
        }
        if (StringUtils.isBlank(secret)) {
            throw new AException("请输入临时安全密码", 403);
        }
        String ip = IPUtils.getIp(request);
        if (ipAdminWhiteService.isBootstrapPending() || ipAdminWhiteService.isEmpty()) {
            IpAdminWhiteEntity entity = ipAdminWhiteService.bootstrapClaim(request, secret);
            adminAccessGuard.auditWithIp(request, "admin_ip_unlock_bootstrap", "ip=" + entity.getIp());
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("ip", entity.getIp());
            result.put("mode", "bootstrap");
            return result;
        }
        if (ipAdminWhiteService.isAdminWhite(ip)) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("ip", ip);
            result.put("mode", "already");
            return result;
        }
        SecurityOpsConfig ops = sysConfigService.getSecurityOpsConfig();
        String configured = ops != null ? ops.getClearToken() : null;
        if (StringUtils.isBlank(configured) || !configured.equals(secret.trim())) {
            adminAccessGuard.warnDenied("admin_ip_unlock", "ipadminwhiteUnlock", request, "临时安全密码错误");
            throw new AException("临时安全密码无效", 403);
        }
        IpAdminWhiteEntity entity = ipAdminWhiteService.create(ip, "unlock", "临时安全密码认领");
        if (entity == null) {
            throw new AException("写入管理IP失败", 500);
        }
        adminAccessGuard.auditWithIp(request, "admin_ip_unlock", "ip=" + entity.getIp());
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("ip", entity.getIp());
        result.put("mode", "clearToken");
        return result;
    }

    @GetMapping("/ipadminwhite/list")
    public Map<String, Object> getIpAdminWhiteList(@RequestParam Map<String, Object> params, HttpServletRequest request) {
        requireOps(request, "ipadminwhiteList");
        PageUtils page = ipAdminWhiteService.queryPage(params);
        List<Map<String, Object>> data = page.getList().stream()
                .map(entity -> {
                    IpAdminWhiteEntity ipEntity = (IpAdminWhiteEntity) entity;
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

    @PostMapping("/ipadminwhite/add")
    public Map<String, Object> addIpAdminWhite(@RequestParam String ip, HttpServletRequest request) {
        requireOps(request, "ipadminwhiteAdd");
        IpAdminWhiteEntity entity = ipAdminWhiteService.create(ip, "手动添加", "手动添加");
        adminAccessGuard.auditWithIp(request, "wall.ipadminwhite.add", "ip=" + ip);
        if (null == entity) {
            return Collections.singletonMap("fail", false);
        }
        return Collections.singletonMap("success", true);
    }

    @PostMapping("/ipadminwhite/remove")
    public Map<String, Object> removeIpAdminWhite(@RequestParam String ip, HttpServletRequest request) {
        requireOps(request, "ipadminwhiteRemove");
        String current = IPUtils.getIp(request);
        if (StringUtils.isNotBlank(ip) && ip.equals(current)) {
            assertClearToken(request);
        }
        ipAdminWhiteService.removeByIp(ip);
        adminAccessGuard.auditWithIp(request, "wall.ipadminwhite.remove", "ip=" + ip);
        return Collections.singletonMap("success", true);
    }
}
