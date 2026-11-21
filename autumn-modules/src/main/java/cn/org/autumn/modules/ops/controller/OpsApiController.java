package cn.org.autumn.modules.ops.controller;

import cn.org.autumn.exception.AException;
import cn.org.autumn.model.OpsClientCertEntry;
import cn.org.autumn.model.Response;
import cn.org.autumn.model.SecurityOpsCertConfig;
import cn.org.autumn.model.SecurityOpsConfig;
import cn.org.autumn.modules.ops.service.OpsCertService;
import cn.org.autumn.modules.ops.service.OpsRuntimeSnapshotService;
import cn.org.autumn.modules.ops.service.OpsSessionService;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.sys.support.AdminAccessGuard;
import cn.org.autumn.modules.wall.entity.IpAdminWhiteEntity;
import cn.org.autumn.modules.wall.entity.IpBlackEntity;
import cn.org.autumn.modules.wall.entity.IpWhiteEntity;
import cn.org.autumn.modules.wall.entity.JumpEntity;
import cn.org.autumn.modules.wall.entity.ShieldEntity;
import cn.org.autumn.modules.wall.service.IpAdminWhiteService;
import cn.org.autumn.modules.wall.service.IpBlackService;
import cn.org.autumn.modules.wall.service.IpWhiteService;
import cn.org.autumn.modules.wall.service.JumpService;
import cn.org.autumn.modules.wall.service.ShieldService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import cn.org.autumn.ops.AutumnOpsProbe;
import cn.org.autumn.utils.IPUtils;
import cn.org.autumn.utils.PageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Date;

/**
 * 运维双边 RSA 开放 API（与业务 {@code /rsa/api/v1} 隔离；豁免管理 IP 门禁）。
 */
@Slf4j
@RestController
@RequestMapping("/ops/api/v1")
public class OpsApiController {

    @Autowired
    private OpsCertService opsCertService;

    @Autowired
    private OpsSessionService opsSessionService;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private IpAdminWhiteService ipAdminWhiteService;

    @Autowired
    private IpWhiteService ipWhiteService;

    @Autowired
    private IpBlackService ipBlackService;

    @Autowired
    private ShieldService shieldService;

    @Autowired
    private JumpService jumpService;

    @Autowired
    private AdminAccessGuard adminAccessGuard;

    @Autowired
    private OpsRuntimeSnapshotService opsRuntimeSnapshotService;

    @PostMapping("/probe")
    public Response<Map<String, Object>> probe(HttpServletRequest request) {
        assertProbe(request);
        // Do not create server RSA here; keys are created on successful enroll.
        Map<String, Object> data = new HashMap<>();
        data.put("autumn", true);
        data.put("protocolVersion", 1);
        data.put("opsCert", opsCertService.opsCertStatus());
        data.put("adminIpEnable", sysConfigService.isAdminIpEnable());
        data.put("enrollAvailable", opsCertService.isEnrollAvailable());
        data.put("discoverEnabled", opsCertService.isDiscoverEnabled());
        SecurityOpsCertConfig cfg = opsCertService.getConfig();
        data.put("siteId", cfg.getSiteId());
        data.put("serverFingerprint", cfg.getFingerprint());
        return Response.ok(data);
    }

    @PostMapping("/enroll")
    public Response<Map<String, Object>> enroll(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        String secret = str(body, "secret");
        String clientPublicKey = str(body, "clientPublicKey");
        String label = str(body, "label");
        OpsClientCertEntry entry = opsCertService.enroll(secret, clientPublicKey, label);
        // App 首次入驻成功：自动开启管理 IP 校验并认领当前访问 IP
        enableAdminIpGateAfterOpsCertBind(request, entry.getCertId(), "ops-enroll");
        SecurityOpsCertConfig cfg = opsCertService.getConfig();
        Map<String, Object> data = new HashMap<>();
        data.put("certId", entry.getCertId());
        data.put("siteId", cfg.getSiteId());
        data.put("serverPublicKey", cfg.getServerPublicKey());
        data.put("serverFingerprint", cfg.getFingerprint());
        data.put("adminIpEnable", sysConfigService.isAdminIpEnable());
        data.put("discoverEnabled", opsCertService.isDiscoverEnabled());
        // 私钥永不下发
        return Response.ok(data);
    }

    @PostMapping("/challenge")
    public Response<Map<String, Object>> challenge(@RequestBody Map<String, Object> body) {
        String certId = str(body, "certId");
        String nonce = opsSessionService.createChallenge(certId);
        Map<String, Object> data = new HashMap<>();
        data.put("nonce", nonce);
        data.put("expireSeconds", 120);
        return Response.ok(data);
    }

    @PostMapping("/session")
    public Response<Map<String, Object>> session(@RequestBody Map<String, Object> body) {
        String certId = str(body, "certId");
        String nonce = str(body, "nonce");
        String signature = str(body, "signature");
        String aesKeyWrapped = str(body, "aesKeyWrapped");
        String aesIv = str(body, "aesIv");
        OpsSessionService.OpsSession session = opsSessionService.openSession(
                certId, nonce, signature, aesKeyWrapped, aesIv);
        Map<String, Object> data = new HashMap<>();
        data.put("sessionId", session.getSessionId());
        data.put("expireAt", session.getExpireAt());
        return Response.ok(data);
    }

    @PostMapping("/adminip/list")
    public Response<Map<String, Object>> adminIpList(@RequestBody(required = false) Map<String, Object> body,
                                                     HttpServletRequest request) {
        requireOpsSession(request);
        Map<String, Object> params = pageParams(body);
        putOptional01(params, body, "forbidden");
        PageUtils page = ipAdminWhiteService.queryPage(params);
        List<Map<String, Object>> rows = page.getList().stream()
                .map(o -> adminIpRow((IpAdminWhiteEntity) o))
                .collect(Collectors.toList());
        return Response.ok(pageData(page, rows));
    }

