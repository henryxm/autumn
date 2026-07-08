package cn.org.autumn.modules.oauth.oauth2;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.apache.oltu.oauth2.common.OAuth.*;
import static org.apache.oltu.oauth2.common.OAuth.HttpMethod.POST;
import static org.apache.oltu.oauth2.common.error.OAuthError.OAUTH_ERROR_URI;
import static org.apache.oltu.oauth2.common.error.OAuthError.TokenResponse.*;

import cn.org.autumn.modules.oauth.oauth2.support.OAuthAccessTokenResolver;
import cn.org.autumn.modules.oauth.oauth2.support.OAuthAuthorizeAppIconSupport;
import cn.org.autumn.modules.oauth.oauth2.support.OAuthConsentCsrfSupport;
import cn.org.autumn.modules.oauth.oauth2.support.OAuthConsentSupport;
import cn.org.autumn.modules.oauth.oauth2.support.OAuthRedirectSupport;
import cn.org.autumn.modules.oauth.oauth2.support.OAuthResponseSupport;
import cn.org.autumn.modules.oauth.entity.ClientDetailsEntity;
import cn.org.autumn.modules.oauth.entity.TokenStoreEntity;
import cn.org.autumn.modules.oauth.service.ClientDetailsService;
import cn.org.autumn.modules.oauth.service.TokenStoreService;
import cn.org.autumn.modules.oauth.store.TokenStore;
import cn.org.autumn.modules.oauth.store.ValueType;
import cn.org.autumn.modules.qrc.model.TicketSnapshot;
import cn.org.autumn.modules.qrc.service.ClientGrantService;
import cn.org.autumn.modules.qrc.service.ScanTicketService;
import cn.org.autumn.modules.spm.service.SuperPositionModelService;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.modules.sys.shiro.LogoutSkipSupport;
import cn.org.autumn.modules.sys.shiro.ShiroSessionService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.sys.support.SysLogoutSupport;
import cn.org.autumn.modules.usr.dto.UserProfile;
import cn.org.autumn.modules.usr.entity.UserProfileEntity;
import cn.org.autumn.modules.usr.service.UserLoginLogService;
import cn.org.autumn.modules.usr.service.UserProfileService;
import cn.org.autumn.modules.client.oauth2.WebOauthEndpointResolver;
import cn.org.autumn.modules.client.service.AuthSiteRoleService;
import cn.org.autumn.modules.client.service.OauthRpLoginService;
import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.client.service.WebAuthenticationService;
import cn.org.autumn.site.AuthPageAttributes;
import cn.org.autumn.site.AuthPageSupport;
import cn.org.autumn.site.PageFactory;
import cn.org.autumn.utils.IPUtils;
import cn.org.autumn.utils.Utils;
import cn.org.autumn.utils.WebPathUtils;
import com.alibaba.fastjson.JSON;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;

import java.net.*;
import java.io.UnsupportedEncodingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.oltu.oauth2.as.issuer.MD5Generator;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuerImpl;
import org.apache.oltu.oauth2.as.request.OAuthAuthzRequest;
import org.apache.oltu.oauth2.as.request.OAuthTokenRequest;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.message.types.ParameterStyle;
import org.apache.oltu.oauth2.common.message.types.ResponseType;
import org.apache.oltu.oauth2.common.utils.OAuthUtils;
import org.apache.oltu.oauth2.rs.request.OAuthAccessResourceRequest;
import org.apache.oltu.oauth2.rs.response.OAuthRSResponse;
import org.apache.shiro.authc.*;
import org.apache.shiro.web.util.SavedRequest;
import org.apache.shiro.web.util.WebUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

@Slf4j
@Controller
@RequestMapping("/oauth2")
@Tags({@Tag(name = "oauth", description = "客户端授权登录认证获取Token接口")})
public class AuthorizationController {

    @Autowired
    ClientDetailsService clientDetailsService;

    @Autowired
    SuperPositionModelService superPositionModelService;

    @Autowired
    SysUserService sysUserService;

    @Autowired
    UserProfileService userProfileService;

    @Autowired
    UserLoginLogService userLoginLogService;

    @Autowired
    TokenStoreService tokenStoreService;

    @Autowired
    SysConfigService sysConfigService;

    @Autowired
    PageFactory pageFactory;

    @Autowired
    AuthPageSupport authPageSupport;

    @Autowired
    AuthSiteRoleService authSiteRoleService;

    @Autowired
    OauthRpLoginService oauthRpLoginService;

    @Autowired
    WebAuthenticationService webAuthenticationService;

