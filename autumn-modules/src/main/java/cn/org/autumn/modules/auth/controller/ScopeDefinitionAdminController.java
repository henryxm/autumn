package cn.org.autumn.modules.auth.controller;

import cn.org.autumn.annotation.SkipInterceptor;
import cn.org.autumn.auth.scope.AuthScopeDef;
import cn.org.autumn.auth.scope.AuthScopeSet;
import cn.org.autumn.auth.scope.AuthTrack;
import cn.org.autumn.modules.auth.entity.ScopeDefinitionEntity;
import cn.org.autumn.modules.auth.service.ScopeDefinitionService;
import cn.org.autumn.modules.sys.support.SystemAdminApi;
import cn.org.autumn.utils.R;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/oauth/admin/scopes")
@SkipInterceptor
public class ScopeDefinitionAdminController {

    @Autowired
    private ScopeDefinitionService scopeDefinitionService;

    @Autowired
    private SystemAdminApi systemAdminApi;

    @GetMapping("/catalog")
    public R catalog(HttpServletRequest request, @RequestParam String track) {
        return admin(request, () -> {
            AuthTrack authTrack = parseTrack(track);
            if (authTrack == null) {
                return R.error("track 须为 oauth 或 opl");
            }
            List<AuthScopeDef> defs = scopeDefinitionService.listCatalog(authTrack);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("items", toViews(defs));
            data.put("track", authTrack.name().toLowerCase());
            data.put("basicCodes", new ArrayList<String>(AuthScopeSet.basicFor(authTrack).getCodes()));
            return R.ok().put("data", data);
        });
    }

    @GetMapping("/list")
    public R list(HttpServletRequest request) {
        return admin(request, () -> R.ok().put("items", scopeDefinitionService.listForAdmin()));
    }

    @PostMapping("/save")
    public R save(HttpServletRequest request, @RequestBody Map<String, String> body) {
        return admin(request, () -> {
            ScopeDefinitionEntity entity = scopeDefinitionService.saveCustom(body.get("code"), body.get("label"), body.get("tracks"), body.get("fields"), body.get("sensitivity"), body.get("requires"));
            return R.ok().put("item", entity);
        });
    }

    @PostMapping("/enabled")
    public R enabled(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        return admin(request, () -> {
            String uuid = body.get("uuid") == null ? null : body.get("uuid").toString();
            boolean enabled = body.get("enabled") != null && Boolean.parseBoolean(body.get("enabled").toString());
            return R.ok().put("item", scopeDefinitionService.updateEnabled(uuid, enabled));
        });
    }

    @PostMapping("/delete")
    public R delete(HttpServletRequest request, @RequestBody Map<String, String> body) {
        return admin(request, () -> {
            scopeDefinitionService.deleteCustom(body.get("uuid"));
            return R.ok();
        });
    }

    private AuthTrack parseTrack(String track) {
        if (StringUtils.isBlank(track)) {
            return null;
        }
        try {
            return AuthTrack.valueOf(track.trim().toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

    private R admin(HttpServletRequest request, java.util.function.Supplier<R> action) {
        return systemAdminApi.execute(request, "ScopeDefinition", action);
    }

    private List<Map<String, Object>> toViews(List<AuthScopeDef> defs) {
        List<Map<String, Object>> views = new ArrayList<>();
        if (defs == null) {
            return views;
        }
        for (AuthScopeDef def : defs) {
            if (def == null) {
                continue;
            }
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("code", def.getCode());
            view.put("label", def.getLabel());
            view.put("enabled", def.isEnabled());
            view.put("builtin", def.isBuiltin());
            view.put("sensitivity", def.getSensitivity() == null ? null : def.getSensitivity().name());
            views.add(view);
        }
        return views;
    }
}
