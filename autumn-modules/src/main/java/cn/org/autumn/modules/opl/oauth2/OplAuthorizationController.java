package cn.org.autumn.modules.opl.oauth2;

import static jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.apache.oltu.oauth2.common.OAuth.HttpMethod.POST;

import cn.org.autumn.modules.oauth.oauth2.support.OAuthAccessTokenResolver;
import cn.org.autumn.modules.oauth.oauth2.support.OAuthAuthorizeAppIconSupport;
import cn.org.autumn.modules.oauth.oauth2.support.OAuthConsentCsrfSupport;
import cn.org.autumn.modules.oauth.oauth2.support.OAuthConsentSupport;
import cn.org.autumn.modules.oauth.oauth2.support.OAuthRedirectSupport;
import cn.org.autumn.modules.oauth.oauth2.support.OAuthResponseSupport;
import cn.org.autumn.modules.opl.entity.OpenAppEntity;
import cn.org.autumn.modules.opl.entity.OpenCodeEntity;
import cn.org.autumn.modules.opl.service.OpenAppService;
import cn.org.autumn.modules.opl.service.OpenCodeService;
import cn.org.autumn.modules.opl.service.OplOAuthRateLimiter;
import cn.org.autumn.modules.opl.service.OplExtensionService;
import cn.org.autumn.modules.opl.support.OplSnapshots;
import cn.org.autumn.modules.qrc.model.TicketSnapshot;
import cn.org.autumn.modules.qrc.service.ScanTicketService;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.modules.sys.shiro.LogoutSkipSupport;
import cn.org.autumn.modules.sys.shiro.ShiroSessionService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.sys.support.SysLogoutSupport;
import cn.org.autumn.modules.usr.service.UserProfileService;
import cn.org.autumn.opl.OplConstants;
import cn.org.autumn.opl.model.OpenAuthorizationRequest;
import cn.org.autumn.opl.model.OpenAppType;
import cn.org.autumn.opl.model.OpenTokenSnapshot;
import cn.org.autumn.opl.model.OpenUserInfoSnapshot;
import cn.org.autumn.opl.spi.OpenPlatformService;
import cn.org.autumn.site.AuthPageAttributes;
import cn.org.autumn.site.PageFactory;
import cn.org.autumn.utils.IPUtils;
import cn.org.autumn.utils.Utils;
import cn.org.autumn.utils.WebPathUtils;
import com.alibaba.fastjson2.JSON;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Enumeration;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.message.types.ResponseType;
import org.apache.oltu.oauth2.rs.response.OAuthRSResponse;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.web.util.SavedRequest;
import org.apache.shiro.web.util.WebUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

@Slf4j
@Controller
@RequestMapping(OplConstants.OAUTH2_BASE)
public class OplAuthorizationController {

    @Autowired
    private OpenAppService openAppService;

    @Autowired
    private OpenCodeService openCodeService;

    @Autowired
    private OpenPlatformService openPlatformService;

    @Autowired
    private OplExtensionService oplExtensionService;

    @Autowired
    private PageFactory pageFactory;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private ScanTicketService scanTicketService;

    @Autowired
    private OplOAuthRateLimiter oplOAuthRateLimiter;

    @Autowired
    private ShiroSessionService shiroSessionService;

    @Autowired
    private OAuthAuthorizeAppIconSupport authorizeAppIconSupport;

