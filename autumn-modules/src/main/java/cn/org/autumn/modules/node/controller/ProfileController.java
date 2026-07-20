package cn.org.autumn.modules.node.controller;

import cn.org.autumn.node.Profile;
import cn.org.autumn.node.ProfileService;
import cn.org.autumn.node.Registry;
import cn.org.autumn.utils.R;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
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

    public ProfileController(ProfileService profileService, ObjectProvider<Registry> registryProvider) {
        this.profileService = profileService;
        this.registryProvider = registryProvider;
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

    @GetMapping("/registry")
    @ResponseBody
    public R registry() {
        Registry registry = registryProvider.getIfAvailable();
        if (registry == null) {
            return R.ok().put("enabled", false);
        }
        return R.ok()
                .put("enabled", registry.enabled())
                .put("online", registry.online())
                .put("nodes", registry.snapshot());
    }

    @PutMapping("/registry/assign")
    @ResponseBody
    public R assign(@RequestBody Map<String, Object> body) {
        Registry registry = registryProvider.getIfAvailable();
        if (registry == null || !registry.enabled()) {
            return R.error("registry disabled; set autumn.node.registry=true");
        }
        String uuid = body != null && body.get("uuid") != null ? String.valueOf(body.get("uuid")) : null;
        @SuppressWarnings("unchecked")
        List<String> roles = body != null && body.get("roles") instanceof List<?> list
                ? list.stream().map(String::valueOf).toList()
                : List.of();
        boolean ok = registry.assign(uuid, roles);
        return ok ? R.ok() : R.error("assign failed");
    }
}