    @PostMapping("/adminip/add")
    public Response<Map<String, Object>> adminIpAdd(@RequestBody Map<String, Object> body,
                                                    HttpServletRequest request) {
        OpsSessionService.OpsSession session = requireOpsSession(request);
        String ip = resolveIp(body, request);
        boolean existed = ipAdminWhiteService.getByIp(ip) != null;
        IpAdminWhiteEntity entity = ipAdminWhiteService.create(ip, "ops-cert", "ops-cert certId=" + session.getCertId());
        if (entity == null) {
            throw new AException("写入管理IP失败", 500);
        }
        adminAccessGuard.auditWithIp(request, "ops.adminip.add", "ip=" + ip + " certId=" + session.getCertId());
        Map<String, Object> data = adminIpRow(entity);
        data.put("success", true);
        data.put("existed", existed);
        return Response.ok(data);
    }

    @PostMapping("/adminip/remove")
    public Response<Map<String, Object>> adminIpRemove(@RequestBody Map<String, Object> body,
                                                       HttpServletRequest request) {
        OpsSessionService.OpsSession session = requireOpsSession(request);
        String ip = str(body, "ip");
        if (StringUtils.isBlank(ip)) {
            throw new AException("ip 不能为空", 400);
        }
        String current = IPUtils.getIp(request);
        if (StringUtils.isNotBlank(current) && current.equals(ip)) {
            assertClearToken(body);
        }
        ipAdminWhiteService.removeByIp(ip);
        adminAccessGuard.auditWithIp(request, "ops.adminip.remove", "ip=" + ip + " certId=" + session.getCertId());
        return Response.ok(Map.of("success", true));
    }

    /** 禁用/启用管理 IP：{@code forbidden=1} 禁用，{@code 0} 启用。 */
    @PostMapping("/adminip/setForbidden")
    public Response<Map<String, Object>> adminIpSetForbidden(@RequestBody Map<String, Object> body,
                                                             HttpServletRequest request) {
        OpsSessionService.OpsSession session = requireOpsSession(request);
        String ip = str(body, "ip");
        int forbidden = require01(body, "forbidden");
        if (StringUtils.isBlank(ip)) {
            throw new AException("ip 不能为空", 400);
        }
        IpAdminWhiteEntity entity = ipAdminWhiteService.getByIp(ip);
        if (entity == null) {
            throw new AException("管理IP不存在", 404);
        }
        entity.setForbidden(forbidden);
        entity.setUpdateTime(new Date());
        ipAdminWhiteService.updateById(entity);
        adminAccessGuard.auditWithIp(request, "ops.adminip.setForbidden",
                "ip=" + ip + " forbidden=" + forbidden + " certId=" + session.getCertId());
        return Response.ok(Map.of("ip", ip, "forbidden", forbidden, "success", true));
    }

    @PostMapping("/currentIp")
    public Response<Map<String, Object>> currentIp(HttpServletRequest request) {
        requireOpsSession(request);
        String ip = StringUtils.defaultString(IPUtils.getIp(request));
        Map<String, Object> data = new HashMap<>();
        data.put("ip", ip);
        data.put("inAdmin", StringUtils.isNotBlank(ip) && ipAdminWhiteService.getByIp(ip) != null);
        data.put("inWhite", StringUtils.isNotBlank(ip) && ipWhiteService.getByIp(ip) != null);
        return Response.ok(data);
    }

    @PostMapping("/ipwhite/list")
    public Response<Map<String, Object>> ipWhiteList(@RequestBody(required = false) Map<String, Object> body,
                                                     HttpServletRequest request) {
        requireOpsSession(request);
        Map<String, Object> params = pageParams(body);
        putOptional01(params, body, "forbidden");
        PageUtils page = ipWhiteService.queryPage(params);
        List<Map<String, Object>> rows = page.getList().stream()
                .map(o -> ipWhiteRow((IpWhiteEntity) o))
                .collect(Collectors.toList());
        return Response.ok(pageData(page, rows));
    }

    @PostMapping("/ipwhite/add")
    public Response<Map<String, Object>> ipWhiteAdd(@RequestBody Map<String, Object> body,
                                                    HttpServletRequest request) {
        OpsSessionService.OpsSession session = requireOpsSession(request);
        String ip = resolveIp(body, request);
        boolean existed = ipWhiteService.getByIp(ip) != null;
        IpWhiteEntity entity = ipWhiteService.create(ip, "ops-cert", "ops-cert certId=" + session.getCertId());
        if (entity == null) {
            throw new AException("写入用户白名单失败", 500);
        }
        adminAccessGuard.auditWithIp(request, "ops.ipwhite.add", "ip=" + ip + " certId=" + session.getCertId());
        Map<String, Object> data = ipWhiteRow(entity);
        data.put("success", true);
        data.put("existed", existed);
        return Response.ok(data);
    }

    @PostMapping("/ipwhite/remove")
    public Response<Map<String, Object>> ipWhiteRemove(@RequestBody Map<String, Object> body,
                                                       HttpServletRequest request) {
        OpsSessionService.OpsSession session = requireOpsSession(request);
        String ip = str(body, "ip");
        if (StringUtils.isBlank(ip)) {
            throw new AException("ip 不能为空", 400);
        }
        String current = IPUtils.getIp(request);
        if (StringUtils.isNotBlank(current) && current.equals(ip)) {
            assertClearToken(body);
        }
        ipWhiteService.removeByIp(ip);
        adminAccessGuard.auditWithIp(request, "ops.ipwhite.remove", "ip=" + ip + " certId=" + session.getCertId());
        return Response.ok(Map.of("success", true));
    }