    @RequestMapping(value = "authorize", method = {RequestMethod.GET, RequestMethod.HEAD})
    public String authorize(HttpServletRequest request, HttpServletResponse response, Model model,
                            @RequestParam(name = OplConstants.PARAM_APP_ID) String appId,
                            @RequestParam(name = OAuth.OAUTH_REDIRECT_URI) String redirectUri,
                            @RequestParam(name = OAuth.OAUTH_RESPONSE_TYPE, defaultValue = "code") String responseType,
                            @RequestParam(required = false) String scope,
                            @RequestParam(required = false) String state,
                            @RequestParam(required = false, name = "code_challenge") String codeChallenge,
                            @RequestParam(required = false, name = "code_challenge_method") String codeChallengeMethod) {
        try {
            if (!ResponseType.CODE.toString().equalsIgnoreCase(responseType)) {
                return OAuthResponseSupport.writeOAuthError(response, "仅支持authorization_code", OAuthError.OAUTH_ERROR, SC_BAD_REQUEST);
            }
            OpenAppEntity app = openAppService.requireActiveApp(appId);
            openAppService.validateRedirectUri(app, redirectUri);
            String resolvedScope = StringUtils.defaultIfBlank(scope, app.getScope());
            oplExtensionService.validateScope(OplSnapshots.toAppSnapshot(app), resolvedScope);
            OpenAuthorizationRequest authRequest = buildAuthRequest(appId, redirectUri, responseType, resolvedScope, state, null, false);
            authRequest.setAppType(app.getAppType());
            oplExtensionService.beforeAuthorizePage(OplSnapshots.toAppSnapshot(app), authRequest);
            String loginError = request.getParameter("error");
            if (ShiroUtils.needLogin() && scanTicketService.shouldUseQrAuthorize()) {
                try {
                    TicketSnapshot ticket = scanTicketService.createAuthorizeTicket(request, appId, redirectUri, resolvedScope, state, null);
                    return oplConsentView(request, response, model, app, appId, redirectUri, responseType, resolvedScope, state, codeChallenge, codeChallengeMethod, ticket, false, loginError);
                } catch (Exception e) {
                    if (log.isDebugEnabled())
                        log.debug("opl QRC authorize ticket failed: {}", e.getMessage());
                }
            }
            return oplConsentView(request, response, model, app, appId, redirectUri, responseType, resolvedScope, state, codeChallenge, codeChallengeMethod, null, !ShiroUtils.needLogin(), loginError);
        } catch (Exception e) {
            if (log.isDebugEnabled())
                log.debug("opl authorize failed: {}", e.getMessage());
            try {
                return OAuthResponseSupport.writeOAuthError(response, e.getMessage(), OAuthError.OAUTH_ERROR, SC_BAD_REQUEST);
            } catch (OAuthSystemException ex) {
                return "error";
            }
        }
    }

    @RequestMapping(value = "authorize/approve", method = RequestMethod.POST)
    public Object approve(HttpServletRequest request, HttpServletResponse response, Model model,
                          @RequestParam(name = OplConstants.PARAM_APP_ID) String appId,
                          @RequestParam(name = OAuth.OAUTH_REDIRECT_URI) String redirectUri,
                          @RequestParam(name = OAuth.OAUTH_RESPONSE_TYPE, defaultValue = "code") String responseType,
                          @RequestParam(required = false) String scope,
                          @RequestParam(required = false) String state,
                          @RequestParam(name = "userConsent", defaultValue = "false") String userConsent,
                          @RequestParam(name = "consentCsrf", required = false) String consentCsrf,
                          @RequestParam(required = false, name = "code_challenge") String codeChallenge,
                          @RequestParam(required = false, name = "code_challenge_method") String codeChallengeMethod) throws Exception {
        if (ShiroUtils.needLogin()) {
            return oplLoginFallbackRedirect(request, "请先登录后再确认授权");
        }
        if (!OAuthConsentCsrfSupport.validateAndConsume(request, consentCsrf)) {
            OpenAppEntity appForCsrf = openAppService.getByAppId(appId);
            if (appForCsrf == null) {
                return OAuthResponseSupport.writeOAuthError(response, "无效的授权请求", OAuthError.OAUTH_ERROR, SC_BAD_REQUEST);
            }
            return oplConsentView(request, response, model, appForCsrf, appId, redirectUri, responseType, scope, state, codeChallenge, codeChallengeMethod, null, true, "授权请求已过期，请刷新页面后重试");
        }
        OpenAppEntity app = openAppService.requireActiveApp(appId);
        openAppService.validateRedirectUri(app, redirectUri);
        String resolvedScope = StringUtils.defaultIfBlank(scope, app.getScope());
        oplExtensionService.validateScope(OplSnapshots.toAppSnapshot(app), resolvedScope);
        if (!OAuthConsentSupport.consented(userConsent)) {
            return oplConsentView(request, response, model, app, appId, redirectUri, responseType, resolvedScope, state, codeChallenge, codeChallengeMethod, null, true, "请勾选授权协议后再确认");
        }
        if (!ResponseType.CODE.toString().equalsIgnoreCase(responseType)) {
            return OAuthResponseSupport.writeOAuthError(response, "仅支持authorization_code", OAuthError.OAUTH_ERROR, SC_BAD_REQUEST);
        }
        SysUserEntity user = ShiroUtils.getUserEntity();
        OpenAuthorizationRequest authRequest = buildAuthRequest(appId, redirectUri, responseType, resolvedScope, state, user.getUuid(), true);
        authRequest.setAppType(app.getAppType());
        oplExtensionService.beforeApprove(OplSnapshots.toAppSnapshot(app), authRequest);
        OpenCodeEntity codeEntity = openCodeService.issue(appId, user.getUuid(), redirectUri, codeChallenge, codeChallengeMethod);
        oplExtensionService.afterCodeIssued(OplSnapshots.toAppSnapshot(app), authRequest, codeEntity.getCode());
        RedirectView view = new RedirectView(OAuthRedirectSupport.appendCodeAndState(redirectUri, codeEntity.getCode(), state), false);
        return new ModelAndView(view);
    }

