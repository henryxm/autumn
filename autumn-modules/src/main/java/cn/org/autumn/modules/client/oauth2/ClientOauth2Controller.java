package cn.org.autumn.modules.client.oauth2;

import cn.org.autumn.config.ApplicationInitializationProgress;
import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.client.oauth2.WebOauthBindException.ConflictType;
import cn.org.autumn.modules.client.service.OauthRpLoginService;
import cn.org.autumn.modules.client.service.OauthRpStateService;
import cn.org.autumn.modules.client.service.AuthSiteRoleService;
import cn.org.autumn.modules.client.dto.OauthRpStatePayload;
import cn.org.autumn.modules.client.dto.WebOauthBindPendingContext;
import cn.org.autumn.modules.client.service.WebAuthenticationService;
import cn.org.autumn.modules.client.service.WebOauthBindPendingService;
import cn.org.autumn.modules.client.service.WebOauthBindService;
import cn.org.autumn.modules.client.service.WebOauthLoginService;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.site.HealthFactory;
import cn.org.autumn.site.PageFactory;
import cn.org.autumn.site.VersionFactory;
import cn.org.autumn.utils.IPUtils;
import cn.org.autumn.utils.R;
import cn.org.autumn.utils.WebPathUtils;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/** OAuth2 RP 浏览器入口：B1 授权回调、绑定选择页、冲突页。 */
@Controller
@RequestMapping("client")
@Slf4j
public class ClientOauth2Controller {

    @Autowired
    WebAuthenticationService webAuthenticationService;

    @Autowired
    SysConfigService sysConfigService;

    @Autowired
    PageFactory pageFactory;

    @Autowired
    WebOauthLoginService webOauthLoginService;

    @Autowired
    WebOauthBindPendingService webOauthBindPendingService;

    @Autowired
    WebOauthBindService webOauthBindService;

    @Autowired
    AuthSiteRoleService authSiteRoleService;

    @Autowired
    OauthRpLoginService oauthRpLoginService;

    @Autowired
    WebOauthEndpointResolver webOauthEndpointResolver;

    @Autowired
    OauthRpStateService oauthRpStateService;

    @Autowired
    HealthFactory healthFactory;

    @Autowired
    VersionFactory versionFactory;

    @Autowired(required = false)
    ApplicationInitializationProgress applicationInitializationProgress;

    @RequestMapping("oauth2/login")
    public Object rpAuthorize(HttpServletRequest request, HttpServletResponse response, Model model) {
        String qs = request.getQueryString();
        String target = WebPathUtils.forBrowser(request, "/oauth2/login");
        if (StringUtils.isNotBlank(qs)) {
            target = target + "?" + qs;
        }
        return pageFactory.direct(request, response, model, target);
    }

    @RequestMapping("oauth2/callback")
    public Object defaultCodeCallback(HttpServletRequest request, HttpServletResponse response, Model model) throws OAuthSystemException {
        if (log.isDebugEnabled()) {
            String query = request.getQueryString();
            if (StringUtils.isNotBlank(query))
                query = "?" + query;
            else
                query = "";
            log.debug("Client OAuth login:{}{}", request.getRequestURL().toString(), query);
        }
        String oauthError = request.getParameter("error");
        if (StringUtils.isNotBlank(oauthError)) {
            return authCallbackErrorPage(request, response, model, oauthError, request.getParameter("error_description"));
        }
        String authCode = request.getParameter(OAuth.OAUTH_CODE);
        if (StringUtils.isEmpty(authCode)) {
            return authCallbackErrorPage(request, response, model, OAuthError.OAUTH_ERROR, "未收到授权码，请重新发起授权。");
        }
        WebAuthenticationEntity webAuthenticationEntity = resolveCallbackWebAuth(request);
        if (webAuthenticationEntity == null) {
            return authCallbackErrorPage(request, response, model, OAuthError.OAUTH_ERROR, "未找到 OAuth 客户端配置，请联系管理员。");
        }
        try {
            if (isRemoteRpCallback(webAuthenticationEntity)) {
                String callback = oauthRpLoginService.handleCallback(request, authCode, request.getParameter(OAuth.OAUTH_STATE), oauthError, request.getParameter("error_description"));
                if (StringUtils.isBlank(callback)) {
                    callback = WebPathUtils.forBrowser(request, "/oauth2/success");
                }
                return pageFactory.direct(request, response, model, callback);
            }
            webOauthLoginService.completeOAuthCallback(request, webAuthenticationEntity, authCode);
        } catch (WebOauthBindException e) {
            log.warn("OAuth bind conflict: type={}, clientId={}", e.getConflictType(), e.getClientId());
            if (e.getConflictType() == ConflictType.BIND_CHOICE_REQUIRED) {
                return authCallbackBindChoicePage(request, response, model, e);
            }
            if (e.getConflictType() == ConflictType.UPSTREAM_UUID_INVALID) {
                return authCallbackErrorPage(request, response, model, OAuthError.OAUTH_ERROR, "授权用户信息无效，请重新发起授权。");
            }
            return authCallbackConflictPage(request, response, model, e);
        } catch (Exception e) {
            log.error("OAuth callback failed: {}", e.getMessage());
            return authCallbackErrorPage(request, response, model, OAuthError.OAUTH_ERROR, StringUtils.defaultIfBlank(e.getMessage(), "授权登录失败，请稍后重试。"));
        }
        String callback = WebPathUtils.safeOauthCallbackForClient(request, request.getParameter("callback"));
        if (StringUtils.isBlank(callback)) {
            callback = WebPathUtils.forBrowser(request, "/oauth2/success");
        }
        if (log.isDebugEnabled())
            log.debug("Callback URL:{}", callback);
        return pageFactory.direct(request, response, model, callback);
    }