    /** 禁用/启用用户白名单：{@code forbidden=1} 禁用，{@code 0} 启用。 */
    @PostMapping("/ipwhite/setForbidden")
    public Response<Map<String, Object>> ipWhiteSetForbidden(@RequestBody Map<String, Object> body,
                                                             HttpServletRequest request) {
        OpsSessionService.OpsSession session = requireOpsSession(request);
        String ip = str(body, "ip");
        int forbidden = require01(body, "forbidden");
        if (StringUtils.isBlank(ip)) {
            throw new AException("ip 不能为空", 400);
        }
        IpWhiteEntity entity = ipWhiteService.getByIp(ip);
        if (entity == null) {
            throw new AException("白名单IP不存在", 404);
        }
        entity.setForbidden(forbidden);
        entity.setUpdateTime(new Date());
        ipWhiteService.updateById(entity);
        adminAccessGuard.auditWithIp(request, "ops.ipwhite.setForbidden",
                "ip=" + ip + " forbidden=" + forbidden + " certId=" + session.getCertId());
        return Response.ok(Map.of("ip", ip, "forbidden", forbidden, "success", true));
    }

    @PostMapping("/ipblack/list")
    public Response<Map<String, Object>> ipBlackList(@RequestBody(required = false) Map<String, Object> body,
                                                     HttpServletRequest request) {
        requireOpsSession(request);
        Map<String, Object> params = pageParams(body);
        putOptional01(params, body, "available");
        PageUtils page = ipBlackService.queryPage(params);
        List<Map<String, Object>> rows = page.getList().stream()
                .map(o -> ipBlackRow((IpBlackEntity) o))
                .collect(Collectors.toList());
        return Response.ok(pageData(page, rows));
    }

    @PostMapping("/ipblack/add")
    public Response<Map<String, Object>> ipBlackAdd(@RequestBody Map<String, Object> body,
                                                    HttpServletRequest request) {
        OpsSessionService.OpsSession session = requireOpsSession(request);
        String ip = str(body, "ip");
        if (StringUtils.isBlank(ip)) {
            throw new AException("ip 不能为空", 400);
        }
        if (IpBlackService.isPolicyIp(ip)) {
            throw new AException("0.0.0.0 为黑名单策略阈值，请调用 firewallCount 接口", 400);
        }
        boolean existed = ipBlackService.getByIp(ip) != null;
        IpBlackEntity entity = ipBlackService.create(ip, "ops-cert", "ops-cert certId=" + session.getCertId());
        if (entity == null) {
            throw new AException("写入用户黑名单失败", 500);
        }
        adminAccessGuard.auditWithIp(request, "ops.ipblack.add", "ip=" + ip + " certId=" + session.getCertId());
        Map<String, Object> data = ipBlackRow(entity);
        data.put("success", true);
        data.put("existed", existed);
        return Response.ok(data);
    }

    @PostMapping("/ipblack/remove")
    public Response<Map<String, Object>> ipBlackRemove(@RequestBody Map<String, Object> body,
                                                       HttpServletRequest request) {
        OpsSessionService.OpsSession session = requireOpsSession(request);
        String ip = str(body, "ip");
        if (StringUtils.isBlank(ip)) {
            throw new AException("ip 不能为空", 400);
        }
        ipBlackService.removeByIp(ip);
        adminAccessGuard.auditWithIp(request, "ops.ipblack.remove", "ip=" + ip + " certId=" + session.getCertId());
        return Response.ok(Map.of("success", true));
    }

    /**
     * 生效/停用黑名单：与 wall 缓存一致——{@code available=0} 生效拦截，{@code 1} 停用（保留记录）。
     * {@code 0.0.0.0} 为策略哨兵，禁止切换。
     */
    @PostMapping("/ipblack/setAvailable")
    public Response<Map<String, Object>> ipBlackSetAvailable(@RequestBody Map<String, Object> body,
                                                             HttpServletRequest request) {
        OpsSessionService.OpsSession session = requireOpsSession(request);
        String ip = str(body, "ip");
        int available = require01(body, "available");
        if (StringUtils.isBlank(ip)) {
            throw new AException("ip 不能为空", 400);
        }
        ipBlackService.setAvailable(ip, available);
        adminAccessGuard.auditWithIp(request, "ops.ipblack.setAvailable",
                "ip=" + ip + " available=" + available + " certId=" + session.getCertId());
        return Response.ok(Map.of("ip", ip, "available", available, "success", true));
    }

    /** 读取自动拉黑阈值（存于黑名单表策略行 0.0.0.0 的 count）。 */
    @PostMapping("/ipblack/firewallCount/get")
    public Response<Map<String, Object>> ipBlackFirewallCountGet(HttpServletRequest request) {
        requireOpsSession(request);
        return Response.ok(Map.of("firewallCount", ipBlackService.getFirewallCount()));
    }

    /** 设置自动拉黑阈值（持久化到 0.0.0.0 策略行；最小值 500）。 */
    @PostMapping("/ipblack/firewallCount/set")
    public Response<Map<String, Object>> ipBlackFirewallCountSet(@RequestBody Map<String, Object> body,
                                                                 HttpServletRequest request) {
        OpsSessionService.OpsSession session = requireOpsSession(request);
        Object raw = body == null ? null : body.get("count");
        if (raw == null) {
            throw new AException("count 不能为空", 400);
        }
        int count;
        try {
            count = raw instanceof Number ? ((Number) raw).intValue() : Integer.parseInt(String.valueOf(raw).trim());
        } catch (Exception e) {
            throw new AException("count 须为整数", 400);
        }
        ipBlackService.setFirewallCount(count);
        adminAccessGuard.auditWithIp(request, "ops.ipblack.firewallCount",
                "count=" + ipBlackService.getFirewallCount() + " certId=" + session.getCertId());
        return Response.ok(Map.of("firewallCount", ipBlackService.getFirewallCount(), "success", true));
    }

    // —— 盾牌（wall_shield + 运行时开关）——

    @PostMapping("/shield/list")
    public Response<Map<String, Object>> shieldList(@RequestBody(required = false) Map<String, Object> body,
                                                    HttpServletRequest request) {
        requireOpsSession(request);
        PageUtils page = shieldService.queryPage(pageParams(body));
        List<Map<String, Object>> rows = page.getList().stream().map(o -> {
            ShieldEntity e = (ShieldEntity) o;
            Map<String, Object> m = new HashMap<>();
            m.put("id", e.getId());
            m.put("uri", e.getUri());
            m.put("enable", e.isEnable());
            m.put("auto", e.getAuto());
            return m;
        }).collect(Collectors.toList());
        return Response.ok(pageData(page, rows));
    }

