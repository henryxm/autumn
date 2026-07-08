package cn.org.autumn.site;

import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.client.service.WebAuthenticationService;
import cn.org.autumn.modules.oauth.oauth2.support.OAuthConsentCsrfSupport;
import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import cn.org.autumn.modules.opc.service.ConnectAppService;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.opl.OplConstants;
import cn.org.autumn.utils.WebPathUtils;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

/**
 * 授权流程页面 Model 准备与客户端解析，供 {@link PageFactory} 默认实现及业务扩展参考。
 */
@Component
public class AuthPageSupport {

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private WebAuthenticationService webAuthenticationService;

    @Autowired
    private ConnectAppService connectAppService;

    @Autowired
    private SitePortalSupport sitePortalSupport;

    public void prepareOauthLoginEntry(HttpServletRequest request, Model model, String clientId) {
        AuthPageAttributes.apply(model, sysConfigService, request, sitePortalSupport);
        AuthPageAttributes.markFlowKind(model, AuthPageAttributes.FLOW_OAUTH_LOGIN_ENTRY);
        WebAuthenticationEntity webAuth = resolveWebAuth(request, clientId);
        if (webAuth != null) {
            model.addAttribute("clientId", webAuth.getClientId());
            model.addAttribute("clientName", webAuth.getName());
            model.addAttribute("redirectUri", webAuth.getRedirectUri());
            model.addAttribute("sameInstance", WebPathUtils.isSameSiteUrl(webAuth.getOriginUri(), sysConfigService.getBaseUrl()));
        } else if (StringUtils.isNotBlank(clientId)) {
            model.addAttribute("clientId", clientId);
            model.addAttribute("redirectUri", WebPathUtils.forBrowser(request, "/client/oauth2/callback"));
        }
        AuthPageAttributes.applyAuthFlowBoot(request, model);
    }

    public void prepareOauthLoginSuccess(HttpServletRequest request, Model model) {
        AuthPageAttributes.apply(model, sysConfigService, request, sitePortalSupport);
        AuthPageAttributes.markFlowKind(model, AuthPageAttributes.FLOW_OAUTH_LOGIN_SUCCESS);
        AuthPageAttributes.applyAuthFlowBoot(request, model);
    }

    public void prepareOpenLoginEntry(HttpServletRequest request, Model model, String appId) {
        AuthPageAttributes.apply(model, sysConfigService, request, sitePortalSupport);
        AuthPageAttributes.markFlowKind(model, AuthPageAttributes.FLOW_OPEN_LOGIN_ENTRY);
        if (StringUtils.isNotBlank(appId)) {
            ConnectAppEntity app = connectAppService.getByAppId(appId);
            model.addAttribute("appId", appId);
            if (app != null) {
                model.addAttribute("appName", app.getName());
            }
        }
        AuthPageAttributes.applySafeOauthCallback(request, model);
        AuthPageAttributes.applyAuthFlowBoot(request, model);
    }

    public void prepareOpenLoginSuccess(HttpServletRequest request, Model model, String appId) {
        AuthPageAttributes.apply(model, sysConfigService, request, sitePortalSupport);
        AuthPageAttributes.markFlowKind(model, AuthPageAttributes.FLOW_OPEN_LOGIN_SUCCESS);
        if (StringUtils.isBlank(appId) && request != null) {
            appId = request.getParameter("app_id");
        }
        if (StringUtils.isNotBlank(appId)) {
            model.addAttribute("appId", appId);
        }
        AuthPageAttributes.applyAuthFlowBoot(request, model);
    }

    public void prepareOauthAuthorize(HttpServletRequest request, Model model) {
        prepareAuthorizePage(request, model, true, false);
    }

    public void prepareOplAuthorize(HttpServletRequest request, Model model) {
        prepareAuthorizePage(request, model, false, true);
    }

    public void prepareAuthorizePage(HttpServletRequest request, Model model, boolean oauthAuthorize, boolean oplAuthorize) {
        if (model == null) {
            return;
        }
        if (oauthAuthorize) {
            model.addAttribute(AuthPageAttributes.ATTR_OAUTH_AUTHORIZE, true);
            AuthPageAttributes.markFlowKind(model, AuthPageAttributes.FLOW_OAUTH_AS_AUTHORIZE);
        }
        if (oplAuthorize) {
            model.addAttribute(AuthPageAttributes.ATTR_OPL_AUTHORIZE, true);
            AuthPageAttributes.markFlowKind(model, AuthPageAttributes.FLOW_OPL_AS_AUTHORIZE);
        }
        boolean authorizeMode = oauthAuthorize || oplAuthorize;
        if (authorizeMode) {
            model.addAttribute("authorizeLoginAction", oplAuthorize ? OplConstants.OAUTH2_LOGIN : "/oauth2/login");
            if (request != null) {
                model.addAttribute("consentCsrfToken", OAuthConsentCsrfSupport.issue(request));
                AuthPageAttributes.applyAuthLoginDefaultIcon(request, model);
            }
            if (!model.containsAttribute("oauthLogin")) {
                model.addAttribute("oauthLogin", true);
            }
            if (!model.containsAttribute("bodyClass")) {
                model.addAttribute("bodyClass", "login-page-v2 oauth-authorize-mode");
            }
        } else if (!model.containsAttribute("oauthLogin")) {
            model.addAttribute("oauthLogin", false);
        }
        if (!authorizeMode && !model.containsAttribute("bodyClass")) {
            model.addAttribute("bodyClass", "login-page-v2");
        }
        if (request != null && !model.containsAttribute("error")) {
            String error = request.getParameter("error");
            if (StringUtils.isNotBlank(error)) {
                model.addAttribute("error", error);
            }
        }
        AuthPageAttributes.apply(model, sysConfigService, request, sitePortalSupport);
        AuthPageAttributes.applySafeOauthCallback(request, model);
        AuthPageAttributes.applyAuthFlowBoot(request, model);
    }

    public WebAuthenticationEntity resolveWebAuth(HttpServletRequest request, String clientId) {
        if (StringUtils.isBlank(clientId) && request != null) {
            clientId = request.getParameter("client_id");
        }
        if (StringUtils.isBlank(clientId)) {
            clientId = sysConfigService.getOauth2LoginClientId();
        }
        WebAuthenticationEntity webAuth = StringUtils.isNotBlank(clientId) ? webAuthenticationService.getByClientId(clientId) : null;
        if (webAuth != null) {
            return webAuth;
        }
        List<WebAuthenticationEntity> list = webAuthenticationService.selectByMap(null);
        if (list != null && !list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }
}
