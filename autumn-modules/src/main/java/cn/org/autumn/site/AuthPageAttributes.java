package cn.org.autumn.site;

import cn.org.autumn.model.AuthLoginProviderList;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.sys.shiro.LogoutSkipSupport;
import cn.org.autumn.utils.Utils;
import cn.org.autumn.utils.WebPathUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ui.Model;

/**
 * 登录、注册、忘记密码及双轨授权流程页面的公共 Model 属性。
 */
public final class AuthPageAttributes {

    public static final String ATTR_OAUTH_AUTHORIZE = "oauthAuthorize";
    public static final String ATTR_OPL_AUTHORIZE = "oplAuthorize";
    public static final String ATTR_AUTH_FLOW_KIND = "authFlowKind";
    public static final String ATTR_CONSENT_APP_NAME = "consentAppName";
    public static final String ATTR_CONSENT_APP_ICON_URI = "consentAppIconUri";
    public static final String ATTR_AUTH_LOGIN_DEFAULT_ICON = "authLoginDefaultIcon";

    public static final String FLOW_OAUTH_AS_AUTHORIZE = "oauth_as_authorize";
    public static final String FLOW_OPL_AS_AUTHORIZE = "opl_as_authorize";
    public static final String FLOW_OAUTH_LOGIN_ENTRY = "oauth_login_entry";
    public static final String FLOW_OAUTH_LOGIN_SUCCESS = "oauth_login_success";
    public static final String FLOW_OPEN_LOGIN_ENTRY = "open_login_entry";
    public static final String FLOW_OPEN_LOGIN_SUCCESS = "open_login_success";
    public static final String FLOW_AUTH_CALLBACK_ERROR = "auth_callback_error";

    private AuthPageAttributes() {
    }

    public static void markFlowKind(Model model, String kind) {
        if (model != null && StringUtils.isNotBlank(kind)) {
            model.addAttribute(ATTR_AUTH_FLOW_KIND, kind);
        }
    }

    /** 授权确认卡片：应用名称与图标（无图标时模板回退 {@link #ATTR_AUTH_LOGIN_DEFAULT_ICON}）。 */
    public static void applyConsentApp(Model model, String appName, String iconUri) {
        if (model == null) {
            return;
        }
        model.addAttribute(ATTR_CONSENT_APP_NAME, StringUtils.defaultIfBlank(appName, "未知应用"));
        if (StringUtils.isNotBlank(iconUri)) {
            model.addAttribute(ATTR_CONSENT_APP_ICON_URI, iconUri.trim());
        }
    }

    public static void applyAuthLoginDefaultIcon(HttpServletRequest request, Model model) {
        if (model == null) {
            return;
        }
        String path = AuthLoginProviderList.DEFAULT_ICON_PATH;
        if (request != null) {
            path = request.getContextPath() + path;
        }
        model.addAttribute(ATTR_AUTH_LOGIN_DEFAULT_ICON, path);
    }

    /**
     * 为授权流程页写入 {@code authFlowCtx} 等前端引导属性，供 {@code auth-flow.js} 与扩展模板使用。
     */
    public static void applyAuthFlowBoot(HttpServletRequest request, Model model) {
        if (model == null) {
            return;
        }
        if (request != null && !model.containsAttribute("authFlowCtx")) {
            model.addAttribute("authFlowCtx", request.getContextPath());
        }
    }

    public static void apply(Model model, SysConfigService sysConfigService) {
        apply(model, sysConfigService, null, null);
    }

    public static void apply(Model model, SysConfigService sysConfigService, HttpServletRequest request, SitePortalSupport sitePortalSupport) {
        if (model == null || sysConfigService == null) {
            return;
        }
        if (!model.containsAttribute("bodyClass")) {
            model.addAttribute("bodyClass", "login-page-v2");
        }
        model.addAttribute("siteName", sysConfigService.getLoadingBrand());
        model.addAttribute("registerEnabled", sysConfigService.isRegisterEnabled());
        model.addAttribute("forgotPasswordEnabled", sysConfigService.isForgotPasswordEnabled());
        model.addAttribute("skipAutologinCookie", LogoutSkipSupport.COOKIE_NAME);
        boolean devAutologinEnabled = sysConfigService.getAccountAuthConfig() != null && sysConfigService.getAccountAuthConfig().isDevAutologinEnabled();
        model.addAttribute("devAutologinEnabled", devAutologinEnabled);
        if (sitePortalSupport != null) {
            sitePortalSupport.applyToModel(request, model);
        }
    }

    /**
     * 登录页 OAuth callback 服务端净化，写入 {@code safeOauthCallback} 供 {@code __LOGIN_CONFIG__} 使用。
     */
    public static void applySafeOauthCallback(HttpServletRequest request, Model model) {
        if (request == null || model == null) {
            return;
        }
        String raw = request.getParameter("callback");
        if (StringUtils.isBlank(raw)) {
            raw = Utils.getCallback(request);
        }
        if (StringUtils.isBlank(raw) && (Boolean.TRUE.equals(model.getAttribute(ATTR_OAUTH_AUTHORIZE)) || Boolean.TRUE.equals(model.getAttribute(ATTR_OPL_AUTHORIZE)))) {
            StringBuffer url = request.getRequestURL();
            String query = request.getQueryString();
            raw = StringUtils.isNotBlank(query) ? url + "?" + query : url.toString();
        }
        model.addAttribute("safeOauthCallback", WebPathUtils.canonicalOauthLoginCallback(request, raw));
    }
}