    @PostMapping("/shield/save")
    public Response<Map<String, Object>> shieldSave(@RequestBody Map<String, Object> body,
                                                    HttpServletRequest request) {
        OpsSessionService.OpsSession session = requireOpsSession(request);
        String uri = str(body, "uri");
        if (StringUtils.isBlank(uri)) {
            throw new AException("uri 不能为空", 400);
        }
        if (shieldService.getOne(new QueryWrapper<ShieldEntity>().eq("uri", uri), false) != null) {
            throw new AException("该资源已存在", 409);
        }
        int auto = parsePositiveInt(body == null ? null : body.get("auto"), 10000);
        if (auto < 1000) {
            throw new AException("auto 须不小于 1000", 400);
        }
        ShieldEntity entity = new ShieldEntity();
        entity.setUri(uri);
        entity.setEnable(boolVal(body, "enable", true));
        entity.setAuto(auto);
        shieldService.save(entity);
        shieldService.clear();
        adminAccessGuard.auditWithIp(request, "ops.shield.save",
                "uri=" + uri + " certId=" + session.getCertId());
        return Response.ok(shieldRow(entity));
    }

    @PostMapping("/shield/update")
    public Response<Map<String, Object>> shieldUpdate(@RequestBody Map<String, Object> body,
                                                      HttpServletRequest request) {
        OpsSessionService.OpsSession session = requireOpsSession(request);
        Long id = requireLong(body, "id");
        ShieldEntity entity = shieldService.getById(id);
        if (entity == null) {
            throw new AException("盾牌规则不存在", 404);
        }
        String uri = str(body, "uri");
        if (StringUtils.isNotBlank(uri) && !uri.equals(entity.getUri())) {
            ShieldEntity dup = shieldService.getOne(new QueryWrapper<ShieldEntity>().eq("uri", uri), false);
            if (dup != null && !dup.getId().equals(id)) {
                throw new AException("该资源已存在", 409);
            }
            entity.setUri(uri);
        }
        if (body != null && body.containsKey("enable")) {
            entity.setEnable(boolVal(body, "enable", entity.isEnable()));
        }
        if (body != null && body.containsKey("auto")) {
            int auto = parsePositiveInt(body.get("auto"), entity.getAuto());
            if (auto < 1000) {
                throw new AException("auto 须不小于 1000", 400);
            }
            entity.setAuto(auto);
        }
        shieldService.updateById(entity);
        shieldService.clear();
        adminAccessGuard.auditWithIp(request, "ops.shield.update",
                "id=" + id + " certId=" + session.getCertId());
        return Response.ok(shieldRow(entity));
    }

    @PostMapping("/shield/delete")
    public Response<Map<String, Object>> shieldDelete(@RequestBody Map<String, Object> body,
                                                      HttpServletRequest request) {
        OpsSessionService.OpsSession session = requireOpsSession(request);
        List<Long> ids = requireIds(body);
        shieldService.removeBatchByIds(ids);
        shieldService.clear();
        adminAccessGuard.auditWithIp(request, "ops.shield.delete",
                "ids=" + ids + " certId=" + session.getCertId());
        return Response.ok(Map.of("success", true));
    }

    @PostMapping("/shield/status")
    public Response<Map<String, Object>> shieldStatus(HttpServletRequest request) {
        requireOpsSession(request);
        Map<String, Object> data = new HashMap<>();
        data.put("attack", ShieldService.getAttack());
        data.put("print", ShieldService.getPrint());
        data.put("enable", shieldService.getEnable());
        return Response.ok(data);
    }

    @PostMapping("/shield/setAttack")
    public Response<Map<String, Object>> shieldSetAttack(@RequestBody Map<String, Object> body,
                                                         HttpServletRequest request) {
        OpsSessionService.OpsSession session = requireOpsSession(request);
        boolean attack = requireBool(body, "attack");
        ShieldService.setAttack(attack);
        adminAccessGuard.auditWithIp(request, "ops.shield.setAttack",
                "attack=" + attack + " certId=" + session.getCertId());
        return shieldStatus(request);
    }

    @PostMapping("/shield/setPrint")
    public Response<Map<String, Object>> shieldSetPrint(@RequestBody Map<String, Object> body,
                                                        HttpServletRequest request) {
        OpsSessionService.OpsSession session = requireOpsSession(request);
        boolean print = requireBool(body, "print");
        ShieldService.setPrint(print);
        adminAccessGuard.auditWithIp(request, "ops.shield.setPrint",
                "print=" + print + " certId=" + session.getCertId());
        return shieldStatus(request);
    }

    @PostMapping("/shield/setEnable")
    public Response<Map<String, Object>> shieldSetEnable(@RequestBody Map<String, Object> body,
                                                         HttpServletRequest request) {
        OpsSessionService.OpsSession session = requireOpsSession(request);
        boolean enable = requireBool(body, "enable");
        shieldService.setEnable(enable);
        adminAccessGuard.auditWithIp(request, "ops.shield.setEnable",
                "enable=" + enable + " certId=" + session.getCertId());
        return shieldStatus(request);
    }

    // —— 跳转（wall_jump）——

    @PostMapping("/jump/list")
    public Response<Map<String, Object>> jumpList(@RequestBody(required = false) Map<String, Object> body,
                                                  HttpServletRequest request) {
        requireOpsSession(request);
        PageUtils page = jumpService.queryPage(pageParams(body));
        List<Map<String, Object>> rows = page.getList().stream().map(o -> {
            JumpEntity e = (JumpEntity) o;
            Map<String, Object> m = new HashMap<>();
            m.put("id", e.getId());
            m.put("host", e.getHost());
            m.put("uri", e.getUri());
            m.put("url", e.getUrl());
            m.put("enable", e.isEnable());
            return m;
        }).collect(Collectors.toList());
        return Response.ok(pageData(page, rows));
    }

