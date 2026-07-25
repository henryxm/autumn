package cn.org.autumn.modules.node.controller;

import cn.org.autumn.node.Profile;
import cn.org.autumn.node.ProfileService;
import cn.org.autumn.node.Registry;
import cn.org.autumn.node.role.ServerRole;
import cn.org.autumn.node.role.ServerRoleRegistry;
import cn.org.autumn.utils.R;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 本机节点画像 HTTP API（通用；路径不含消费方产品名）。
 * <p>
 * 前缀 {@code /sys/node}：读改 profile、切换 home、强制 reload；可选 registry 快照与远程 assign。
 * 外部手动改盘后默认等缓存 TTL（约 1 分钟）或 {@code POST /profile/reload} 立即生效。
 */
@Slf4j
@Controller
@RequestMapping("/sys/node")
public class ProfileController {

    private final ProfileService profileService;
    private final ObjectProvider<Registry> registryProvider;
    private final ObjectProvider<ServerRoleRegistry> roleRegistryProvider;

    public ProfileController(ProfileService profileService, ObjectProvider<Registry> registryProvider,
                             ObjectProvider<ServerRoleRegistry> roleRegistryProvider) {
        this.profileService = profileService;
        this.registryProvider = registryProvider;
        this.roleRegistryProvider = roleRegistryProvider;
    }

    @GetMapping("/profile")
    @ResponseBody
    public R profile() {
        try {
            return R.ok().put("profile", profileService.profile());
        } catch (Exception e) {
            log.error("get profile failed", e);
            return R.error(e.getMessage());
        }
    }

    @GetMapping("/profile/uuid")
    @ResponseBody
    public R uuid() {
        try {
            return R.ok().put("uuid", profileService.uuid());
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    @PutMapping("/profile")
    @ResponseBody
    public R patch(@RequestBody Map<String, Object> body) {
        try {
            Profile p = profileService.patch(body);
            return R.ok().put("profile", p);
        } catch (Exception e) {
            log.error("patch profile failed", e);
            return R.error(e.getMessage());
        }
    }

    @PutMapping("/profile/home")
    @ResponseBody
    public R home(@RequestBody Map<String, Object> body) {
        try {
            String home = body != null && body.get("home") != null ? String.valueOf(body.get("home")) : null;
            boolean migrate = body != null && Boolean.TRUE.equals(body.get("migrate"));
            Profile p = profileService.home(home, migrate);
            return R.ok().put("profile", p).put("home", profileService.home().toString()).put("file", profileService.file().toString());
        } catch (Exception e) {
            log.error("set profile home failed", e);
            return R.error(e.getMessage());
        }
    }

    @PostMapping("/profile/reload")
    @ResponseBody
    public R reload() {
        try {
            return R.ok().put("profile", profileService.reload());
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    @PostMapping("/profile/reset-uuid")
    @ResponseBody
    public R resetUuid() {
        try {
            return R.ok().put("profile", profileService.resetUuid());
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    /** 已注册服务器角色目录（含预置 LOCAL/JOB 等），供管理页勾选。 */
    @GetMapping("/roles")
    @ResponseBody
    public R roles() {
        ServerRoleRegistry registry = roleRegistryProvider.getIfAvailable();
        List<Map<String, Object>> list = new ArrayList<>();
        if (registry != null) {
            for (ServerRole role : registry.list()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("code", role.getCode());
                m.put("name", role.getName());
                m.put("description", role.getDescription());
                m.put("capabilities", role.getCapabilities());
                m.put("order", role.getOrder());
                list.add(m);
            }
        }
        return R.ok().put("roles", list);
    }

    @GetMapping("/registry")
    @ResponseBody
    public R registry() {
        Registry registry = registryProvider.getIfAvailable();
        if (registry == null) {
            return R.ok().put("enabled", false).put("configured", false).put("members", List.of());
        }
        boolean configured = registry.configured();
        boolean enabled = registry.enabled();
        return R.ok()
                .put("enabled", enabled)
                .put("configured", configured)
                .put("namespace", registry.namespacePublic())
                .put("staleMs", Registry.STALE_MS)
                .put("redisOpen", registry.redisOpen())
                .put("followsRedis", registry.followsRedisOpen())
                .put("online", registry.online())
                .put("nodes", registry.snapshot())
                .put("members", registry.members());
    }

    /** 立即上报本机心跳（改本机 roles 后刷新成员表用）。 */
    @PostMapping("/registry/beat")
    @ResponseBody
    public R beat() {
        Registry registry = registryProvider.getIfAvailable();
        if (registry == null || !registry.enabled()) {
            return R.error("registry inactive: need autumn.redis.open=true and Redisson");
        }
        try {
            registry.beat();
            return R.ok().put("members", registry.members()).put("online", registry.online());
        } catch (Exception e) {
            log.error("registry beat failed", e);
            return R.error(e.getMessage());
        }
    }

    @PutMapping("/registry/assign")
    @ResponseBody
    public R assign(@RequestBody Map<String, Object> body) {
        Registry registry = registryProvider.getIfAvailable();
        if (registry == null || !registry.enabled()) {
            return R.error("registry inactive: need autumn.redis.open=true and Redisson");
        }
        String uuid = body != null && body.get("uuid") != null ? String.valueOf(body.get("uuid")) : null;
        List<String> roles = parseRoles(body);
        boolean ok = registry.assign(uuid, roles);
        return ok ? R.ok() : R.error("assign failed");
    }

    /** 批量远程指派 roles；异步 Pub/Sub，不保证目标已落盘。 */
    @PutMapping("/registry/assign-batch")
    @ResponseBody
    public R assignBatch(@RequestBody Map<String, Object> body) {
        Registry registry = registryProvider.getIfAvailable();
        if (registry == null || !registry.enabled()) {
            return R.error("registry inactive: need autumn.redis.open=true and Redisson");
        }
        List<String> uuids = new ArrayList<>();
        if (body != null && body.get("uuids") instanceof List<?> list) {
            for (Object o : list) {
                if (o != null && StringUtils.isNotBlank(String.valueOf(o))) {
                    uuids.add(String.valueOf(o).trim());
                }
            }
        }
        if (uuids.isEmpty()) {
            return R.error("uuids required");
        }
        List<String> roles = parseRoles(body);
        int ok = 0;
        int fail = 0;
        List<Map<String, Object>> results = new ArrayList<>();
        for (String uuid : uuids) {
            boolean success = registry.assign(uuid, roles);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("uuid", uuid);
            row.put("ok", success);
            results.add(row);
            if (success) {
                ok++;
            } else {
                fail++;
            }
        }
        return R.ok().put("ok", ok).put("fail", fail).put("results", results);
    }

    private static List<String> parseRoles(Map<String, Object> body) {
        if (body != null && body.get("roles") instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