    @ResponseBody
    @PostMapping("oauth2/bind/unbind")
    public R unbind(HttpServletRequest request, @RequestParam(required = false) String clientId) {
        WebAuthenticationEntity webAuth = resolveWebAuth(request, clientId);
        if (webAuth == null) {
            return R.error("未找到 OAuth 客户端配置");
        }
        try {
            webOauthBindService.unbindForSessionUser(webAuth);
            return R.ok();
        } catch (IllegalStateException e) {
            return R.error(e.getMessage());
        }
    }

    @RequestMapping("oauth2/bind/choice")
    public Object bindChoicePage(HttpServletRequest request, HttpServletResponse response, Model model, @RequestParam("token") String token) {
        if (StringUtils.isBlank(token)) {
            return authCallbackErrorPage(request, response, model, OAuthError.OAUTH_ERROR, "绑定会话无效或已过期");
        }
        WebOauthBindPendingContext pending = webOauthBindPendingService.peek(token);
        if (pending == null) {
            return authCallbackErrorPage(request, response, model, OAuthError.OAUTH_ERROR, "绑定会话已过期，请重新发起授权");
        }
        model.addAttribute("bindToken", token);
        model.addAttribute("loginBindUrl", WebPathUtils.forBrowser(request, "/login?callback=" + encodeCallback(WebPathUtils.forBrowser(request, "/client/oauth2/bind/confirm?token=" + token))));
        model.addAttribute("createBindAction", WebPathUtils.forBrowser(request, "/client/oauth2/bind/create"));
        model.addAttribute("title", "选择账号绑定方式");
        model.addAttribute("message", "授权已成功，请选择将此外部账号与本地账号的关联方式。");
        return pageFactory.oauthBindChoice(request, response, model);
    }

    @RequestMapping("oauth2/bind/confirm")
    public Object bindConfirm(HttpServletRequest request, HttpServletResponse response, Model model, @RequestParam("token") String token) {
        WebOauthBindPendingContext pending = webOauthBindPendingService.peek(token);
        String callback = pending == null ? null : pending.getCallback();
        try {
            webOauthLoginService.completePendingBindSession(request, token);
        } catch (WebOauthBindException e) {
            if (e.getConflictType() == ConflictType.UPSTREAM_BOUND_TO_OTHER || e.getConflictType() == ConflictType.LOCAL_ALREADY_BOUND) {
                return authCallbackConflictPage(request, response, model, e);
            }
            return authCallbackErrorPage(request, response, model, OAuthError.OAUTH_ERROR, e.getMessage());
        } catch (Exception e) {
            return authCallbackErrorPage(request, response, model, OAuthError.OAUTH_ERROR, StringUtils.defaultIfBlank(e.getMessage(), "绑定失败，请重试"));
        }
        if (StringUtils.isBlank(callback)) {
            callback = WebPathUtils.forBrowser(request, "/oauth2/success");
        }
        return pageFactory.direct(request, response, model, callback);
    }

