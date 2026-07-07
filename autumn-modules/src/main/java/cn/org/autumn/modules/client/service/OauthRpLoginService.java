package cn.org.autumn.modules.client.service;

import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.client.oauth2.WebOauthEndpointResolver;
import cn.org.autumn.modules.oauth.oauth2.support.OAuth2HttpClient;
import cn.org.autumn.utils.WebPathUtils;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** RP 联邦 OAuth 授权跳转与 B1 callback 换票入口（委托 {@link WebOauthLoginService#completeRemoteOAuthCallback}）。 */
@Service
public class OauthRpLoginService {

    @Autowired
    AuthSiteRoleService authSiteRoleService;

    @Autowired
    OauthRpStateService oauthRpStateService;

    @Autowired
    OAuth2HttpClient oauth2HttpClient;

    @Autowired
    WebOauthLoginService webOauthLoginService;

    @Autowired
    WebOauthEndpointResolver webOauthEndpointResolver;

    public String buildAuthorizeRedirect(HttpServletRequest request, String callback) {
        WebAuthenticationEntity rpClient = authSiteRoleService.resolveRpClient(request);
        if (rpClient == null) {
            throw new IllegalStateException("未配置 RP OAuth 客户端");
        }
        String state = oauthRpStateService.issueState(WebPathUtils.safeOauthCallbackForClient(request, callback));
        String authorizeUri = webOauthEndpointResolver.resolveAuthorizeUri(rpClient);
        if (StringUtils.isBlank(authorizeUri)) {
            throw new IllegalStateException("未配置授权地址（originUri 或 authorizeUri）");
        }
        return oauth2HttpClient.buildAuthorizeUrl(authorizeUri, OAuth2HttpClient.CredentialParam.OAUTH, rpClient.getClientId(), rpClient.getRedirectUri(), rpClient.getScope(), state);
    }

    public String handleCallback(HttpServletRequest request, String code, String state, String error, String errorDescription) {
        if (StringUtils.isNotBlank(error)) {
            throw new IllegalStateException(StringUtils.defaultIfBlank(errorDescription, error));
        }
        if (StringUtils.isBlank(code)) {
            throw new IllegalArgumentException("未收到授权码");
        }
        String expectedCallback = oauthRpStateService.consumeState(state);
        if (expectedCallback == null && StringUtils.isNotBlank(state)) {
            throw new IllegalStateException("state 无效或已过期");
        }
        WebAuthenticationEntity rpClient = authSiteRoleService.resolveRpClient(request);
        if (rpClient == null) {
            throw new IllegalStateException("未配置 RP OAuth 客户端");
        }
        webOauthLoginService.completeRemoteOAuthCallback(request, rpClient, code);
        String callback = expectedCallback;
        if (StringUtils.isBlank(callback)) {
            callback = WebPathUtils.safeOauthCallbackForClient(request, request.getParameter("callback"));
        }
        if (StringUtils.isBlank(callback)) {
            callback = WebPathUtils.forBrowser(request, "/");
        }
        return callback;
    }
}