    @RequestMapping(value = OplConstants.NS + "/login")
    public Object login(HttpServletRequest request, HttpServletResponse response, String username, String password, boolean rememberMe, Model model) {
        return login(request, response, username, password, rememberMe, "opl-oauth-login", "开放平台授权登录", model);
    }

    public Object login(HttpServletRequest request, HttpServletResponse response, String username, String password, boolean rememberMe, String way, String reason, Model model) {
        Enumeration<String> enumeration = request.getParameterNames();
        if (!enumeration.hasMoreElements()) {
            model.addAttribute("url", WebPathUtils.forBrowser(request, OplConstants.OAUTH2_LOGIN + "?redirect=login"));
            return "direct";
        }
        String error = "";
        if (model.containsAttribute("error")) {
            error = String.valueOf(model.getAttribute("error"));
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
                    if (userEntity != null) {
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
                if (error.length() > 200) {
                    error = error.substring(0, 200);
                }
                String safeCallback = WebPathUtils.safeOauthCallbackForClient(request, callback);
                if (StringUtils.isNotBlank(safeCallback)) {
                    return redirectUrl(appendQueryParam(safeCallback, "error", error));
                }
                return "redirect:" + OplConstants.OAUTH2_LOGIN + "?error=" + URLEncoder.encode(error, "UTF-8");
            }
        } catch (Exception ignored) {
        }
        String perror = request.getParameter("error");
        if (StringUtils.isBlank(perror)) {
            perror = "";
        }
        model.addAttribute("error", perror);
        return pageFactory.login(request, response, model);
    }

