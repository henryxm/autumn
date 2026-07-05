package cn.org.autumn.modules.opl.controller;

import cn.org.autumn.annotation.SkipInterceptor;
import cn.org.autumn.opl.OplConstants;
import cn.org.autumn.opl.model.OpenAppRegisterOutcome;
import cn.org.autumn.modules.opl.entity.OpenAccountEntity;
import cn.org.autumn.modules.opl.entity.OpenAppEntity;
import cn.org.autumn.modules.opl.service.OplAdminService;
import cn.org.autumn.modules.sys.support.SystemAdminApi;
import cn.org.autumn.opl.model.OpenAppType;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 开放平台综合管理 API（系统管理员）。
 */
@Slf4j
@RestController
@RequestMapping(OplConstants.ADMIN_BASE)
@SkipInterceptor
public class OplAdminController {

    @Autowired
    private OplAdminService oplAdminService;

    @Autowired
    private SystemAdminApi systemAdminApi;

    @GetMapping("/overview")
    public R overview(HttpServletRequest request) {
        return admin(request, () -> R.ok().put("data", oplAdminService.overview()));
    }

    @GetMapping("/appTypes")
    public R appTypes(HttpServletRequest request) {
        return admin(request, () -> R.ok().put("list", oplAdminService.listAppTypes()));
    }

    @GetMapping("/accounts")
    public R accounts(HttpServletRequest request, @RequestParam Map<String, Object> params) {
        return admin(request, () -> R.ok().put("page", oplAdminService.pageAccounts(params)));
    }

    @GetMapping("/accounts/all")
    public R allAccounts(HttpServletRequest request) {
        return admin(request, () -> {
            List<OpenAccountEntity> list = oplAdminService.listAllAccounts();
            return R.ok().put("list", list);
        });
    }

    @PostMapping("/account/create")
    public R createAccount(HttpServletRequest request, @RequestBody Map<String, String> body) {
        return admin(request, () -> {
            String user = body == null ? null : body.get("user");
            String name = body == null ? null : body.get("name");
            OpenAccountEntity account = oplAdminService.createAccount(user, name);
            return R.ok().put("account", account);
        });
    }

    @GetMapping("/apps")
    public R apps(HttpServletRequest request, @RequestParam Map<String, Object> params) {
        return admin(request, () -> R.ok().put("page", oplAdminService.pageApps(params)));
    }

    @GetMapping("/app/detail")
    public R appDetail(HttpServletRequest request, @RequestParam("appId") String appId) {
        return admin(request, () -> R.ok().put("app", oplAdminService.getAppDetail(appId)));
    }

    @PostMapping("/app/create")
    public R createApp(HttpServletRequest request, @RequestBody Map<String, String> body) {
        return admin(request, () -> {
            String account = body == null ? null : body.get("account");
            String name = body == null ? null : body.get("name");
            String redirectUri = body == null ? null : body.get("redirectUri");
            String scope = body == null ? null : body.get("scope");
            OpenAppType appType = body == null || StringUtils.isBlank(body.get("appType")) ? OpenAppType.Web : OpenAppType.parse(body.get("appType"));
            OpenAppRegisterOutcome result = oplAdminService.createApp(account, name, redirectUri, scope, appType);
            return R.ok().put("data", result);
        });
    }

    @PostMapping("/app/resetSecret")
    public R resetSecret(HttpServletRequest request, @RequestBody Map<String, String> body) {
        return admin(request, () -> {
            String appId = body == null ? null : body.get("appId");
            OpenAppRegisterOutcome result = oplAdminService.resetAppSecret(appId);
            return R.ok().put("data", result);
        });
    }

    @PostMapping("/app/status")
    public R updateStatus(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        return admin(request, () -> {
            String appId = body == null || body.get("appId") == null ? null : body.get("appId").toString();
            int status = body == null || body.get("status") == null ? OpenAppEntity.STATUS_ACTIVE : Integer.parseInt(body.get("status").toString());
            OpenAppEntity app = oplAdminService.updateAppStatus(appId, status);
            return R.ok().put("app", app);
        });
    }

    @PostMapping("/app/update")
    public R updateApp(HttpServletRequest request, @RequestBody Map<String, String> body) {
        return admin(request, () -> {
            String appId = body == null ? null : body.get("appId");
            OpenAppType appType = body == null || StringUtils.isBlank(body.get("appType")) ? null : OpenAppType.parse(body.get("appType"));
            OpenAppEntity app = oplAdminService.updateAppInfo(appId, body == null ? null : body.get("name"), body == null ? null : body.get("redirectUri"), body == null ? null : body.get("scope"), appType);
            return R.ok().put("app", app);
        });
    }

    @GetMapping("/app/users")
    public R appUsers(HttpServletRequest request, @RequestParam Map<String, Object> params) {
        return admin(request, () -> R.ok().put("page", oplAdminService.pageAppUsers(params)));
    }

    @GetMapping("/unions")
    public R unions(HttpServletRequest request, @RequestParam Map<String, Object> params) {
        return admin(request, () -> R.ok().put("page", oplAdminService.pageUnions(params)));
    }

    @GetMapping("/codes")
    public R codes(HttpServletRequest request, @RequestParam Map<String, Object> params) {
        return admin(request, () -> R.ok().put("page", oplAdminService.pageCodes(params)));
    }

    @GetMapping("/tokens")
    public R tokens(HttpServletRequest request, @RequestParam Map<String, Object> params) {
        return admin(request, () -> R.ok().put("page", oplAdminService.pageTokens(params)));
    }

    private R admin(HttpServletRequest request, Supplier<R> action) {
        return systemAdminApi.execute(request, "OPL", action);
    }
}
