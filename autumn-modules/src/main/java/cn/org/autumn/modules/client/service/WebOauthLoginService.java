package cn.org.autumn.modules.client.service;

import cn.org.autumn.modules.client.dto.WebOauthBindPendingContext;
import cn.org.autumn.modules.client.dto.WebOauthBindResolveResult;
import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.client.oauth2.WebOauthBindException;
import cn.org.autumn.modules.client.oauth2.WebOauthBindException.ConflictType;
import cn.org.autumn.modules.client.oauth2.WebOauthBindSupport;
import cn.org.autumn.modules.client.oauth2.WebOauthEndpointResolver;
import cn.org.autumn.utils.WebPathUtils;
import cn.org.autumn.modules.oauth.oauth2.support.OAuth2HttpClient;
import cn.org.autumn.modules.oauth.oauth2.support.OAuthTokenResponse;
import cn.org.autumn.modules.oauth.oauth2.support.OAuthTokenResponseParser;
import cn.org.autumn.modules.oauth.service.ClientDetailsService;
import cn.org.autumn.modules.oauth.store.TokenStore;
import cn.org.autumn.modules.oauth.store.ValueType;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.usr.dto.UserProfile;
import cn.org.autumn.modules.usr.entity.UserProfileEntity;
import cn.org.autumn.modules.usr.service.UserProfileService;
import cn.org.autumn.modules.usr.service.UserTokenService;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.oltu.oauth2.as.issuer.MD5Generator;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuerImpl;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 经典 OAuth2 RP 回调编排：换票、拉 userInfo、绑定、establishSession。{@link #completeOAuthCallback} 与 {@link #completeRemoteOAuthCallback} 共用同一套 finish 逻辑。 */
@Service
public class WebOauthLoginService {

    @Autowired
    private OAuth2HttpClient oauth2HttpClient;

    @Autowired
    private WebOauthBindService webOauthBindService;

    @Autowired
    private WebOauthBindSupport webOauthBindSupport;

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private UserTokenService userTokenService;

    @Autowired
    private ClientDetailsService clientDetailsService;

    @Autowired
    private WebOauthBindPendingService webOauthBindPendingService;

    @Autowired
    private WebAuthenticationService webAuthenticationService;

    @Autowired
    private WebOauthEndpointResolver webOauthEndpointResolver;

    public String bindChoicePageUrl(HttpServletRequest request, String pendingToken) {
        return WebPathUtils.forBrowser(request, "/client/oauth2/bind/choice?token=" + pendingToken);
    }

    @Transactional(rollbackFor = Exception.class)
    public WebOauthBindResolveResult completePendingCreateNew(HttpServletRequest request, String pendingToken) {
        return finishPendingBind(request, pendingToken, true);
    }

    @Transactional(rollbackFor = Exception.class)
    public WebOauthBindResolveResult completePendingBindSession(HttpServletRequest request, String pendingToken) {
        return finishPendingBind(request, pendingToken, false);
    }

    @Transactional(rollbackFor = Exception.class)
    public WebOauthBindResolveResult completeOAuthCallback(HttpServletRequest request, WebAuthenticationEntity webAuth, String code) {
        return finishOAuthLogin(request, webAuth, code, false);
    }

    @Transactional(rollbackFor = Exception.class)
    public WebOauthBindResolveResult completeRemoteOAuthCallback(HttpServletRequest request, WebAuthenticationEntity webAuth, String code) {
        return finishOAuthLogin(request, webAuth, code, true);
    }

    private WebOauthBindResolveResult finishOAuthLogin(HttpServletRequest request, WebAuthenticationEntity webAuth, String code, boolean remoteIdp) {
        if (webAuth == null) {
            throw new IllegalArgumentException("OAuth 客户端配置不存在");
        }
        if (StringUtils.isBlank(code)) {
            throw new IllegalArgumentException("授权码不能为空");
        }
        String tokenBody = exchangeAuthorizationCode(webAuth, code);
        OAuth2HttpClient.UserInfoDelivery delivery = webOauthBindSupport.resolveUserInfoDelivery(webAuth, request);
        if (remoteIdp && StringUtils.isBlank(webAuth.getUserInfoDelivery())) {
            delivery = OAuth2HttpClient.UserInfoDelivery.BEARER;
        }
        UserProfile upstream = fetchUserInfo(webAuth, tokenBody, delivery, remoteIdp);
        if (upstream == null || StringUtils.isBlank(upstream.getUuid())) {
            throw WebOauthBindException.invalidUpstream(webAuth);
        }
        String callback = WebPathUtils.safeOauthCallbackForClient(request, request.getParameter("callback"));
        WebOauthBindResolveResult result;
        try {
            result = webOauthBindService.resolveAndBind(webAuth, upstream, request);
        } catch (WebOauthBindException e) {
            if (e.getConflictType() == ConflictType.BIND_CHOICE_REQUIRED) {
                String token = webOauthBindPendingService.save(webAuth, upstream, tokenBody, callback);
                throw WebOauthBindException.bindChoiceRequired(webAuth, upstream.getUuid(), token);
            }
            throw e;
        }
        userProfileService.establishSession(result.getProfile());
        userTokenService.saveToken(tokenBody);
        return result;
    }

    private WebOauthBindResolveResult finishPendingBind(HttpServletRequest request, String pendingToken, boolean createNewUser) {
        WebOauthBindPendingContext pending = webOauthBindPendingService.consume(pendingToken);
        if (pending == null || StringUtils.isBlank(pending.getWebAuthUuid()) || StringUtils.isBlank(pending.getUpstreamJson())) {
            throw new IllegalStateException("绑定会话已过期，请重新发起授权");
        }
        WebAuthenticationEntity webAuth = webAuthenticationService.getByUuid(pending.getWebAuthUuid());
        if (webAuth == null) {
            throw new IllegalStateException("OAuth 客户端配置不存在");
        }
        UserProfile upstream = JSON.parseObject(pending.getUpstreamJson(), UserProfile.class);
        if (upstream == null || StringUtils.isBlank(upstream.getUuid())) {
            throw WebOauthBindException.invalidUpstream(webAuth);
        }
        WebOauthBindResolveResult result = createNewUser ? webOauthBindService.bindCreateNewUser(webAuth, upstream) : webOauthBindService.bindSessionUser(webAuth, upstream);
        userProfileService.establishSession(result.getProfile());
        if (StringUtils.isNotBlank(pending.getTokenBody())) {
            userTokenService.saveToken(pending.getTokenBody());
        }
        return result;
    }

    private UserProfile fetchUserInfo(WebAuthenticationEntity webAuth, String tokenBody, OAuth2HttpClient.UserInfoDelivery delivery, boolean remoteIdp) {
        try {
            String accessToken = extractAccessToken(tokenBody);
            if (StringUtils.isNotBlank(accessToken) && clientDetailsService.isValidAccessToken(accessToken)) {
                return fetchUserProfileLocally(accessToken);
            }
            String tokenForUserInfo = tokenBody;
            if (delivery == OAuth2HttpClient.UserInfoDelivery.BEARER) {
                tokenForUserInfo = accessToken;
                if (StringUtils.isBlank(tokenForUserInfo)) {
                    return null;
                }
            }
            String userInfoUri = webOauthEndpointResolver.resolveUserInfoUri(webAuth, remoteIdp);
            if (StringUtils.isBlank(userInfoUri)) {
                throw new IllegalArgumentException("未配置 userInfoUri");
            }
            String userinfo = oauth2HttpClient.fetchUserInfoBody(userInfoUri, tokenForUserInfo, delivery);
            return JSON.parseObject(userinfo, UserProfile.class);
        } catch (Exception e) {
            return null;
        }
    }

    private String exchangeAuthorizationCode(WebAuthenticationEntity webAuth, String code) {
        if (canExchangeLocally(code, webAuth)) {
            return exchangeAuthorizationCodeLocally(code);
        }
        String tokenUri = webOauthEndpointResolver.resolveAccessTokenUri(webAuth);
        if (StringUtils.isBlank(tokenUri)) {
            throw new IllegalArgumentException("未配置 accessTokenUri");
        }
        String body = oauth2HttpClient.exchangeAuthorizationCodeRaw(OAuth2HttpClient.CredentialParam.OAUTH, tokenUri, webAuth.getClientId(), webAuth.getClientSecret(), code, webAuth.getRedirectUri());
        OAuthTokenResponse parsed = OAuthTokenResponseParser.parse(body);
        if (StringUtils.isBlank(parsed.getAccessToken())) {
            throw new IllegalStateException(StringUtils.isNotBlank(body) ? body : "换取 access_token 失败");
        }
        return body;
    }

    private boolean canExchangeLocally(String code, WebAuthenticationEntity webAuth) {
        if (!clientDetailsService.isValidCode(code)) {
            return false;
        }
        if (!clientDetailsService.isValidClientSecret(webAuth.getClientSecret())) {
            return false;
        }
        return clientDetailsService.findByClientId(webAuth.getClientId()) != null;
    }

    private String exchangeAuthorizationCodeLocally(String code) {
        try {
            TokenStore tokenStore = clientDetailsService.get(ValueType.authCode, code);
            if (tokenStore == null) {
                throw new IllegalStateException("授权码无效");
            }
            OAuthIssuerImpl issuer = new OAuthIssuerImpl(new MD5Generator());
            String accessToken = issuer.accessToken();
            String refreshToken = issuer.refreshToken();
            clientDetailsService.putToken(accessToken, refreshToken, tokenStore);
            TokenStore store = clientDetailsService.get(ValueType.accessToken, accessToken);
            long expiresIn = store == null ? 3600L : store.getExpireIn();
            JSONObject json = new JSONObject();
            json.put(OAuth.OAUTH_ACCESS_TOKEN, accessToken);
            json.put(OAuth.OAUTH_REFRESH_TOKEN, refreshToken);
            json.put(OAuth.OAUTH_TOKEN_TYPE, "bearer");
            json.put("expires_in", expiresIn);
            json.put(OAuth.OAUTH_SCOPE, "basic");
            return json.toJSONString();
        } catch (OAuthSystemException e) {
            throw new IllegalStateException("本地换票失败: " + e.getMessage(), e);
        }
    }

    private UserProfile fetchUserProfileLocally(String accessToken) {
        TokenStore tokenStore = clientDetailsService.get(ValueType.accessToken, accessToken);
        if (tokenStore == null || !(tokenStore.getValue() instanceof SysUserEntity)) {
            throw new IllegalStateException("access_token 无效");
        }
        UserProfileEntity profileEntity = userProfileService.from((SysUserEntity) tokenStore.getValue());
        return UserProfile.from(profileEntity);
    }

    private String extractAccessToken(String tokenResponseBody) {
        if (StringUtils.isBlank(tokenResponseBody)) {
            return null;
        }
        if (tokenResponseBody.trim().startsWith("{")) {
            JSONObject json = JSON.parseObject(tokenResponseBody);
            return json == null ? null : json.getString(OAuth.OAUTH_ACCESS_TOKEN);
        }
        return tokenResponseBody.trim();
    }
}