    @RequestMapping(value = "token", method = RequestMethod.POST)
    @ResponseBody
    public String token(HttpServletRequest request,
                        @RequestParam(name = OplConstants.PARAM_APP_ID) String appId,
                        @RequestParam(name = OplConstants.PARAM_APP_SECRET) String appSecret,
                        @RequestParam(name = OAuth.OAUTH_GRANT_TYPE) String grantType,
                        @RequestParam(required = false, name = OAuth.OAUTH_CODE) String code,
                        @RequestParam(required = false, name = OAuth.OAUTH_REDIRECT_URI) String redirectUri,
                        @RequestParam(required = false, name = OAuth.OAUTH_REFRESH_TOKEN) String refreshToken,
                        @RequestParam(required = false, name = "code_verifier") String codeVerifier) throws OAuthSystemException {
        try {
            oplOAuthRateLimiter.check(request, appId);
        } catch (IllegalStateException e) {
            return OAuthResponseSupport.oauthErrorBody(e.getMessage(), OAuthError.TokenResponse.INVALID_REQUEST, 429);
        }
        if (!openPlatformService.validateAppSecret(appId, appSecret)) {
            return OAuthResponseSupport.oauthErrorBody("appSecret认证失败", OAuthError.TokenResponse.UNAUTHORIZED_CLIENT, SC_UNAUTHORIZED);
        }
        OpenTokenSnapshot snapshot;
        String responseScope = OplConstants.DEFAULT_SCOPE;
        if (GrantType.AUTHORIZATION_CODE.toString().equalsIgnoreCase(grantType)) {
            if (StringUtils.isBlank(redirectUri)) {
                return OAuthResponseSupport.oauthErrorBody("redirect_uri不能为空", OAuthError.TokenResponse.INVALID_REQUEST, SC_BAD_REQUEST);
            }
            try {
                snapshot = openPlatformService.issueTokenFromCode(appId, code, redirectUri, codeVerifier);
            } catch (IllegalArgumentException e) {
                return OAuthResponseSupport.oauthErrorBody(e.getMessage(), OAuthError.TokenResponse.INVALID_GRANT, SC_BAD_REQUEST);
            }
            if (snapshot == null) {
                return OAuthResponseSupport.oauthErrorBody("授权码无效", OAuthError.TokenResponse.INVALID_GRANT, SC_BAD_REQUEST);
            }
            OpenAppEntity app = openAppService.getByAppId(appId);
            if (app != null && StringUtils.isNotBlank(app.getScope())) {
                responseScope = app.getScope();
            }
        } else if (GrantType.REFRESH_TOKEN.toString().equalsIgnoreCase(grantType)) {
            snapshot = openPlatformService.refreshToken(appId, refreshToken);
            if (snapshot == null) {
                return OAuthResponseSupport.oauthErrorBody("refresh_token无效", OAuthError.TokenResponse.INVALID_GRANT, SC_BAD_REQUEST);
            }
        } else {
            return OAuthResponseSupport.oauthErrorBody("不支持的grant_type", OAuthError.TokenResponse.UNSUPPORTED_GRANT_TYPE, SC_BAD_REQUEST);
        }
        OAuthResponse oAuthResponse = OAuthASResponse.tokenResponse(HttpServletResponse.SC_OK)
                .setAccessToken(snapshot.getAccessToken())
                .setRefreshToken(snapshot.getRefreshToken())
                .setTokenType(OAuth.DEFAULT_TOKEN_TYPE.toString())
                .setExpiresIn(String.valueOf(snapshot.getAccessExpireIn()))
                .setScope(responseScope)
                .buildJSONMessage();
        return oAuthResponse.getBody();
    }

