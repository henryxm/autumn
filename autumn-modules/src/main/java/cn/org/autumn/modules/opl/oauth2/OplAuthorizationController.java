package cn.org.autumn.modules.opl.oauth2;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

import cn.org.autumn.modules.opl.entity.OpenAppEntity;
import cn.org.autumn.modules.opl.entity.OpenCodeEntity;
import cn.org.autumn.modules.opl.service.OpenAppService;
import cn.org.autumn.modules.opl.service.OpenCodeService;
import cn.org.autumn.modules.opl.service.OplExtensionService;
import cn.org.autumn.modules.opl.support.OplSnapshots;
import cn.org.autumn.opl.OplConstants;
import cn.org.autumn.opl.model.OpenAuthorizationRequest;
import cn.org.autumn.opl.model.OpenAppType;
import cn.org.autumn.opl.model.OpenTokenSnapshot;
import cn.org.autumn.opl.model.OpenUserInfoSnapshot;
import cn.org.autumn.opl.spi.OpenPlatformService;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.site.PageFactory;
import com.alibaba.fastjson.JSON;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.message.types.ResponseType;
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

    @RequestMapping(value = "authorize", method = {RequestMethod.GET, RequestMethod.HEAD})
    public String authorize(HttpServletRequest request, HttpServletResponse response, Model model,
                            @RequestParam(name = OplConstants.PARAM_APP_ID) String appId,
                            @RequestParam(name = OAuth.OAUTH_REDIRECT_URI) String redirectUri,
                            @RequestParam(name = OAuth.OAUTH_RESPONSE_TYPE, defaultValue = "code") String responseType,
                            @RequestParam(required = false) String scope,
                            @RequestParam(required = false) String state) {
        try {
            OpenAppEntity app = openAppService.requireActiveApp(appId);
            openAppService.validateRedirectUri(app, redirectUri);
            OpenAuthorizationRequest authRequest = buildAuthRequest(appId, redirectUri, responseType, scope, state, null, false);
            authRequest.setAppType(app.getAppType());
            oplExtensionService.beforeAuthorizePage(OplSnapshots.toAppSnapshot(app), authRequest);
            model.addAttribute("appId", appId);
            model.addAttribute("appName", app.getName());
            model.addAttribute("appType", app.getAppType() == null ? OpenAppType.Web.name() : app.getAppType().name());
            model.addAttribute("redirectUri", redirectUri);
            model.addAttribute("responseType", responseType);
            model.addAttribute("scope", StringUtils.defaultIfBlank(scope, app.getScope()));
            model.addAttribute("state", state);
            model.addAttribute("denyRedirect", buildAccessDeniedRedirect(redirectUri, state));
            boolean loggedIn = !ShiroUtils.needLogin();
            model.addAttribute("authorizeLoggedIn", loggedIn);
            if (loggedIn) {
                SysUserEntity user = ShiroUtils.getUserEntity();
                if (user != null) {
                    model.addAttribute("loginUserName", StringUtils.defaultIfBlank(user.getUsername(), user.getUuid()));
                }
            }
            if (!loggedIn) {
                return pageFactory.login(request, response, model);
            }
            return "opl/authorize";
        } catch (Exception e) {
            log.warn("opl authorize failed: {}", e.getMessage());
            try {
                return writeOAuthError(response, e.getMessage(), OAuthError.OAUTH_ERROR, SC_BAD_REQUEST);
            } catch (OAuthSystemException ex) {
                return "error";
            }
        }
    }

    @RequestMapping(value = "authorize/approve", method = RequestMethod.POST)
    public Object approve(HttpServletRequest request,
                          @RequestParam(name = OplConstants.PARAM_APP_ID) String appId,
                          @RequestParam(name = OAuth.OAUTH_REDIRECT_URI) String redirectUri,
                          @RequestParam(name = OAuth.OAUTH_RESPONSE_TYPE, defaultValue = "code") String responseType,
                          @RequestParam(required = false) String scope,
                          @RequestParam(required = false) String state,
                          @RequestParam(name = "userConsent", defaultValue = "false") String userConsent) throws Exception {
        if (ShiroUtils.needLogin()) {
            throw new IllegalStateException("请先登录后再确认授权");
        }
        if (!consented(userConsent)) {
            throw new IllegalStateException("请勾选授权协议后再确认");
        }
        OpenAppEntity app = openAppService.requireActiveApp(appId);
        openAppService.validateRedirectUri(app, redirectUri);
        if (!ResponseType.CODE.toString().equalsIgnoreCase(responseType)) {
            throw new IllegalArgumentException("仅支持authorization_code");
        }
        SysUserEntity user = ShiroUtils.getUserEntity();
        OpenAuthorizationRequest authRequest = buildAuthRequest(appId, redirectUri, responseType, scope, state, user.getUuid(), true);
        authRequest.setAppType(app.getAppType());
        oplExtensionService.beforeApprove(OplSnapshots.toAppSnapshot(app), authRequest);
        OpenCodeEntity codeEntity = openCodeService.issue(appId, user.getUuid(), redirectUri);
        oplExtensionService.afterCodeIssued(OplSnapshots.toAppSnapshot(app), authRequest, codeEntity.getCode());
        String url = redirectUri + (redirectUri.contains("?") ? "&" : "?") + OAuth.OAUTH_CODE + "=" + codeEntity.getCode();
        if (StringUtils.isNotBlank(state)) {
            url += "&" + OAuth.OAUTH_STATE + "=" + URLEncoder.encode(state, "UTF-8");
        }
        RedirectView view = new RedirectView(url, false);
        return new ModelAndView(view);
    }

    @RequestMapping(value = "token", method = RequestMethod.POST)
    @ResponseBody
    public String token(HttpServletRequest request,
                        @RequestParam(name = OplConstants.PARAM_APP_ID) String appId,
                        @RequestParam(name = OplConstants.PARAM_APP_SECRET) String appSecret,
                        @RequestParam(name = OAuth.OAUTH_GRANT_TYPE) String grantType,
                        @RequestParam(required = false, name = OAuth.OAUTH_CODE) String code,
                        @RequestParam(required = false, name = OAuth.OAUTH_REDIRECT_URI) String redirectUri,
                        @RequestParam(required = false, name = OAuth.OAUTH_REFRESH_TOKEN) String refreshToken) throws OAuthSystemException {
        if (!openPlatformService.validateAppSecret(appId, appSecret)) {
            return oauthErrorBody("appSecret认证失败", OAuthError.TokenResponse.UNAUTHORIZED_CLIENT, SC_UNAUTHORIZED);
        }
        OpenTokenSnapshot snapshot;
        if (GrantType.AUTHORIZATION_CODE.toString().equalsIgnoreCase(grantType)) {
            try {
                snapshot = openPlatformService.issueTokenFromCode(appId, code, redirectUri);
            } catch (IllegalArgumentException e) {
                return oauthErrorBody(e.getMessage(), OAuthError.TokenResponse.INVALID_GRANT, SC_BAD_REQUEST);
            }
            if (snapshot == null) {
                return oauthErrorBody("授权码无效", OAuthError.TokenResponse.INVALID_GRANT, SC_BAD_REQUEST);
            }
        } else if (GrantType.REFRESH_TOKEN.toString().equalsIgnoreCase(grantType)) {
            snapshot = openPlatformService.refreshToken(appId, refreshToken);
            if (snapshot == null) {
                return oauthErrorBody("refresh_token无效", OAuthError.TokenResponse.INVALID_GRANT, SC_BAD_REQUEST);
            }
        } else {
            return oauthErrorBody("不支持的grant_type", OAuthError.TokenResponse.UNSUPPORTED_GRANT_TYPE, SC_BAD_REQUEST);
        }
        OAuthResponse response = OAuthASResponse.tokenResponse(HttpServletResponse.SC_OK)
                .setAccessToken(snapshot.getAccessToken())
                .setRefreshToken(snapshot.getRefreshToken())
                .setTokenType(OAuth.DEFAULT_TOKEN_TYPE.toString())
                .setExpiresIn(String.valueOf(snapshot.getAccessExpireIn()))
                .setScope(OplConstants.DEFAULT_SCOPE)
                .buildJSONMessage();
        return response.getBody();
    }

    @RequestMapping(value = "userInfo", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public HttpEntity<?> userInfo(HttpServletRequest request) {
        String accessToken = resolveAccessToken(request);
        OpenUserInfoSnapshot snapshot = openPlatformService.buildUserInfo(accessToken);
        if (snapshot == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(JSON.toJSONString(snapshot), HttpStatus.OK);
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

    private String resolveAccessToken(HttpServletRequest request) {
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.isNotBlank(auth) && auth.toLowerCase().startsWith("bearer ")) {
            return auth.substring(7).trim();
        }
        return request.getParameter(OAuth.OAUTH_ACCESS_TOKEN);
    }

    private boolean consented(String userConsent) {
        return "true".equalsIgnoreCase(userConsent) || "1".equals(userConsent) || "on".equalsIgnoreCase(userConsent);
    }

    private String buildAccessDeniedRedirect(String redirectUri, String state) {
        try {
            String url = redirectUri + (redirectUri.contains("?") ? "&" : "?") + "error=access_denied";
            if (StringUtils.isNotBlank(state)) {
                url += "&" + OAuth.OAUTH_STATE + "=" + URLEncoder.encode(state, "UTF-8");
            }
            return url;
        } catch (UnsupportedEncodingException e) {
            return redirectUri;
        }
    }

    private String oauthErrorBody(String description, String error, int status) throws OAuthSystemException {
        OAuthResponse response = OAuthASResponse.errorResponse(status).setError(error).setErrorDescription(description).buildJSONMessage();
        return response.getBody();
    }

    private String writeOAuthError(HttpServletResponse response, String description, String error, int status) throws OAuthSystemException {
        OAuthResponse oAuthResponse = OAuthASResponse.errorResponse(status).setError(error).setErrorDescription(description).buildJSONMessage();
        if (response != null && !response.isCommitted()) {
            response.setStatus(oAuthResponse.getResponseStatus());
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json;charset=UTF-8");
            try {
                response.getWriter().write(oAuthResponse.getBody());
                response.getWriter().flush();
            } catch (Exception e) {
                throw new OAuthSystemException(e);
            }
        }
        return oAuthResponse.getBody();
    }
}