    @PostMapping("/jump/save")
    public Response<Map<String, Object>> jumpSave(@RequestBody Map<String, Object> body,
                                                  HttpServletRequest request) {
        OpsSessionService.OpsSession session = requireOpsSession(request);
        String host = str(body, "host");
        String uri = str(body, "uri");
        String url = str(body, "url");
        if (StringUtils.isBlank(host) || StringUtils.isBlank(uri) || StringUtils.isBlank(url)) {
            throw new AException("host、uri、url 均不能为空", 400);
        }
        if (jumpService.getOne(new QueryWrapper<JumpEntity>()
                .eq("host", host).eq("uri", uri), false) != null) {
            throw new AException("该域名与资源组合已存在", 409);
        }
        JumpEntity entity = new JumpEntity();
        entity.setHost(host);
        entity.setUri(uri);
        entity.setUrl(url);
        entity.setEnable(boolVal(body, "enable", true));
        jumpService.save(entity);
        jumpService.clear();
        adminAccessGuard.auditWithIp(request, "ops.jump.save",
                "host=" + host + " uri=" + uri + " certId=" + session.getCertId());
        return Response.ok(jumpRow(entity));
    }

    @PostMapping("/jump/update")
    public Response<Map<String, Object>> jumpUpdate(@RequestBody Map<String, Object> body,
                                                    HttpServletRequest request) {
        OpsSessionService.OpsSession session = requireOpsSession(request);
        Long id = requireLong(body, "id");
        JumpEntity entity = jumpService.getById(id);
        if (entity == null) {
            throw new AException("跳转规则不存在", 404);
        }
        String host = StringUtils.isNotBlank(str(body, "host")) ? str(body, "host") : entity.getHost();
        String uri = StringUtils.isNotBlank(str(body, "uri")) ? str(body, "uri") : entity.getUri();
        if (!host.equals(entity.getHost()) || !uri.equals(entity.getUri())) {
            JumpEntity dup = jumpService.getOne(new QueryWrapper<JumpEntity>()
                    .eq("host", host).eq("uri", uri), false);
            if (dup != null && !dup.getId().equals(id)) {
                throw new AException("该域名与资源组合已存在", 409);
            }
        }
        entity.setHost(host);
        entity.setUri(uri);
        if (StringUtils.isNotBlank(str(body, "url"))) {
            entity.setUrl(str(body, "url"));
        }
        if (body != null && body.containsKey("enable")) {
            entity.setEnable(boolVal(body, "enable", entity.isEnable()));
        }
        if (StringUtils.isBlank(entity.getUrl())) {
            throw new AException("url 不能为空", 400);
        }
        jumpService.updateById(entity);
        jumpService.clear();
        adminAccessGuard.auditWithIp(request, "ops.jump.update",
                "id=" + id + " certId=" + session.getCertId());
        return Response.ok(jumpRow(entity));
    }

    @PostMapping("/jump/delete")
    public Response<Map<String, Object>> jumpDelete(@RequestBody Map<String, Object> body,
                                                    HttpServletRequest request) {
        OpsSessionService.OpsSession session = requireOpsSession(request);
        List<Long> ids = requireIds(body);
        jumpService.removeBatchByIds(ids);
        jumpService.clear();
        adminAccessGuard.auditWithIp(request, "ops.jump.delete",
                "ids=" + ids + " certId=" + session.getCertId());
        return Response.ok(Map.of("success", true));
    }

    @PostMapping("/gate/get")
    public Response<Map<String, Object>> gateGet(HttpServletRequest request) {
        requireOpsSession(request);
        SecurityOpsConfig ops = sysConfigService.getSecurityOpsConfig();
        boolean clearTokenConfigured = ops != null && StringUtils.isNotBlank(ops.getClearToken());
        Map<String, Object> data = new HashMap<>();
        data.put("adminIpEnable", sysConfigService.isAdminIpEnable());
        data.put("discoverEnabled", opsCertService.isDiscoverEnabled());
        // 仅暴露是否已配置，不下发令牌明文
        data.put("clearTokenConfigured", clearTokenConfigured);
        return Response.ok(data);
    }

    /**
     * 运行状态快照：JVM/磁盘/库连接与表数/防火墙与攻击线索/访问与 Redis 等分段聚合。
     */
    @PostMapping("/runtime/snapshot")
    public Response<Map<String, Object>> runtimeSnapshot(HttpServletRequest request) {
        OpsSessionService.OpsSession session = requireOpsSession(request);
        Map<String, Object> data = opsRuntimeSnapshotService.snapshot();
        adminAccessGuard.auditWithIp(request, "ops.runtime.snapshot",
                "certId=" + session.getCertId());
        return Response.ok(data);
    }

    /** 校验清除确认令牌是否正确（无副作用，供运维 App 保存前核对）。 */
    @PostMapping("/gate/verifyClearToken")
    public Response<Map<String, Object>> gateVerifyClearToken(@RequestBody Map<String, Object> body,
                                                             HttpServletRequest request) {
        requireOpsSession(request);
        assertClearToken(body);
        return Response.ok(Map.of("valid", true));
    }