    @RequestMapping(value = "userInfo", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public HttpEntity<?> userInfo(HttpServletRequest request) throws OAuthSystemException {
        String accessToken = OAuthAccessTokenResolver.resolve(request, OAuthAccessTokenResolver.Policy.STANDARD);
        if (StringUtils.isBlank(accessToken)) {
            OAuthResponse oauthResponse = OAuthRSResponse.errorResponse(SC_UNAUTHORIZED).setRealm("Apache Oltu").setError(OAuthError.ResourceResponse.INVALID_TOKEN).buildHeaderMessage();
            HttpHeaders headers = new HttpHeaders();
            headers.add(OAuth.HeaderType.WWW_AUTHENTICATE, oauthResponse.getHeader(OAuth.HeaderType.WWW_AUTHENTICATE));
            return new ResponseEntity<>(headers, HttpStatus.UNAUTHORIZED);
        }
        OpenUserInfoSnapshot snapshot = openPlatformService.buildUserInfo(accessToken);
        if (snapshot == null) {
            OAuthResponse oauthResponse = OAuthRSResponse.errorResponse(SC_UNAUTHORIZED).setRealm("Apache Oltu").setError(OAuthError.ResourceResponse.INVALID_TOKEN).buildHeaderMessage();
            HttpHeaders headers = new HttpHeaders();
            headers.add(OAuth.HeaderType.WWW_AUTHENTICATE, oauthResponse.getHeader(OAuth.HeaderType.WWW_AUTHENTICATE));
            return new ResponseEntity<>(headers, HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(JSON.toJSONString(snapshot), HttpStatus.OK);
    }

    private String oplConsentView(HttpServletRequest request, HttpServletResponse response, Model model, OpenAppEntity app, String appId, String redirectUri, String responseType, String scope, String state, String codeChallenge, String codeChallengeMethod, TicketSnapshot ticket, boolean loggedIn, String consentError) {
        fillAuthorizeModel(model, app, appId, redirectUri, responseType, scope, state, codeChallenge, codeChallengeMethod, consentError);
        if (ticket != null) {
            scanTicketService.fillAuthorizeModel(model, ticket);
        } else {
            model.addAttribute("pollIntervalMs", scanTicketService.getScanLoginConfig().getPollIntervalMs());
        }
        model.addAttribute("authorizeLoggedIn", loggedIn);
        if (loggedIn) {
            SysUserEntity user = ShiroUtils.getUserEntity();
            if (user != null) {
                model.addAttribute("loginUserName", StringUtils.defaultIfBlank(user.getUsername(), user.getUuid()));
            }
            if (request != null) {
                model.addAttribute("authorizeLogoutUrl", buildAuthorizeLogoutUrl(request, buildAuthorizeReturnUrl(request, model)));
            }
        }
        if (StringUtils.isNotBlank(consentError)) {
            model.addAttribute("error", consentError);
        }
        try {
            String view = pageFactory.oplAuthorize(request, response, model);
            return StringUtils.isNotBlank(view) ? view : "login";
        } catch (Exception e) {
            log.warn("opl consent view failed: {}", e.getMessage());
            return "login";
        }
    }

    @RequestMapping(value = "authorize/logout", method = RequestMethod.GET)
    public ModelAndView authorizeLogout(HttpServletRequest request, HttpServletResponse response, @RequestParam(required = false) String returnTo) {
        SysLogoutSupport.logoutAndForceReauth(shiroSessionService);
        LogoutSkipSupport.mark(request, response);
        String url = normalizeRedirectUrl(returnTo);
        if (StringUtils.isBlank(url)) {
            url = WebPathUtils.forBrowser(request, OplConstants.OAUTH2_BASE + "/authorize");
        } else {
            url = WebPathUtils.safePostLoginRedirect(request, url);
        }
        return redirectUrl(url);
    }

    private String buildAuthorizeReturnUrl(HttpServletRequest request, Model model) {
        String base = WebPathUtils.forBrowser(request, OplConstants.OAUTH2_BASE + "/authorize");
        String qs = request != null ? request.getQueryString() : null;
        if (StringUtils.isNotBlank(qs) && qs.contains(OplConstants.PARAM_APP_ID + "=")) {
            return base + "?" + qs;
        }
        try {
            String responseType = StringUtils.defaultIfBlank((String) model.getAttribute("responseType"), ResponseType.CODE.toString());
            String redirectUri = (String) model.getAttribute("redirectUri");
            String scope = (String) model.getAttribute("scope");
            String state = (String) model.getAttribute("state");
            String appId = (String) model.getAttribute("appId");
            StringBuilder sb = new StringBuilder(base);
            sb.append("?response_type=").append(URLEncoder.encode(responseType, "UTF-8"));
            sb.append("&").append(OplConstants.PARAM_APP_ID).append("=").append(URLEncoder.encode(StringUtils.defaultString(appId), "UTF-8"));
            sb.append("&redirect_uri=").append(URLEncoder.encode(StringUtils.defaultString(redirectUri), "UTF-8"));
            if (StringUtils.isNotBlank(scope)) {
                sb.append("&scope=").append(URLEncoder.encode(scope, "UTF-8"));
            }
            if (StringUtils.isNotBlank(state)) {
                sb.append("&state=").append(URLEncoder.encode(state, "UTF-8"));
            }
            return sb.toString();
        } catch (UnsupportedEncodingException e) {
            return base;
        }
    }

    private String buildAuthorizeLogoutUrl(HttpServletRequest request, String returnUrl) {
        try {
            String base = WebPathUtils.forBrowser(request, OplConstants.OAUTH2_BASE + "/authorize/logout");
            return base + "?returnTo=" + URLEncoder.encode(returnUrl, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return WebPathUtils.forBrowser(request, OplConstants.OAUTH2_BASE + "/authorize/logout");
        }
    }

    private void fillAuthorizeModel(Model model, OpenAppEntity app, String appId, String redirectUri, String responseType, String scope, String state, String codeChallenge, String codeChallengeMethod, String error) {
        model.addAttribute("oplAuthorize", true);
        model.addAttribute("bodyClass", "login-page-v2 oauth-authorize-mode");
        model.addAttribute("appId", appId);
        model.addAttribute("appName", app.getName());
        AuthPageAttributes.applyConsentApp(model, StringUtils.defaultIfBlank(app.getName(), appId), authorizeAppIconSupport.resolveByAppId(appId));
        model.addAttribute("appType", app.getAppType() == null ? OpenAppType.Web.name() : app.getAppType().name());
        model.addAttribute("redirectUri", redirectUri);
        model.addAttribute("responseType", responseType);
        model.addAttribute("scope", StringUtils.defaultIfBlank(scope, app.getScope()));
        model.addAttribute("state", state);
        model.addAttribute("codeChallenge", codeChallenge);
        model.addAttribute("codeChallengeMethod", codeChallengeMethod);
        model.addAttribute("denyRedirect", OAuthRedirectSupport.buildAccessDeniedRedirect(redirectUri, state));
        if (StringUtils.isNotBlank(error)) {
            model.addAttribute("error", error);
        }
    }

    private OpenAuthorizationRequest buildAuthRequest(String appId, String redirectUri, String responseType, String scope, String state, String userUuid, boolean consented) {
        OpenAuthorizationRequest request = new OpenAuthorizationRequest();
        request.setAppId(appId);
        request.setRedirectUri(redirectUri);
        request.setResponseType(responseType);
        request.setScope(scope);
        request.setState(state);
        request.setUserUuid(userUuid);
        request.setConsented(consented);
        return request;
    }

    private Object oplLoginFallbackRedirect(HttpServletRequest request, String error) {
        try {
            ModelAndView mav = new ModelAndView();
            String url = request.getRequestURL().toString();
            String queryString = request.getQueryString();
            if (StringUtils.isNotEmpty(queryString)) {
                url = url + "?" + queryString;
            }
            mav.addObject("callback", url);
            String view = "redirect:" + OplConstants.OAUTH2_LOGIN;
            if (StringUtils.isNotBlank(error)) {
                view = view + "?error=" + URLEncoder.encode(error, "UTF-8");
            }
            mav.setViewName(view);
            return mav;
        } catch (Exception e) {
            log.warn("opl login fallback failed: {}", e.getMessage());
            return "error";
        }
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
        String home = WebPathUtils.forBrowser(request, "/");
        if (StringUtils.isNotBlank(callback)) {
            String safe = WebPathUtils.safePostLoginRedirect(request, callback);
            if (!home.equals(safe)) {
                return safe;
            }
        }
        SavedRequest savedRequest = WebUtils.getSavedRequest(request);
        if (savedRequest != null && StringUtils.isNotBlank(savedRequest.getRequestUrl())) {
            String safe = WebPathUtils.safePostLoginRedirect(request, savedRequest.getRequestUrl());
            if (!home.equals(safe)) {
                return safe;
            }
        }
        return home;
    }

    private String appendQueryParam(String url, String key, String value) throws UnsupportedEncodingException {
        if (StringUtils.isBlank(url) || StringUtils.isBlank(key)) {
            return url;
        }
        String sep = url.contains("?") ? "&" : "?";
        return url + sep + key + "=" + URLEncoder.encode(StringUtils.defaultString(value), "UTF-8");
    }
}
