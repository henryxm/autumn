package cn.org.autumn.modules.ops.controller;

import cn.org.autumn.exception.AException;
import cn.org.autumn.model.OpsClientCertEntry;
import cn.org.autumn.model.Response;
import cn.org.autumn.model.SecurityOpsCertConfig;
import cn.org.autumn.model.SecurityOpsConfig;
import cn.org.autumn.modules.ops.service.OpsCertService;
import cn.org.autumn.modules.ops.service.OpsSessionService;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.sys.support.AdminAccessGuard;
import cn.org.autumn.modules.wall.entity.IpAdminWhiteEntity;
import cn.org.autumn.modules.wall.entity.IpBlackEntity;
import cn.org.autumn.modules.wall.entity.IpWhiteEntity;
import cn.org.autumn.modules.wall.service.IpAdminWhiteService;
import cn.org.autumn.modules.wall.service.IpBlackService;
import cn.org.autumn.modules.wall.service.IpWhiteService;
import cn.org.autumn.ops.AutumnOpsProbe;
import cn.org.autumn.utils.IPUtils;
import cn.org.autumn.utils.PageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private AdminAccessGuard adminAccessGuard;

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
        SecurityOpsCertConfig cfg = opsCertService.getConfig();
        Map<String, Object> data = new HashMap<>();
        data.put("certId", entry.getCertId());
        data.put("siteId", cfg.getSiteId());
        data.put("serverPublicKey", cfg.getServerPublicKey());
        data.put("serverFingerprint", cfg.getFingerprint());
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
        Map<String, Object> params = body != null ? body : Map.of();
        PageUtils page = ipAdminWhiteService.queryPage(params);
        List<Map<String, Object>> rows = page.getList().stream().map(o -> {
            IpAdminWhiteEntity e = (IpAdminWhiteEntity) o;
            Map<String, Object> m = new HashMap<>();
            m.put("ip", e.getIp());
            m.put("tag", e.getTag());
            m.put("description", e.getDescription());
            m.put("forbidden", e.getForbidden());
            return m;
        }).collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("count", page.getTotalCount());
        data.put("list", rows);
        return Response.ok(data);
    }

    @PostMapping("/adminip/add")
    public Response<Map<String, Object>> adminIpAdd(@RequestBody Map<String, Object> body,
                                                    HttpServletRequest request) {
        OpsSessionService.OpsSession session = requireOpsSession(request);
        String ip = resolveIp(body, request);
        IpAdminWhiteEntity entity = ipAdminWhiteService.create(ip, "ops-cert", "ops-cert certId=" + session.getCertId());
        if (entity == null) {
            throw new AException("写入管理IP失败", 500);
        }
        adminAccessGuard.auditWithIp(request, "ops.adminip.add", "ip=" + ip + " certId=" + session.getCertId());
        return Response.ok(Map.of("ip", entity.getIp(), "success", true));
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

    @PostMapping("/currentIp")
    public Response<Map<String, Object>> currentIp(HttpServletRequest request) {
        requireOpsSession(request);
        return Response.ok(Map.of("ip", StringUtils.defaultString(IPUtils.getIp(request))));
    }

    @PostMapping("/ipwhite/list")
    public Response<Map<String, Object>> ipWhiteList(@RequestBody(required = false) Map<String, Object> body,
                                                     HttpServletRequest request) {
        requireOpsSession(request);
        Map<String, Object> params = body != null ? body : Map.of();
        PageUtils page = ipWhiteService.queryPage(params);
        List<Map<String, Object>> rows = page.getList().stream().map(o -> {
            IpWhiteEntity e = (IpWhiteEntity) o;
            Map<String, Object> m = new HashMap<>();
            m.put("ip", e.getIp());
            m.put("tag", e.getTag());
            m.put("description", e.getDescription());
            m.put("forbidden", e.getForbidden());
            return m;
        }).collect(Collectors.toList());
        return Response.ok(Map.of("count", page.getTotalCount(), "list", rows));
    }

    @PostMapping("/ipwhite/add")
    public Response<Map<String, Object>> ipWhiteAdd(@RequestBody Map<String, Object> body,
                                                    HttpServletRequest request) {
        OpsSessionService.OpsSession session = requireOpsSession(request);
        String ip = resolveIp(body, request);
        IpWhiteEntity entity = ipWhiteService.create(ip, "ops-cert", "ops-cert certId=" + session.getCertId());
        if (entity == null) {
            throw new AException("写入用户白名单失败", 500);
        }
        adminAccessGuard.auditWithIp(request, "ops.ipwhite.add", "ip=" + ip + " certId=" + session.getCertId());
        return Response.ok(Map.of("ip", entity.getIp(), "success", true));
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

    @PostMapping("/ipblack/list")
    public Response<Map<String, Object>> ipBlackList(@RequestBody(required = false) Map<String, Object> body,
                                                     HttpServletRequest request) {
        requireOpsSession(request);
        Map<String, Object> params = body != null ? body : Map.of();
        PageUtils page = ipBlackService.queryPage(params);
        List<Map<String, Object>> rows = page.getList().stream().map(o -> {
            IpBlackEntity e = (IpBlackEntity) o;
            Map<String, Object> m = new HashMap<>();
            m.put("ip", e.getIp());
            m.put("tag", e.getTag());
            m.put("description", e.getDescription());
            m.put("available", e.getAvailable());
            return m;
        }).collect(Collectors.toList());
        return Response.ok(Map.of("count", page.getTotalCount(), "list", rows));
    }

    @PostMapping("/ipblack/add")
    public Response<Map<String, Object>> ipBlackAdd(@RequestBody Map<String, Object> body,
                                                    HttpServletRequest request) {
        OpsSessionService.OpsSession session = requireOpsSession(request);
        String ip = str(body, "ip");
        if (StringUtils.isBlank(ip)) {
            throw new AException("ip 不能为空", 400);
        }
        IpBlackEntity entity = ipBlackService.create(ip, "ops-cert", "ops-cert certId=" + session.getCertId());
        if (entity == null) {
            throw new AException("写入用户黑名单失败", 500);
        }
        adminAccessGuard.auditWithIp(request, "ops.ipblack.add", "ip=" + ip + " certId=" + session.getCertId());
        return Response.ok(Map.of("ip", entity.getIp(), "success", true));
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

    @PostMapping("/gate/get")
    public Response<Map<String, Object>> gateGet(HttpServletRequest request) {
        requireOpsSession(request);
        Map<String, Object> data = new HashMap<>();
        data.put("adminIpEnable", sysConfigService.isAdminIpEnable());
        return Response.ok(data);
    }

    @PostMapping("/gate/setAdminIpEnable")
    public Response<Map<String, Object>> gateSetAdminIpEnable(@RequestBody Map<String, Object> body,
                                                              HttpServletRequest request) {
        OpsSessionService.OpsSession session = requireOpsSession(request);
        boolean enable = Boolean.TRUE.equals(body.get("enable"))
                || "true".equalsIgnoreCase(str(body, "enable"));
        if (!enable) {
            assertClearToken(body);
        }
        SecurityOpsConfig current = sysConfigService.getSecurityOpsConfig();
        SecurityOpsConfig toSave = new SecurityOpsConfig();
        toSave.setClearToken(current.getClearToken());
        toSave.setReinitToken(current.getReinitToken());
        toSave.setBeanExecuteEnabled(current.isBeanExecuteEnabled());
        toSave.setWebShellEnabled(current.isWebShellEnabled());
        toSave.setAdminIpEnable(enable);
        sysConfigService.saveSecurityOpsConfig(toSave);
        if (enable) {
            String ip = IPUtils.getIp(request);
            if (StringUtils.isNotBlank(ip) && !ipAdminWhiteService.isAdminWhite(ip)) {
                ipAdminWhiteService.create(ip, "ops-enable", "运维证书开启管理IP时认领");
            }
        }
        adminAccessGuard.auditWithIp(request, "ops.gate.adminIpEnable",
                "enable=" + enable + " certId=" + session.getCertId());
        return Response.ok(Map.of("adminIpEnable", sysConfigService.isAdminIpEnable()));
    }

    @PostMapping("/cert/list")
    public Response<Map<String, Object>> certList(HttpServletRequest request) {
        requireOpsSession(request);
        List<Map<String, Object>> list = opsCertService.listClients().stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("certId", c.getCertId());
            m.put("label", c.getLabel());
            m.put("revoked", c.isRevoked());
            m.put("createdAt", c.getCreatedAt());
            m.put("lastUsedAt", c.getLastUsedAt());
            return m;
        }).collect(Collectors.toList());
        return Response.ok(Map.of("list", list));
    }

    @PostMapping("/cert/revoke")
    public Response<Map<String, Object>> certRevoke(@RequestBody Map<String, Object> body,
                                                    HttpServletRequest request) {
        OpsSessionService.OpsSession session = requireOpsSession(request);
        String certId = str(body, "certId");
        boolean ok = opsCertService.revoke(certId);
        adminAccessGuard.auditWithIp(request, "ops.cert.revoke",
                "target=" + certId + " by=" + session.getCertId());
        return Response.ok(Map.of("success", ok));
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
        data.put("hint", "Check server log for OpsEnroll secret=");
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
        if (StringUtils.isBlank(configured) || !configured.equals(clearToken)) {
            throw new AException("须提供正确 clearToken", 403);
        }
    }

    private String resolveIp(Map<String, Object> body, HttpServletRequest request) {
        String ip = str(body, "ip");
        boolean claimCurrent = Boolean.TRUE.equals(body.get("claimCurrent"))
                || "true".equalsIgnoreCase(str(body, "claimCurrent"));
        if (claimCurrent || StringUtils.isBlank(ip)) {
            ip = IPUtils.getIp(request);
        }
        if (StringUtils.isBlank(ip)) {
            throw new AException("ip 不能为空", 400);
        }
        return ip;
    }

    private static String str(Map<String, Object> body, String key) {
        if (body == null || !body.containsKey(key) || body.get(key) == null) {
            return "";
        }
        return String.valueOf(body.get(key)).trim();
    }
}