    @PostMapping("oauth2/bind/create")
    public Object bindCreate(HttpServletRequest request, HttpServletResponse response, Model model, @RequestParam("token") String token) {
        WebOauthBindPendingContext pending = webOauthBindPendingService.peek(token);
        String callback = pending == null ? null : pending.getCallback();
        try {
            webOauthLoginService.completePendingCreateNew(request, token);
        } catch (WebOauthBindException e) {
            return authCallbackErrorPage(request, response, model, OAuthError.OAUTH_ERROR, e.getMessage());
        } catch (Exception e) {
            return authCallbackErrorPage(request, response, model, OAuthError.OAUTH_ERROR, StringUtils.defaultIfBlank(e.getMessage(), "创建账号失败，请重试"));
        }
        if (StringUtils.isBlank(callback)) {
            callback = WebPathUtils.forBrowser(request, "/oauth2/success");
        }
        return pageFactory.direct(request, response, model, callback);
    }

    private Object authCallbackBindChoicePage(HttpServletRequest request, HttpServletResponse response, Model model, WebOauthBindException e) {
        String token = e.getPendingToken();
        if (StringUtils.isBlank(token)) {
            return authCallbackErrorPage(request, response, model, OAuthError.OAUTH_ERROR, "绑定会话无效，请重新发起授权");
        }
        return pageFactory.direct(request, response, model, webOauthLoginService.bindChoicePageUrl(request, token));
    }

    private static String encodeCallback(String callback) {
        try {
            return java.net.URLEncoder.encode(callback, "UTF-8");
        } catch (Exception e) {
            return callback;
        }
    }

    private WebAuthenticationEntity resolveCallbackWebAuth(HttpServletRequest request) {
        String state = request.getParameter(OAuth.OAUTH_STATE);
        OauthRpStatePayload payload = oauthRpStateService.peekStatePayload(state);
        if (payload != null && StringUtils.isNotBlank(payload.getClientId())) {
            WebAuthenticationEntity byState = webAuthenticationService.getByClientId(payload.getClientId());
            if (byState != null) {
                return byState;
            }
        }
        if (authSiteRoleService.isRpEnabled()) {
            WebAuthenticationEntity rpClient = authSiteRoleService.resolveRpClient(request);
            if (rpClient != null && webOauthEndpointResolver.hasRemoteOrigin(rpClient)) {
                return rpClient;
            }
        }
        return resolveWebAuth(request);
    }

    private boolean isRemoteRpCallback(WebAuthenticationEntity webAuth) {
        return webOauthEndpointResolver.hasRemoteOrigin(webAuth);
    }

    private WebAuthenticationEntity resolveWebAuth(HttpServletRequest request) {
        return resolveWebAuth(request, null);
    }

    private WebAuthenticationEntity resolveWebAuth(HttpServletRequest request, String clientId) {
        if (StringUtils.isNotBlank(clientId)) {
            return webAuthenticationService.getByClientId(clientId.trim());
        }
        String host = request.getHeader("host");
        WebAuthenticationEntity webAuthenticationEntity = webAuthenticationService.getByClientId(host);
        if (webAuthenticationEntity == null) {
            webAuthenticationEntity = webAuthenticationService.getByClientId(sysConfigService.getOauth2LoginClientId(host));
        }
        return webAuthenticationEntity;
    }

    private Object authCallbackErrorPage(HttpServletRequest request, HttpServletResponse response, Model model, String error, String description) {
        model.addAttribute("oauthError", error);
        model.addAttribute("loginUrl", WebPathUtils.forBrowser(request, "/login"));
        if ("access_denied".equalsIgnoreCase(error)) {
            model.addAttribute("title", "授权已取消");
            model.addAttribute("message", "您已取消授权，应用无法获取您的账号信息。如需使用相关功能，请重新发起授权并点击确认。");
        } else if (StringUtils.isNotBlank(description)) {
            model.addAttribute("title", "授权失败");
            model.addAttribute("message", description);
        } else {
            model.addAttribute("title", "授权未完成");
            model.addAttribute("message", "授权未能完成，请稍后重试。");
        }
        return pageFactory.authCallbackError(request, response, model);
    }