    /**
     * 运维 App 配置/修改清除确认令牌（不下发明文）。
     * <ul>
     *   <li>服务端尚未配置：已认证运维会话可直接设置 {@code clearToken}</li>
     *   <li>已配置：须提供正确 {@code oldClearToken}，再写入新 {@code clearToken}</li>
     * </ul>
     */
    @PostMapping("/gate/setClearToken")
    public Response<Map<String, Object>> gateSetClearToken(@RequestBody Map<String, Object> body,
                                                          HttpServletRequest request) {
        OpsSessionService.OpsSession session = requireOpsSession(request);
        String next = str(body, "clearToken");
        if (StringUtils.isBlank(next)) {
            throw new AException("请填写清除确认令牌", 400);
        }
        if (next.length() < 4 || next.length() > 256) {
            throw new AException("清除确认令牌长度须为 4～256", 400);
        }
        if (next.chars().anyMatch(Character::isWhitespace)) {
            throw new AException("清除确认令牌不能包含空白字符", 400);
        }
        SecurityOpsConfig current = sysConfigService.getSecurityOpsConfig();
        if (current == null) {
            current = new SecurityOpsConfig();
        }
        String configured = current.getClearToken();
        boolean firstTime = StringUtils.isBlank(configured);
        if (!firstTime) {
            String old = str(body, "oldClearToken");
            if (StringUtils.isBlank(old)) {
                old = str(body, "clearTokenOld");
            }
            if (StringUtils.isBlank(old) || !configured.equals(old)) {
                throw new AException("原清除确认令牌不正确", 403);
            }
            if (configured.equals(next)) {
                return Response.ok(Map.of("clearTokenConfigured", true, "changed", false));
            }
        }
        SecurityOpsConfig toSave = new SecurityOpsConfig();
        toSave.setClearToken(next);
        toSave.setReinitToken(current.getReinitToken());
        toSave.setBeanExecuteEnabled(current.isBeanExecuteEnabled());
        toSave.setWebShellEnabled(current.isWebShellEnabled());
        toSave.setAdminIpEnable(current.isAdminIpEnable());
        sysConfigService.saveSecurityOpsConfig(toSave);
        adminAccessGuard.auditWithIp(request, "ops.gate.setClearToken",
                "firstTime=" + firstTime + " certId=" + session.getCertId());
        return Response.ok(Map.of("clearTokenConfigured", true, "changed", true));
    }

    @PostMapping("/gate/setAdminIpEnable")
    public Response<Map<String, Object>> gateSetAdminIpEnable(@RequestBody Map<String, Object> body,
                                                              HttpServletRequest request) {
        OpsSessionService.OpsSession session = requireOpsSession(request);
        boolean enable = parseEnableFlag(body.get("enable"), str(body, "enable"));
        if (!enable) {
            assertClearToken(body);
            persistAdminIpEnable(false);
        } else {
            enableAdminIpGateAfterOpsCertBind(request, session.getCertId(), "ops-enable");
        }
        adminAccessGuard.auditWithIp(request, "ops.gate.adminIpEnable",
                "enable=" + enable + " certId=" + session.getCertId());
        return Response.ok(Map.of("adminIpEnable", sysConfigService.isAdminIpEnable()));
    }

    /**
     * 开关公开发现页 {@code /ops/discover}（不要求 clearToken）。
     */
    @PostMapping("/gate/setDiscoverEnabled")
    public Response<Map<String, Object>> gateSetDiscoverEnabled(@RequestBody Map<String, Object> body,
                                                                HttpServletRequest request) {
        OpsSessionService.OpsSession session = requireOpsSession(request);
        boolean enable = parseEnableFlag(body.get("enable"), str(body, "enable"));
        opsCertService.setDiscoverEnabled(enable);
        adminAccessGuard.auditWithIp(request, "ops.gate.discoverEnabled",
                "enable=" + enable + " certId=" + session.getCertId());
        return Response.ok(Map.of("discoverEnabled", opsCertService.isDiscoverEnabled()));
    }