    @Autowired
    WebOauthEndpointResolver webOauthEndpointResolver;

    @Autowired
    ScanTicketService scanTicketService;

    @Autowired
    ClientGrantService clientGrantService;

    @Autowired
    ShiroSessionService shiroSessionService;

    @Autowired
    OAuthAuthorizeAppIconSupport authorizeAppIconSupport;

    private String oauthErrorBody(String description, String error, int errorResponse) throws OAuthSystemException {
        return OAuthResponseSupport.oauthErrorBody(description, error, errorResponse);
    }

    private String writeOAuthError(HttpServletResponse response, String description, String error, int errorResponse) throws OAuthSystemException {
        return OAuthResponseSupport.writeOAuthError(response, description, error, errorResponse);
    }

    /**
     * RFC 允许对授权 URL 发 HEAD，但 Oltu {@code OAuthAuthzRequest} 仅接受 GET。
     */
    private HttpServletRequest authzRequestForOltu(HttpServletRequest request) {
        if (request == null || !"HEAD".equalsIgnoreCase(request.getMethod())) {
            return request;
        }
        return new HttpServletRequestWrapper(request) {
            @Override
            public String getMethod() {
                return "GET";
            }
        };
    }

    private String normalizeRedirectUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return url;
        }
        return url.replace("&amp;", "&").trim();
    }

    private ModelAndView redirectUrl(String url) {
        RedirectView view = new RedirectView(normalizeRedirectUrl(url), false);
        return new ModelAndView(view);
    }

    private String resolvePostLoginRedirect(HttpServletRequest request, String callback) {
        callback = normalizeRedirectUrl(callback);
        if (StringUtils.isNotBlank(callback)) return WebPathUtils.safePostLoginRedirect(request, callback);
        SavedRequest savedRequest = WebUtils.getSavedRequest(request);
        if (savedRequest != null && StringUtils.isNotBlank(savedRequest.getRequestUrl())) {
            String safe = WebPathUtils.safePostLoginRedirect(request, savedRequest.getRequestUrl());
            String home = WebPathUtils.forBrowser(request, "/");
            if (!home.equals(safe)) return safe;
        }
        if (superPositionModelService != null && superPositionModelService.menuWithSpm())
            return WebPathUtils.forBrowser(request, "/");
        return WebPathUtils.forBrowser(request, "/");
    }

    private String appendQueryParam(String url, String key, String value) throws UnsupportedEncodingException {
        if (StringUtils.isBlank(url) || StringUtils.isBlank(key)) {
            return url;
        }
        String sep = url.contains("?") ? "&" : "?";
        return url + sep + key + "=" + URLEncoder.encode(StringUtils.defaultString(value), "utf-8");
    }

    private String loginAuthorizeView(HttpServletRequest request, HttpServletResponse response, Model model, TicketSnapshot ticket, ClientDetailsEntity client, String mode) {
        return oauthConsentView(request, response, model, ticket, client, !ShiroUtils.needLogin(), null);
    }

    private String buildAuthorizeReturnUrl(HttpServletRequest request, Model model, ClientDetailsEntity client) {
        String base = WebPathUtils.forBrowser(request, "/oauth2/authorize");
        String qs = request != null ? request.getQueryString() : null;
        if (StringUtils.isNotBlank(qs) && qs.contains("client_id=")) {
            return base + "?" + qs;
        }
        try {
            String responseType = StringUtils.defaultIfBlank((String) model.getAttribute("responseType"), ResponseType.CODE.toString());
            String redirectUri = (String) model.getAttribute("redirectUri");
            String scope = (String) model.getAttribute("scope");
            String state = (String) model.getAttribute("state");
            StringBuilder sb = new StringBuilder(base);
            sb.append("?response_type=").append(URLEncoder.encode(responseType, "utf-8"));
            sb.append("&client_id=").append(URLEncoder.encode(client.getClientId(), "utf-8"));
            sb.append("&redirect_uri=").append(URLEncoder.encode(StringUtils.defaultString(redirectUri), "utf-8"));
            if (StringUtils.isNotBlank(scope)) {
                sb.append("&scope=").append(URLEncoder.encode(scope, "utf-8"));
            }
            if (StringUtils.isNotBlank(state)) {
                sb.append("&state=").append(URLEncoder.encode(state, "utf-8"));
            }
            return sb.toString();
        } catch (UnsupportedEncodingException e) {
            return base;
        }
    }

    private String buildAuthorizeLogoutUrl(HttpServletRequest request, String returnUrl) {
        try {
            String base = WebPathUtils.forBrowser(request, "/oauth2/authorize/logout");
            return base + "?returnTo=" + URLEncoder.encode(returnUrl, "utf-8");
        } catch (UnsupportedEncodingException e) {
            return WebPathUtils.forBrowser(request, "/oauth2/authorize/logout");
        }
    }

    private String oauthConsentView(HttpServletRequest request, HttpServletResponse response, Model model, TicketSnapshot ticket, ClientDetailsEntity client, boolean loggedIn, String consentError) {
        if (ticket != null) {
            scanTicketService.fillAuthorizeModel(model, ticket);
        } else {
            model.addAttribute("pollIntervalMs", scanTicketService.getScanLoginConfig().getPollIntervalMs());
        }
        model.addAttribute("oauthLogin", true);
        model.addAttribute("oauthAuthorize", true);
        model.addAttribute("bodyClass", "login-page-v2 oauth-authorize-mode");
        model.addAttribute("authorizeLoggedIn", loggedIn);
        String clientName = client.getClientName();
        if (StringUtils.isBlank(clientName)) {
            clientName = client.getClientId();
        }
        model.addAttribute("clientName", clientName);
        AuthPageAttributes.applyConsentApp(model, clientName, authorizeAppIconSupport.resolveByClient(client));
        model.addAttribute("clientId", client.getClientId());
        if (loggedIn) {
            SysUserEntity user = ShiroUtils.getUserEntity();
            if (user != null) {
                model.addAttribute("loginUserName", StringUtils.isNotBlank(user.getUsername()) ? user.getUsername() : user.getUuid());
            }
            if (request != null) {
                String returnUrl = buildAuthorizeReturnUrl(request, model, client);
                model.addAttribute("authorizeLogoutUrl", buildAuthorizeLogoutUrl(request, returnUrl));
            }
        }
        if (StringUtils.isNotBlank(consentError)) {
            model.addAttribute("error", consentError);
        }
        try {
            String view = pageFactory.oauthAuthorize(request, response, model);
            return StringUtils.isNotBlank(view) ? view : "login";
        } catch (Exception e) {
            log.warn("OAuth consent view failed: {}", e.getMessage(), e);
            return "login";
        }
    }

    private ModelAndView issueAuthCodeRedirect(HttpServletRequest request, ClientDetailsEntity client, String redirectURI, String state, String callback, String responseType) throws OAuthSystemException {
        String authCode = null;
        if (responseType.equals(ResponseType.CODE.toString())) {
            OAuthIssuerImpl oAuthIssuerImpl = new OAuthIssuerImpl(new MD5Generator());
            authCode = oAuthIssuerImpl.authorizationCode();
            clientDetailsService.putAuthCode(authCode, ShiroUtils.getUserEntity());
        }
        if (StringUtils.isNotBlank(authCode)) {
            return redirectUrl(clientGrantService.buildAuthorizeRedirect(redirectURI, authCode, state, callback));
        }
        return redirectUrl(redirectURI);
    }

    private String buildAccessDeniedRedirect(String redirectURI, String state) {
        return OAuthRedirectSupport.buildAccessDeniedRedirect(redirectURI, state);
    }

    private Object oauthLoginFallbackRedirect(HttpServletRequest request, HttpServletResponse response, String error) throws OAuthSystemException {
        try {
            ModelAndView mav = new ModelAndView();
            String url = request.getRequestURL().toString();
            String queryString = request.getQueryString();
            if (StringUtils.isNotEmpty(queryString)) {
                url = url + "?" + queryString;
            }
            mav.addObject("callback", url);
            String view = "redirect:/oauth2/login";
            if (StringUtils.isNotBlank(error)) {
                view = view + "?error=" + URLEncoder.encode(error, "utf-8");
            }
            mav.setViewName(view);
            return mav;
        } catch (Exception e) {
            log.warn("OAuth login fallback redirect failed: {}", e.getMessage());
            return writeOAuthError(response, StringUtils.isNotBlank(error) ? error : "授权暂不可用", OAUTH_ERROR_URI, SC_BAD_REQUEST);
        }
    }

    @RequestMapping("login")
    public Object login(HttpServletRequest request, HttpServletResponse response, String username, String password, boolean rememberMe, Model model) {
        if (!request.getMethod().equalsIgnoreCase(POST)) {
            return loginGet(request, response, model);
        }
        return loginPost(request, response, username, password, rememberMe, "login", "默认登录", model);
    }

    private Object loginGet(HttpServletRequest request, HttpServletResponse response, Model model) {
        if ("login".equals(request.getParameter("redirect"))) {
            return redirectUrl(WebPathUtils.forBrowser(request, "/login"));
        }
        String error = request.getParameter("error");
        String callback = normalizeRedirectUrl(request.getParameter("callback"));
        if (StringUtils.isBlank(callback)) {
            callback = normalizeRedirectUrl(Utils.getCallback(request));
        }
        if (StringUtils.isNotBlank(callback)) {
            String clientId = authSiteRoleService.resolveRpClientId(request);
            String canonicalUrl = WebPathUtils.oauthLoginEntryUrlIfCallbackNeedsCanonical(request, "/oauth2/login", "client_id", clientId, callback);
            if (StringUtils.isNotBlank(canonicalUrl)) {
                return redirectUrl(canonicalUrl);
            }
            callback = WebPathUtils.canonicalOauthLoginCallback(request, callback);
        }
        if (StringUtils.isNotBlank(callback) || StringUtils.isNotBlank(error)) {
            if (StringUtils.isNotBlank(error)) {
                model.addAttribute("error", error);
            }
            if (StringUtils.isNotBlank(callback)) {
                model.addAttribute("callback", callback);
            }
            return pageFactory.login(request, response, model);
        }
        Object rpEntry = tryRpOAuthLoginEntry(request, response, model);
        if (rpEntry != null) {
            return rpEntry;
        }
        return redirectUrl(WebPathUtils.forBrowser(request, "/login"));
    }

    private Object tryRpOAuthLoginEntry(HttpServletRequest request, HttpServletResponse response, Model model) {
        if (!authSiteRoleService.isRpEnabled()) {
            return null;
        }
        String clientId = authSiteRoleService.resolveRpClientId(request);
        if (StringUtils.isBlank(clientId)) {
            return null;
        }
        WebAuthenticationEntity rpClient = webAuthenticationService.getByClientId(clientId);
        if (rpClient == null) {
            return null;
        }
        if (webOauthEndpointResolver.hasRemoteOrigin(rpClient)) {
            try {
                String safeCallback = WebPathUtils.safeOauthCallbackForClient(request, request.getParameter("callback"));
                return redirectUrl(oauthRpLoginService.buildAuthorizeRedirect(request, safeCallback));
            } catch (Exception e) {
                log.warn("RP authorize redirect failed: clientId={}, {}", clientId, e.getMessage());
                return null;
            }
        }
        authPageSupport.prepareOauthLoginEntry(request, model, clientId);
        return pageFactory.oauthLoginEntry(request, response, model);
    }

    public Object loginPost(HttpServletRequest request, HttpServletResponse response, String username, String password, boolean rememberMe, String way, String reason, Model model) {
        String error = "";
        if (model.containsAttribute("error")) {
            Object obj = model.getAttribute("error");
            error = String.valueOf(obj);
        }
        String callback = normalizeRedirectUrl(request.getParameter("callback"));
        if (StringUtils.isBlank(callback)) {
            callback = normalizeRedirectUrl(Utils.getCallback(request));
        }
        try {
            if (StringUtils.isBlank(error) && request.getMethod().equalsIgnoreCase(POST)) {
                String back = resolvePostLoginRedirect(request, callback);
                sysUserService.login(username, password, rememberMe, true, way, reason, request);
                try {
                    String ip = IPUtils.getIp(request);
                    SysUserEntity userEntity = ShiroUtils.getUserEntity();
                    if (null != userEntity) {
                        userProfileService.updateLoginIp(userEntity.getUuid(), ip, request.getHeader("user-agent"));
                    }
                } catch (Exception ignored) {
                }
                return redirectUrl(back);
            }
        } catch (UnknownAccountException e) {
            error = e.getMessage();
        } catch (IncorrectCredentialsException e) {
            error = "账号或密码不正确";
        } catch (LockedAccountException e) {
            error = "账号已被锁定,请联系管理员";
        } catch (AuthenticationException e) {
            error = "账户验证失败";
        }
        try {
            if (StringUtils.isNotBlank(error)) {
                if (error.length() > 200) error = error.substring(0, 200);
                String safeCallback = WebPathUtils.safeOauthCallbackForClient(request, callback);
                if (StringUtils.isNotBlank(safeCallback)) {
                    return redirectUrl(appendQueryParam(safeCallback, "error", error));
                }
                return "redirect:/oauth2/login?error=" + URLEncoder.encode(error, "utf-8");
            }
        } catch (Exception ignored) {
        }
        String perror = request.getParameter("error");
        if (StringUtils.isBlank(perror)) perror = "";
        model.addAttribute("error", perror);
        return pageFactory.login(request, response, model);
    }

    @RequestMapping(value = "authorize", method = {RequestMethod.GET, RequestMethod.HEAD})
    public String applyAuthorize(HttpServletRequest request, HttpServletResponse response, Model model) throws OAuthSystemException {
        try {
            return applyAuthorizeInternal(request, response, model);
        } catch (OAuthProblemException e) {
            log.warn("OAuth authorize request invalid: {}", e.getMessage());
            return writeOAuthError(response, StringUtils.defaultIfBlank(e.getDescription(), "授权请求无效"), OAuthError.OAUTH_ERROR, SC_BAD_REQUEST);
        }
    }

    private String applyAuthorizeInternal(HttpServletRequest request, HttpServletResponse response, Model model) throws OAuthSystemException, OAuthProblemException {
        OAuthAuthzRequest oAuthzRequest = new OAuthAuthzRequest(authzRequestForOltu(request));
        String clientId = oAuthzRequest.getClientId();
        ClientDetailsEntity clientDetailsEntity = clientDetailsService.findByClientId(clientId);
        if (clientDetailsEntity == null) {
            return writeOAuthError(response, "无效的客户端ID", INVALID_CLIENT, SC_BAD_REQUEST);
        }
        if (null == clientDetailsEntity.getTrusted() || 0 == clientDetailsEntity.getTrusted()) {
            return writeOAuthError(response, "不受信任的客户端ID", INVALID_CLIENT, SC_BAD_REQUEST);
        }
        if (null != clientDetailsEntity.getArchived() && 1 == clientDetailsEntity.getArchived()) {
            return writeOAuthError(response, "客户端ID已归档，不能使用", INVALID_CLIENT, SC_BAD_REQUEST);
        }
        String redirectURI = oAuthzRequest.getParam(OAuth.OAUTH_REDIRECT_URI);
        if (StringUtils.isEmpty(redirectURI) || StringUtils.isEmpty(clientDetailsEntity.getRedirectUri())) {
            return writeOAuthError(response, "redirect_uri should not be empty.", OAUTH_ERROR_URI, SC_BAD_REQUEST);
        }
        if (!redirectURI.equalsIgnoreCase(clientDetailsEntity.getRedirectUri())) {
            return writeOAuthError(response, "redirect_uri does not match the uri in system.", OAUTH_ERROR_URI, SC_BAD_REQUEST);
        }
        String state = request.getParameter(OAUTH_STATE);
        String callback = request.getParameter("callback");
        String scope = oAuthzRequest.getParam(OAuth.OAUTH_SCOPE);
        String responseType = oAuthzRequest.getParam(OAuth.OAUTH_RESPONSE_TYPE);
        if (StringUtils.isBlank(responseType)) {
            responseType = ResponseType.CODE.toString();
        }
        model.addAttribute("redirectUri", redirectURI);
        model.addAttribute("scope", scope);
        model.addAttribute("state", state);
        model.addAttribute("responseType", responseType);
        model.addAttribute("denyRedirect", buildAccessDeniedRedirect(redirectURI, state));
        boolean loggedIn = !ShiroUtils.needLogin();
        if ("1".equals(request.getParameter("loggedOut"))) {
            ShiroUtils.logout();
            loggedIn = false;
        }
        String loginError = request.getParameter("error");
        if (!loggedIn) {
            if (scanTicketService.shouldUseQrAuthorize()) {
                try {
                    TicketSnapshot ticket = scanTicketService.createAuthorizeTicket(request, clientId, redirectURI, scope, state, callback);
                    return oauthConsentView(request, response, model, ticket, clientDetailsEntity, false, loginError);
                } catch (Exception e) {
                    log.warn("QRC authorize ticket failed: {}", e.getMessage());
                }
            }
            return oauthConsentView(request, response, model, null, clientDetailsEntity, false, loginError);
        }
        return oauthConsentView(request, response, model, null, clientDetailsEntity, true, loginError);
    }

    @RequestMapping(value = "authorize/logout", method = RequestMethod.GET)
    public ModelAndView authorizeLogout(HttpServletRequest request, HttpServletResponse response, @RequestParam(required = false) String returnTo) {
        SysLogoutSupport.logoutAndForceReauth(shiroSessionService);
        LogoutSkipSupport.mark(request, response);
        String url = normalizeRedirectUrl(returnTo);
        if (StringUtils.isBlank(url)) {
            url = WebPathUtils.forBrowser(request, "/oauth2/authorize");
        } else {
            url = WebPathUtils.safePostLoginRedirect(request, url);
        }
        return redirectUrl(url);
    }

    @RequestMapping(value = "authorize/approve", method = RequestMethod.POST)
    public Object approveAuthorize(HttpServletRequest request, HttpServletResponse response, @RequestParam(name = OAUTH_CLIENT_ID) String clientId, @RequestParam(OAUTH_REDIRECT_URI) String redirectURI, @RequestParam(name = OAUTH_RESPONSE_TYPE, defaultValue = "code") String responseType, @RequestParam(required = false) String scope, @RequestParam(required = false) String state, @RequestParam(required = false) String callback, @RequestParam(name = "userConsent", defaultValue = "false") String userConsent, @RequestParam(name = "consentCsrf", required = false) String consentCsrf, Model model) throws OAuthSystemException, OAuthProblemException {
        if (ShiroUtils.needLogin()) {
            return oauthLoginFallbackRedirect(request, response, "请先登录后再确认授权");
        }
        if (!OAuthConsentCsrfSupport.validateAndConsume(request, consentCsrf)) {
            ClientDetailsEntity clientForCsrf = clientDetailsService.findByClientId(clientId);
            if (clientForCsrf == null) {
                return writeOAuthError(response, "无效的客户端ID", INVALID_CLIENT, SC_BAD_REQUEST);
            }
            model.addAttribute("redirectUri", redirectURI);
            model.addAttribute("scope", scope);
            model.addAttribute("state", state);
            model.addAttribute("responseType", responseType);
            model.addAttribute("denyRedirect", buildAccessDeniedRedirect(redirectURI, state));
            return oauthConsentView(request, response, model, null, clientForCsrf, true, "授权请求已过期，请刷新页面后重试");
        }
        if (!OAuthConsentSupport.consented(userConsent)) {
            ClientDetailsEntity client = clientDetailsService.findByClientId(clientId);
            if (client == null) {
                return writeOAuthError(response, "无效的客户端ID", INVALID_CLIENT, SC_BAD_REQUEST);
            }
            model.addAttribute("redirectUri", redirectURI);
            model.addAttribute("scope", scope);
            model.addAttribute("state", state);
            model.addAttribute("responseType", responseType);
            model.addAttribute("denyRedirect", buildAccessDeniedRedirect(redirectURI, state));
            return oauthConsentView(request, response, model, null, client, true, "请勾选授权协议后再确认");
        }
        ClientDetailsEntity clientDetailsEntity = clientDetailsService.findByClientId(clientId);
        if (clientDetailsEntity == null) {
            return writeOAuthError(response, "无效的客户端ID", INVALID_CLIENT, SC_BAD_REQUEST);
        }
        if (null == clientDetailsEntity.getTrusted() || 0 == clientDetailsEntity.getTrusted()) {
            return writeOAuthError(response, "不受信任的客户端ID", INVALID_CLIENT, SC_BAD_REQUEST);
        }
        if (null != clientDetailsEntity.getArchived() && 1 == clientDetailsEntity.getArchived()) {
            return writeOAuthError(response, "客户端ID已归档，不能使用", INVALID_CLIENT, SC_BAD_REQUEST);
        }
        if (StringUtils.isEmpty(redirectURI) || StringUtils.isEmpty(clientDetailsEntity.getRedirectUri())) {
            return writeOAuthError(response, "redirect_uri should not be empty.", OAUTH_ERROR_URI, SC_BAD_REQUEST);
        }
        if (!redirectURI.equalsIgnoreCase(clientDetailsEntity.getRedirectUri())) {
            return writeOAuthError(response, "redirect_uri does not match the uri in system.", OAUTH_ERROR_URI, SC_BAD_REQUEST);
        }
        if (StringUtils.isBlank(responseType)) {
            responseType = ResponseType.CODE.toString();
        }
        return issueAuthCodeRedirect(request, clientDetailsEntity, redirectURI, state, callback, responseType);
    }

    @RequestMapping(value = "token", method = RequestMethod.POST)
    @ResponseBody
    @Operation(tags = {"oauth"}, servers = {@Server(url = "https://www.minclouds.com", description = "用户授权登录获取Token接口")}, description = "获取Token", summary = "获取Token", operationId = "getToken", method = POST)
    public String applyAccessToken(HttpServletRequest request, @Validated @RequestParam(name = OAUTH_CLIENT_ID) String clientId, @Validated @RequestParam(OAUTH_CLIENT_SECRET) String clientSecret) throws OAuthSystemException, OAuthProblemException {
        //构建OAuth请求
        String username = null;
        String password = null;
        String refresh = null;
        String authCode = null;
        String grantType = null;
        try {
            OAuthTokenRequest tokenRequest = new OAuthTokenRequest(request);
            clientId = tokenRequest.getClientId();
            clientSecret = tokenRequest.getClientSecret();
            grantType = tokenRequest.getParam(OAuth.OAUTH_GRANT_TYPE);
            authCode = tokenRequest.getParam(OAuth.OAUTH_CODE);
            refresh = tokenRequest.getRefreshToken();
            username = tokenRequest.getUsername();
            password = tokenRequest.getPassword();
        } catch (Exception e) {
            log.debug("OAuthTokenRequest:{}", e.getMessage());
            if (null == grantType) {
                grantType = GrantType.CLIENT_CREDENTIALS.toString();
            }
        }
        //校验客户端Id是否正确
        ClientDetailsEntity authClient = clientDetailsService.findByClientId(clientId);
        if (authClient == null) {
            return oauthErrorBody("无效的客户端Id", INVALID_CLIENT, SC_BAD_REQUEST);
        }
        if (null == authClient.getTrusted() || 0 == authClient.getTrusted()) {
            return oauthErrorBody("不受信任的客户端ID", INVALID_CLIENT, SC_BAD_REQUEST);
        }
        if (null != authClient.getArchived() && 1 == authClient.getArchived()) {
            return oauthErrorBody("客户端ID已归档，不能使用", INVALID_CLIENT, SC_BAD_REQUEST);
        }
        //检查客户端安全KEY是否正确
        if (!clientDetailsService.isValidClientSecret(clientSecret)) {
            return oauthErrorBody("客户端安全KEY认证失败！", UNAUTHORIZED_CLIENT, SC_UNAUTHORIZED);
        }
        if (StringUtils.isBlank(grantType)) return oauthErrorBody("非法授权", INVALID_GRANT, SC_BAD_REQUEST);
        if (!authClient.granted(grantType)) return oauthErrorBody("未获得授权", INVALID_GRANT, SC_BAD_REQUEST);
        TokenStore tokenStore = null;
        //验证类型，有AUTHORIZATION_CODE/PASSWORD/REFRESH_TOKEN/CLIENT_CREDENTIALS
        //1. 授权码获取Token模式
        if (grantType.equals(GrantType.AUTHORIZATION_CODE.toString())) {
            if (!clientDetailsService.isValidCode(authCode)) {
                return oauthErrorBody("错误的授权码", INVALID_GRANT, SC_BAD_REQUEST);
            }
            tokenStore = clientDetailsService.get(ValueType.authCode, authCode);
        }
        //2. 使用Refresh Token 获取Token模式
        if (grantType.equals(GrantType.REFRESH_TOKEN.toString())) {
            if (!clientDetailsService.isValidRefreshToken(refresh)) {
                return oauthErrorBody("错误的Refresh Token", INVALID_GRANT, SC_BAD_REQUEST);
            }
            tokenStore = clientDetailsService.get(ValueType.refreshToken, refresh);
        }
        //3.客户端证书授权模式
        if (grantType.equals(GrantType.CLIENT_CREDENTIALS.toString())) {
            SysUserEntity sysUserEntity = sysUserService.getByUsername(authClient.getClientId());
            if (null == sysUserEntity) {
                clientDetailsService.clientToUser();
                sysUserEntity = sysUserService.getByUsername(authClient.getClientId());
            }
            tokenStore = new TokenStore(sysUserEntity);
        }
        //4.密码授权模式
        if (grantType.equals(GrantType.PASSWORD.toString())) {
            sysUserService.login(username, password, false);
            boolean isLogin = ShiroUtils.isLogin();
            if (isLogin) {
                SysUserEntity sysUserEntity = sysUserService.getByUsername(username);
                tokenStore = new TokenStore(sysUserEntity);
                userProfileService.updateLoginIp(sysUserEntity.getUuid(), IPUtils.getIp(request), request.getHeader("user-agent"));
            }
        }
        //5. 简化授权模式
        if (grantType.equals(GrantType.IMPLICIT.toString())) {
            return oauthErrorBody("unsupported_grant_type", UNSUPPORTED_GRANT_TYPE, SC_BAD_REQUEST);
        }
        if (null == tokenStore) {
            return oauthErrorBody("非法授权", INVALID_GRANT, SC_BAD_REQUEST);
        }
        String accessToken = "";
        String refreshToken = "";
        if (null != tokenStore.getValue() && sysConfigService.currentToken() && tokenStore.getValue() instanceof SysUserEntity) {
            SysUserEntity sysUserEntity = (SysUserEntity) tokenStore.getValue();
            TokenStoreEntity tokenStoreEntity = tokenStoreService.findByUser(sysUserEntity);
            if (null != tokenStoreEntity && StringUtils.isNotBlank(tokenStoreEntity.getAccessToken()) && StringUtils.isNotBlank(tokenStoreEntity.getRefreshToken())) {
                boolean validA = clientDetailsService.isValidAccessToken(tokenStoreEntity.getAccessToken());
                boolean validR = clientDetailsService.isValidRefreshToken(tokenStoreEntity.getRefreshToken());
                if (validA && validR) {
                    accessToken = tokenStoreEntity.getAccessToken();
                    refreshToken = tokenStoreEntity.getRefreshToken();
                }
            }
        }
        if (StringUtils.isBlank(accessToken) || StringUtils.isBlank(refreshToken)) {
            //生成访问令牌
            OAuthIssuerImpl authIssuerImpl = new OAuthIssuerImpl(new MD5Generator());
            accessToken = authIssuerImpl.accessToken();
            refreshToken = authIssuerImpl.refreshToken();
            /**
             * accessToken  过期时间：24 * 60 * 60L      一天
             * refreshToken 过期时间：7 * 24 * 60 * 60L  一周
             * 如果服务器重启或者Redis重启后，将立即过期
             */
            clientDetailsService.putToken(accessToken, refreshToken, tokenStore);
        }
        TokenStore store = clientDetailsService.get(ValueType.accessToken, accessToken);
        //生成OAuth响应
        OAuthResponse response = OAuthASResponse.tokenResponse(HttpServletResponse.SC_OK).setAccessToken(accessToken).setRefreshToken(refreshToken).setTokenType("bearer").setExpiresIn(String.valueOf(store.getExpireIn())).setScope("basic").buildJSONMessage();
        return new ResponseEntity<>(response.getBody(), HttpStatus.valueOf(response.getResponseStatus())).getBody();
    }

    @RequestMapping(value = "/userInfo", produces = "application/json;charset=UTF-8", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    @Operation(tags = {"oauth"}, servers = {@Server(url = "https://www.minclouds.com", description = "获取用户信息")}, description = "获取用户信息", summary = "获取用户信息", operationId = "getUserInfo")
    public HttpEntity<?> authUserInfo(HttpServletRequest request) throws OAuthSystemException {
        try {
            String accessTokenKey = OAuthAccessTokenResolver.resolve(request, OAuthAccessTokenResolver.Policy.LEGACY_JSON_WRAP);
            if (StringUtils.isBlank(accessTokenKey)) {
                OAuthResponse oauthResponse = OAuthRSResponse.errorResponse(SC_UNAUTHORIZED).setRealm("Apache Oltu").setError(OAuthError.ResourceResponse.INVALID_TOKEN).buildHeaderMessage();
                HttpHeaders headers = new HttpHeaders();
                headers.add(OAuth.HeaderType.WWW_AUTHENTICATE, oauthResponse.getHeader(OAuth.HeaderType.WWW_AUTHENTICATE));
                return new ResponseEntity<>(headers, HttpStatus.UNAUTHORIZED);
            }
            if (!clientDetailsService.isValidAccessToken(accessTokenKey)) {
                // 如果不存在/过期了，返回未验证错误，需重新验证
                OAuthResponse oauthResponse = OAuthRSResponse.errorResponse(SC_UNAUTHORIZED).setRealm("Apache Oltu").setError(OAuthError.ResourceResponse.INVALID_TOKEN).buildHeaderMessage();
                HttpHeaders headers = new HttpHeaders();
                headers.add(OAuth.HeaderType.WWW_AUTHENTICATE, oauthResponse.getHeader(OAuth.HeaderType.WWW_AUTHENTICATE));
                return new ResponseEntity<>(headers, HttpStatus.UNAUTHORIZED);
            }
            //返回用户名
            TokenStore tokenStore = clientDetailsService.get(ValueType.accessToken, accessTokenKey);
            if (null != tokenStore) {
                Object username = tokenStore.getValue();
                if (username instanceof SysUserEntity) {
                    SysUserEntity sysUserEntity = (SysUserEntity) username;
                    UserProfileEntity profile = userProfileService.from(sysUserEntity);
                    username = UserProfile.from(profile);
                    userLoginLogService.login(profile, "userInfo", "用户查询", request);
                }
                return new ResponseEntity<>(JSON.toJSONString(username), HttpStatus.OK);
            }
        } catch (Exception e) {
            log.debug("authUserInfo failed: {}", e.getMessage());
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
}
