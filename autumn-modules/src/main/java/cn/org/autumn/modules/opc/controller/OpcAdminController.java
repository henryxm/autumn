package cn.org.autumn.modules.opc.controller;

import cn.org.autumn.annotation.SkipInterceptor;
import cn.org.autumn.opc.OpcConstants;
import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import cn.org.autumn.modules.opc.entity.ConnectBindEntity;
import cn.org.autumn.modules.opc.service.OpcAdminService;
import cn.org.autumn.modules.sys.support.SystemAdminApi;
import cn.org.autumn.utils.R;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 开放接入综合管理 API（系统管理员）。
 */
@Slf4j
@RestController
@RequestMapping(OpcConstants.ADMIN_BASE)
@SkipInterceptor
public class OpcAdminController {

    @Autowired
    private OpcAdminService opcAdminService;

    @Autowired
    private SystemAdminApi systemAdminApi;

    @GetMapping("/overview")
    public R overview(HttpServletRequest request) {
        return admin(request, () -> R.ok().put("data", opcAdminService.overview()));
    }

    @GetMapping("/apps")
    public R apps(HttpServletRequest request, @RequestParam Map<String, Object> params) {
        return admin(request, () -> R.ok().put("page", opcAdminService.pageApps(params)));
    }

    @GetMapping("/apps/all")
    public R allApps(HttpServletRequest request) {
        return admin(request, () -> R.ok().put("list", opcAdminService.listAllAppsBrief()));
    }

    @GetMapping("/app/detail")
    public R appDetail(HttpServletRequest request, @RequestParam("appId") String appId) {
        return admin(request, () -> R.ok().put("app", opcAdminService.getAppDetail(appId)));
    }

    @PostMapping("/app/save")
    public R saveApp(HttpServletRequest request, @RequestBody Map<String, String> body) {
        return admin(request, () -> {
            ConnectAppEntity app = opcAdminService.saveApp(
                    body == null ? null : body.get("user"),
                    body == null ? null : body.get("appId"),
                    body == null ? null : body.get("appSecret"),
                    body == null ? null : body.get("platformBaseUrl"),
                    body == null ? null : body.get("redirectUri"),
                    body == null ? null : body.get("name"),
                    body == null ? null : body.get("scope"));
            return R.ok().put("app", app);
        });
    }

    @PostMapping("/app/apply")
    public R applyApp(HttpServletRequest request, @RequestBody Map<String, String> body) {
        return admin(request, () -> {
            ConnectAppEntity app = opcAdminService.applyApp(
                    body == null ? null : body.get("user"),
                    body == null ? null : body.get("platformBaseUrl"),
                    body == null ? null : body.get("name"),
                    body == null ? null : body.get("redirectUri"),
                    body == null ? null : body.get("scope"),
                    body == null ? null : body.get("accessToken"));
            return R.ok().put("app", app);
        });
    }

    @PostMapping("/app/status")
    public R updateStatus(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        return admin(request, () -> {
            String appId = body == null || body.get("appId") == null ? null : body.get("appId").toString();
            int status = body == null || body.get("status") == null ? ConnectAppEntity.STATUS_ACTIVE : Integer.parseInt(body.get("status").toString());
            ConnectAppEntity app = opcAdminService.updateAppStatus(appId, status);
            return R.ok().put("app", app);
        });
    }

    @GetMapping("/binds")
    public R binds(HttpServletRequest request, @RequestParam Map<String, Object> params) {
        return admin(request, () -> R.ok().put("page", opcAdminService.pageBinds(params)));
    }

    @PostMapping("/bind/create")
    public R createBind(HttpServletRequest request, @RequestBody Map<String, String> body) {
        return admin(request, () -> {
            ConnectBindEntity bind = opcAdminService.createBind(
                    body == null ? null : body.get("connectApp"),
                    body == null ? null : body.get("user"),
                    body == null ? null : body.get("openId"),
                    body == null ? null : body.get("unionId"));
            return R.ok().put("bind", bind);
        });
    }

    @PostMapping("/bind/delete")
    public R deleteBind(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        return admin(request, () -> {
            Long id = null;
            if (body != null && body.get("id") != null) {
                id = Long.parseLong(body.get("id").toString());
            }
            opcAdminService.deleteBind(id);
            return R.ok();
        });
    }

    @GetMapping("/config/autoRegister")
    public R getAutoRegister(HttpServletRequest request) {
        return admin(request, () -> {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("enabled", opcAdminService.getAutoRegister());
            return R.ok().put("data", data);
        });
    }

    @PostMapping("/config/autoRegister")
    public R setAutoRegister(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        return admin(request, () -> {
            boolean enabled = body != null && body.get("enabled") != null && parseBoolean(body.get("enabled"));
            opcAdminService.setAutoRegister(enabled);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("enabled", enabled);
            return R.ok().put("data", data);
        });
    }

    private boolean parseBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String text = value == null ? "" : value.toString().trim();
        return "true".equalsIgnoreCase(text) || "1".equals(text);
    }

    private R admin(HttpServletRequest request, Supplier<R> action) {
        return systemAdminApi.execute(request, "OPC", action);
    }
}