    @PostMapping("/cert/list")
    public Response<Map<String, Object>> certList(HttpServletRequest request) {
        OpsSessionService.OpsSession session = requireOpsSession(request);
        List<Map<String, Object>> list = opsCertService.listClients().stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("certId", c.getCertId());
            m.put("label", c.getLabel());
            m.put("revoked", c.isRevoked());
            m.put("createdAt", c.getCreatedAt());
            m.put("lastUsedAt", c.getLastUsedAt());
            m.put("self", session.getCertId() != null && session.getCertId().equals(c.getCertId()));
            return m;
        }).collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("activeCount", opsCertService.countActiveClients());
        return Response.ok(data);
    }

    /**
     * 已登录设备签发一次性认领码，供新设备 {@code /cert/claim} 入驻（私钥仅新设备本地生成）。
     */
    @PostMapping("/cert/issue")
    public Response<Map<String, Object>> certIssue(@RequestBody(required = false) Map<String, Object> body,
                                                   HttpServletRequest request) {
        OpsSessionService.OpsSession session = requireOpsSession(request);
        String label = body == null ? "" : str(body, "label");
        Map<String, Object> data = opsCertService.issueClaim(session.getCertId(), label);
        adminAccessGuard.auditWithIp(request, "ops.cert.issue",
                "claimId=" + data.get("claimId") + " by=" + session.getCertId());
        return Response.ok(data);
    }

    /**
     * 新设备凭认领码提交公钥；响应与 enroll 同形（不下发私钥）。
     * 成功后同样自动开启管理 IP 校验并认领当前访问 IP（幂等）。
     */
    @PostMapping("/cert/claim")
    public Response<Map<String, Object>> certClaim(@RequestBody Map<String, Object> body,
                                                   HttpServletRequest request) {
        String claimSecret = str(body, "claimSecret");
        if (StringUtils.isBlank(claimSecret)) {
            claimSecret = str(body, "secret");
        }
        String clientPublicKey = str(body, "clientPublicKey");
        String label = str(body, "label");
        OpsClientCertEntry entry = opsCertService.claim(claimSecret, clientPublicKey, label);
        enableAdminIpGateAfterOpsCertBind(request, entry.getCertId(), "ops-claim");
        SecurityOpsCertConfig cfg = opsCertService.getConfig();
        Map<String, Object> data = new HashMap<>();
        data.put("certId", entry.getCertId());
        data.put("siteId", cfg.getSiteId());
        data.put("serverPublicKey", cfg.getServerPublicKey());
        data.put("serverFingerprint", cfg.getFingerprint());
        data.put("adminIpEnable", sysConfigService.isAdminIpEnable());
        data.put("discoverEnabled", opsCertService.isDiscoverEnabled());
        return Response.ok(data);
    }

    @PostMapping("/cert/revoke")
    public Response<Map<String, Object>> certRevoke(@RequestBody Map<String, Object> body,
                                                    HttpServletRequest request) {
        OpsSessionService.OpsSession session = requireOpsSession(request);
        String certId = str(body, "certId");
        Map<String, Object> result = opsCertService.revoke(certId);
        adminAccessGuard.auditWithIp(request, "ops.cert.revoke",
                "target=" + certId + " by=" + session.getCertId()
                        + " lastCert=" + result.get("lastCert"));
        return Response.ok(result);
    }

    @PostMapping("/reset")
    public Response<Map<String, Object>> reset(@RequestBody Map<String, Object> body,
                                               HttpServletRequest request) {
        // Reset requires logged-in system admin + clearToken; OpsEnroll reissued, RSA deferred.
        assertClearToken(body);
        adminAccessGuard.requireSystemAdmin("opsReset");
        SecurityOpsCertConfig cfg = opsCertService.reset();
        adminAccessGuard.auditWithIp(request, "ops.cert.reset", "fingerprint=" + cfg.getFingerprint());
        Map<String, Object> data = new HashMap<>();
        data.put("siteId", cfg.getSiteId());
        data.put("serverFingerprint", cfg.getFingerprint());
        data.put("enrollAvailable", true);
        data.put("discoverEnabled", opsCertService.isDiscoverEnabled());
        data.put("hint", "请查看服务器日志中的入驻口令：OpsEnroll secret=");
        return Response.ok(data);
    }

    private void assertProbe(HttpServletRequest request) {
        String token = request.getHeader(AutumnOpsProbe.HEADER);
        if (!AutumnOpsProbe.TOKEN.equals(token)) {
            throw new AException("探测 Token 无效", 403);
        }
    }

    private OpsSessionService.OpsSession requireOpsSession(HttpServletRequest request) {
        String sid = request.getHeader(AutumnOpsProbe.SESSION_HEADER);
        return opsSessionService.requireSession(sid);
    }

    private void assertClearToken(Map<String, Object> body) {
        String clearToken = str(body, "clearToken");
        SecurityOpsConfig ops = sysConfigService.getSecurityOpsConfig();
        String configured = ops != null ? ops.getClearToken() : null;
        // 空配置 = 拒绝一切依赖 clearToken 的危险操作（与 SECURITY_OPS_CONFIG 语义一致）
        if (StringUtils.isBlank(configured)) {
            throw new AException("服务端尚未配置清除确认令牌", 403);
        }
        if (StringUtils.isBlank(clearToken)) {
            throw new AException("请填写清除确认令牌", 400);
        }
        if (!configured.equals(clearToken)) {
            throw new AException("清除确认令牌不正确", 403);
        }
    }

    /**
     * App 运维证书入驻/认领成功后：开启管理 IP 校验，并将当前访问 IP 写入管理白名单（幂等）。
     */
    private void enableAdminIpGateAfterOpsCertBind(HttpServletRequest request, String certId, String tag) {
        if (!sysConfigService.isAdminIpEnable()) {
            persistAdminIpEnable(true);
            adminAccessGuard.auditWithIp(request, "ops.gate.adminIpEnable.auto",
                    "enable=true tag=" + tag + " certId=" + StringUtils.defaultString(certId));
        }
        String ip = IPUtils.getIp(request);
        if (StringUtils.isNotBlank(ip) && !ipAdminWhiteService.isAdminWhite(ip)) {
            ipAdminWhiteService.create(ip, tag, "运维证书对接后自动认领管理IP");
        }
    }

    private void persistAdminIpEnable(boolean enable) {
        SecurityOpsConfig current = sysConfigService.getSecurityOpsConfig();
        if (current == null) {
            current = new SecurityOpsConfig();
        }
        SecurityOpsConfig toSave = new SecurityOpsConfig();
        toSave.setClearToken(current.getClearToken());
        toSave.setReinitToken(current.getReinitToken());
        toSave.setBeanExecuteEnabled(current.isBeanExecuteEnabled());
        toSave.setWebShellEnabled(current.isWebShellEnabled());
        toSave.setAdminIpEnable(enable);
        sysConfigService.saveSecurityOpsConfig(toSave);
    }

    private String resolveIp(Map<String, Object> body, HttpServletRequest request) {
        String ip = str(body, "ip");
        boolean claimCurrent = parseEnableFlag(body.get("claimCurrent"), str(body, "claimCurrent"));
        if (claimCurrent || StringUtils.isBlank(ip)) {
            ip = IPUtils.getIp(request);
        }
        if (StringUtils.isBlank(ip)) {
            throw new AException("ip 不能为空", 400);
        }
        return ip;
    }

    /** 解析开关：true/false、1/0、"true"/"false"（大小写不敏感）。其余视为 false。 */
    private static boolean parseEnableFlag(Object raw, String asString) {
        if (Boolean.TRUE.equals(raw)) {
            return true;
        }
        if (raw instanceof Number && ((Number) raw).intValue() == 1) {
            return true;
        }
        if (StringUtils.isBlank(asString)) {
            return false;
        }
        String s = asString.trim();
        return "true".equalsIgnoreCase(s) || "1".equals(s);
    }

    /** 规范化分页：page 从 1，limit 默认 20、最大 100。 */
    private static Map<String, Object> pageParams(Map<String, Object> body) {
        Map<String, Object> params = new HashMap<>();
        if (body != null) {
            params.putAll(body);
        }
        int page = parsePositiveInt(params.get("page"), 1);
        int limit = parsePositiveInt(params.get("limit"), 20);
        if (limit > 100) {
            limit = 100;
        }
        params.put("page", String.valueOf(page));
        params.put("limit", String.valueOf(limit));
        return params;
    }

    private static Map<String, Object> pageData(PageUtils page, List<Map<String, Object>> rows) {
        int pageNo = page.getCurrPage();
        int limit = page.getPageSize();
        int count = page.getTotalCount();
        boolean hasMore = (long) pageNo * limit < count;
        Map<String, Object> data = new HashMap<>();
        data.put("count", count);
        data.put("page", pageNo);
        data.put("limit", limit);
        data.put("hasMore", hasMore);
        data.put("list", rows);
        return data;
    }

    private static int require01(Map<String, Object> body, String key) {
        Object raw = body == null ? null : body.get(key);
        if (raw == null) {
            throw new AException(key + " 不能为空", 400);
        }
        return parse01(raw, key);
    }

    /** list 可选过滤：body 含 key 时写入 params（0/1）；缺省不筛。 */
    private static void putOptional01(Map<String, Object> params, Map<String, Object> body, String key) {
        if (body == null || !body.containsKey(key) || body.get(key) == null) {
            return;
        }
        Object raw = body.get(key);
        if (raw instanceof String && StringUtils.isBlank((String) raw)) {
            return;
        }
        params.put(key, parse01(raw, key));
    }

    private static int parse01(Object raw, String key) {
        int v;
        if (raw instanceof Number) {
            v = ((Number) raw).intValue();
        } else {
            try {
                v = Integer.parseInt(String.valueOf(raw).trim());
            } catch (NumberFormatException e) {
                throw new AException(key + " 须为 0 或 1", 400);
            }
        }
        if (v != 0 && v != 1) {
            throw new AException(key + " 须为 0 或 1", 400);
        }
        return v;
    }

    private static Map<String, Object> adminIpRow(IpAdminWhiteEntity e) {
        Map<String, Object> m = new HashMap<>();
        m.put("ip", e.getIp());
        m.put("tag", e.getTag());
        m.put("description", e.getDescription());
        m.put("forbidden", e.getForbidden() == null ? 0 : e.getForbidden());
        m.put("createTime", e.getCreateTime() == null ? null : e.getCreateTime().getTime());
        return m;
    }

    private static Map<String, Object> ipWhiteRow(IpWhiteEntity e) {
        Map<String, Object> m = new HashMap<>();
        m.put("ip", e.getIp());
        m.put("tag", e.getTag());
        m.put("description", e.getDescription());
        m.put("forbidden", e.getForbidden() == null ? 0 : e.getForbidden());
        m.put("createTime", e.getCreateTime() == null ? null : e.getCreateTime().getTime());
        return m;
    }

    private static Map<String, Object> ipBlackRow(IpBlackEntity e) {
        Map<String, Object> m = new HashMap<>();
        m.put("ip", e.getIp());
        m.put("tag", e.getTag());
        m.put("description", e.getDescription());
        m.put("available", e.getAvailable() == null ? 0 : e.getAvailable());
        m.put("createTime", e.getCreateTime() == null ? null : e.getCreateTime().getTime());
        return m;
    }

    private static int parsePositiveInt(Object raw, int defaultValue) {
        if (raw == null) {
            return defaultValue;
        }
        try {
            int v = raw instanceof Number
                    ? ((Number) raw).intValue()
                    : Integer.parseInt(String.valueOf(raw).trim());
            return v < 1 ? defaultValue : v;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static String str(Map<String, Object> body, String key) {
        if (body == null || !body.containsKey(key) || body.get(key) == null) {
            return "";
        }
        return String.valueOf(body.get(key)).trim();
    }

    private static Map<String, Object> shieldRow(ShieldEntity e) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", e.getId());
        m.put("uri", e.getUri());
        m.put("enable", e.isEnable());
        m.put("auto", e.getAuto());
        m.put("success", true);
        return m;
    }

    private static Map<String, Object> jumpRow(JumpEntity e) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", e.getId());
        m.put("host", e.getHost());
        m.put("uri", e.getUri());
        m.put("url", e.getUrl());
        m.put("enable", e.isEnable());
        m.put("success", true);
        return m;
    }

    private static boolean boolVal(Map<String, Object> body, String key, boolean defaultValue) {
        if (body == null || !body.containsKey(key) || body.get(key) == null) {
            return defaultValue;
        }
        Object raw = body.get(key);
        if (raw instanceof Boolean) {
            return (Boolean) raw;
        }
        if (raw instanceof Number) {
            return ((Number) raw).intValue() != 0;
        }
        String s = String.valueOf(raw).trim();
        if ("true".equalsIgnoreCase(s) || "1".equals(s)) {
            return true;
        }
        if ("false".equalsIgnoreCase(s) || "0".equals(s)) {
            return false;
        }
        return defaultValue;
    }

    private static boolean requireBool(Map<String, Object> body, String key) {
        if (body == null || !body.containsKey(key) || body.get(key) == null) {
            throw new AException(key + " 不能为空", 400);
        }
        return boolVal(body, key, false);
    }

    private static Long requireLong(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null) {
            throw new AException(key + " 不能为空", 400);
        }
        Object raw = body.get(key);
        try {
            if (raw instanceof Number) {
                return ((Number) raw).longValue();
            }
            return Long.parseLong(String.valueOf(raw).trim());
        } catch (NumberFormatException e) {
            throw new AException(key + " 须为数字", 400);
        }
    }

    private static List<Long> requireIds(Map<String, Object> body) {
        if (body == null || body.get("ids") == null) {
            throw new AException("ids 不能为空", 400);
        }
        Object raw = body.get("ids");
        List<Long> ids = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                if (o == null) {
                    continue;
                }
                if (o instanceof Number) {
                    ids.add(((Number) o).longValue());
                } else {
                    try {
                        ids.add(Long.parseLong(String.valueOf(o).trim()));
                    } catch (NumberFormatException e) {
                        throw new AException("ids 须为数字数组", 400);
                    }
                }
            }
        } else if (raw instanceof Number) {
            ids.add(((Number) raw).longValue());
        } else {
            throw new AException("ids 须为数组", 400);
        }
        if (ids.isEmpty()) {
            throw new AException("ids 不能为空", 400);
        }
        return ids;
    }
}
