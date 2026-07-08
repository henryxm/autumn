package cn.org.autumn.modules.client.controller;

import cn.org.autumn.annotation.SkipInterceptor;
import cn.org.autumn.modules.client.service.OauthRpAdminService;
import cn.org.autumn.modules.client.support.OauthRpAdminConstants;
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

/** 经典 OAuth2 下游 RP 综合管理 API（系统管理员）。 */
@RestController
@RequestMapping(OauthRpAdminConstants.ADMIN_RP)
@SkipInterceptor
public class OauthRpAdminController {

    @Autowired
    private OauthRpAdminService oauthRpAdminService;

    @Autowired
    private SystemAdminApi systemAdminApi;

    @GetMapping("/overview")
    public R overview(HttpServletRequest request) {
        return admin(request, () -> {
            Map<String, Object> data = new LinkedHashMap<>(oauthRpAdminService.overview());
            if (ShiroUtils.isLogin()) {
                data.put("adminUserUuid", ShiroUtils.getUserUuid());
            }
            return R.ok().put("data", data);
        });
    }

    @GetMapping("/clients")
    public R clients(HttpServletRequest request, @RequestParam Map<String, Object> params) {
        return admin(request, () -> R.ok().put("page", oauthRpAdminService.pageClients(params)));
    }

    @GetMapping("/clients/all")
    public R allClients(HttpServletRequest request) {
        return admin(request, () -> R.ok().put("list", oauthRpAdminService.listAllClientsBrief()));
    }

    @GetMapping("/client/detail")
    public R clientDetail(HttpServletRequest request, @RequestParam("clientId") String clientId) {
        return admin(request, () -> R.ok().put("client", oauthRpAdminService.getClientDetail(clientId)));
    }

    @PostMapping("/client/quickCreate")
    public R quickCreate(HttpServletRequest request, @RequestBody Map<String, String> body) {
        return admin(request, () -> {
            String clientId = body == null ? null : body.get("clientId");
            return R.ok().put("client", oauthRpAdminService.quickCreate(clientId));
        });
    }

    @PostMapping("/client/save")
    public R saveClient(HttpServletRequest request, @RequestBody Map<String, String> body) {
        return admin(request, () -> {
            String clientId = body == null ? null : body.get("clientId");
            String name = body == null ? null : body.get("name");
            String clientSecret = body == null ? null : body.get("clientSecret");
            String originUri = body == null ? null : body.get("originUri");
            String redirectUri = body == null ? null : body.get("redirectUri");
            String scope = body == null ? null : body.get("scope");
            String userInfoDelivery = body == null ? null : body.get("userInfoDelivery");
            String icon = body == null ? null : body.get("icon");
            String hash = body == null ? null : body.get("hash");
            Integer pageLogin = parsePageLogin(body == null ? null : body.get("pageLogin"));
            return R.ok().put("client", oauthRpAdminService.saveClient(clientId, name, clientSecret, originUri, redirectUri, scope, userInfoDelivery, icon, hash, pageLogin));
        });
    }

    @PostMapping("/client/update")
    public R updateClient(HttpServletRequest request, @RequestBody Map<String, String> body) {
        return admin(request, () -> {
            String clientId = body == null ? null : body.get("clientId");
            String name = body == null ? null : body.get("name");
            String clientSecret = body == null ? null : body.get("clientSecret");
            String originUri = body == null ? null : body.get("originUri");
            String redirectUri = body == null ? null : body.get("redirectUri");
            String scope = body == null ? null : body.get("scope");
            String userInfoDelivery = body == null ? null : body.get("userInfoDelivery");
            String icon = body == null ? null : body.get("icon");
            String hash = body == null ? null : body.get("hash");
            Integer pageLogin = parsePageLogin(body == null ? null : body.get("pageLogin"));
            return R.ok().put("client", oauthRpAdminService.updateClient(clientId, name, clientSecret, originUri, redirectUri, scope, userInfoDelivery, icon, hash, pageLogin));
        });
    }

    private static Integer parsePageLogin(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        return "1".equals(value.trim()) || "true".equalsIgnoreCase(value.trim()) ? 1 : 0;
    }

    @GetMapping("/binds")
    public R binds(HttpServletRequest request, @RequestParam Map<String, Object> params) {
        return admin(request, () -> R.ok().put("page", oauthRpAdminService.pageBinds(params)));
    }

    @PostMapping("/bind/create")
    public R createBind(HttpServletRequest request, @RequestBody Map<String, String> body) {
        return admin(request, () -> R.ok().put("bind", oauthRpAdminService.createBind(
                body == null ? null : body.get("clientId"),
                body == null ? null : body.get("user"),
                body == null ? null : body.get("upper"))));
    }

    @PostMapping("/bind/delete")
    public R deleteBind(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        return admin(request, () -> {
            Long id = null;
            if (body != null && body.get("id") != null) {
                id = Long.parseLong(body.get("id").toString());
            }
            oauthRpAdminService.deleteBind(id);
            return R.ok();
        });
    }

    private R admin(HttpServletRequest request, Supplier<R> action) {
        return systemAdminApi.execute(request, "OAuthRP", action);
    }
}
