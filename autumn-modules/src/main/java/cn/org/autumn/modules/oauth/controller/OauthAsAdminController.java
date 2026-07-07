package cn.org.autumn.modules.oauth.controller;

import cn.org.autumn.annotation.SkipInterceptor;
import cn.org.autumn.modules.oauth.service.OauthAsAdminService;
import cn.org.autumn.modules.oauth.support.OauthAdminConstants;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.sys.support.SystemAdminApi;
import cn.org.autumn.utils.R;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 经典 OAuth2 上游 AS 综合管理 API（系统管理员）。 */
@RestController
@RequestMapping(OauthAdminConstants.ADMIN_AS)
@SkipInterceptor
public class OauthAsAdminController {

    @Autowired
    private OauthAsAdminService oauthAsAdminService;

    @Autowired
    private SystemAdminApi systemAdminApi;

    @GetMapping("/overview")
    public R overview(HttpServletRequest request) {
        return admin(request, () -> {
            Map<String, Object> data = new LinkedHashMap<>(oauthAsAdminService.overview());
            if (ShiroUtils.isLogin()) {
                data.put("adminUserUuid", ShiroUtils.getUserUuid());
            }
            return R.ok().put("data", data);
        });
    }

    @GetMapping("/clients")
    public R clients(HttpServletRequest request, @RequestParam Map<String, Object> params) {
        return admin(request, () -> R.ok().put("page", oauthAsAdminService.pageClients(params)));
    }

    @GetMapping("/client/detail")
    public R clientDetail(HttpServletRequest request, @RequestParam("clientId") String clientId) {
        return admin(request, () -> R.ok().put("client", oauthAsAdminService.getClientDetail(clientId)));
    }

    @PostMapping("/client/create")
    public R createClient(HttpServletRequest request, @RequestBody Map<String, String> body) {
        return admin(request, () -> {
            String clientId = body == null ? null : body.get("clientId");
            String name = body == null ? null : body.get("name");
            String redirectUri = body == null ? null : body.get("redirectUri");
            String scope = body == null ? null : body.get("scope");
            return R.ok().put("data", oauthAsAdminService.createClient(clientId, name, redirectUri, scope));
        });
    }

    @PostMapping("/client/resetSecret")
    public R resetSecret(HttpServletRequest request, @RequestBody Map<String, String> body) {
        return admin(request, () -> {
            String clientId = body == null ? null : body.get("clientId");
            return R.ok().put("data", oauthAsAdminService.resetSecret(clientId));
        });
    }

    @PostMapping("/client/update")
    public R updateClient(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        return admin(request, () -> {
            String clientId = body == null || body.get("clientId") == null ? null : body.get("clientId").toString();
            String name = body == null || body.get("name") == null ? null : body.get("name").toString();
            String redirectUri = body == null || body.get("redirectUri") == null ? null : body.get("redirectUri").toString();
            String scope = body == null || body.get("scope") == null ? null : body.get("scope").toString();
            Integer trusted = parseInt(body, "trusted");
            Integer archived = parseInt(body, "archived");
            return R.ok().put("client", oauthAsAdminService.updateClient(clientId, name, redirectUri, scope, trusted, archived));
        });
    }

    @GetMapping("/tokens")
    public R tokens(HttpServletRequest request, @RequestParam Map<String, Object> params) {
        return admin(request, () -> R.ok().put("page", oauthAsAdminService.pageTokens(params)));
    }

    private Integer parseInt(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null || StringUtils.isBlank(body.get(key).toString())) {
            return null;
        }
        return Integer.parseInt(body.get(key).toString());
    }

    private R admin(HttpServletRequest request, Supplier<R> action) {
        return systemAdminApi.execute(request, "OAuthAS", action);
    }
}