    private Object authCallbackConflictPage(HttpServletRequest request, HttpServletResponse response, Model model, WebOauthBindException e) {
        model.addAttribute("oauthError", "bind_conflict");
        model.addAttribute("conflictType", e.getConflictType().name());
        model.addAttribute("loginUrl", WebPathUtils.forBrowser(request, "/login"));
        model.addAttribute("logoutUrl", WebPathUtils.forBrowser(request, "/logout"));
        model.addAttribute("manageUrl", WebPathUtils.forBrowser(request, "/modules/client/weboauthbind"));
        model.addAttribute("title", "账号绑定冲突");
        model.addAttribute("message", buildConflictMessage(e));
        model.addAttribute("conflictSolutions", buildConflictSolutions(e));
        return pageFactory.authCallbackError(request, response, model);
    }

    private String buildConflictMessage(WebOauthBindException e) {
        if (e.getConflictType() == ConflictType.UPSTREAM_BOUND_TO_OTHER) {
            return "该上游授权账号已与其他本地用户绑定，无法与当前登录账号关联。";
        }
        if (e.getConflictType() == ConflictType.LOCAL_ALREADY_BOUND) {
            return "您当前登录的本地账号已绑定其他上游授权账号，无法重复绑定。";
        }
        return "授权用户信息无效，请重新发起授权。";
    }

    private String buildConflictSolutions(WebOauthBindException e) {
        StringBuilder sb = new StringBuilder();
        if (e.getConflictType() == ConflictType.UPSTREAM_BOUND_TO_OTHER) {
            sb.append("① 退出当前本地账号后，使用已绑定的账号重新登录，再发起授权；\n");
            sb.append("② 保持当前账号，请管理员在后台解除该上游账号的原绑定关系；\n");
            sb.append("③ 登录后调用解绑接口（若您拥有该上游账号绑定权）解除冲突绑定，再重新授权。");
        } else if (e.getConflictType() == ConflictType.LOCAL_ALREADY_BOUND) {
            sb.append("① 先解除当前本地账号的上游绑定（登录态下 POST client/oauth2/bind/unbind），再重新授权；\n");
            sb.append("② 退出当前账号，换用未绑定的本地账号登录后重新授权；\n");
            sb.append("③ 联系管理员在后台调整绑定关系。");
        } else {
            sb.append("请重新发起授权；若问题持续，请联系管理员。");
        }
        return sb.toString();
    }

    @ResponseBody
    @RequestMapping(value = {"/myip"})
    public String myip(HttpServletRequest request) {
        return IPUtils.getIp(request);
    }

    @ResponseBody
    @RequestMapping(value = {"/health"})
    public R health() {
        R result;
        if (applicationInitializationProgress != null) {
            ApplicationInitializationProgress.Phase p = applicationInitializationProgress.getPhase();
            if (p == ApplicationInitializationProgress.Phase.FAILED) {
                result = R.error(500, "application startup failed");
                attachBootstrapStatus(result);
                return result;
            }
            if (p != ApplicationInitializationProgress.Phase.WIZARD && !applicationInitializationProgress.isLanguageCacheReady()) {
                result = R.error(503, "language cache not ready");
                attachBootstrapStatus(result);
                return result;
            }
        }
        Map<String, Object> o = healthFactory.getHealth();
        if (null != o && !o.isEmpty()) {
            result = R.ok().put("data", o);
        } else {
            result = R.ok();
        }
        attachBootstrapStatus(result);
        return result;
    }

    /**
     * Bootstrap fields for loading-page soft polling (phase / percent / languageReady).
     * Always attached so the UI can update without full page reload while code != 0.
     */
    private void attachBootstrapStatus(R result) {
        if (result == null || applicationInitializationProgress == null) {
            return;
        }
        ApplicationInitializationProgress.Phase p = applicationInitializationProgress.getPhase();
        result.put("phase", p.name());
        result.put("phaseLabel", applicationInitializationProgress.getMessage());
        result.put("percent", applicationInitializationProgress.getPercentForPhase());
        result.put("languageCacheReady", applicationInitializationProgress.isLanguageCacheReady());
        result.put("ready", Integer.valueOf(0).equals(result.get("code")));
        if (p == ApplicationInitializationProgress.Phase.FAILED) {
            result.put("error", applicationInitializationProgress.getFailedDetail());
        }
    }

    @ResponseBody
    @RequestMapping(value = {"/version"})
    public R version() {
        Map<String, Object> o = versionFactory.getVersion();
        if (null != o && !o.isEmpty())
            return R.ok().put("data", o);
        else
            return R.ok();
    }
}
