package cn.org.autumn.modules.oauth.oauth2.support;

import cn.org.autumn.opl.OplConstants;
import cn.org.autumn.utils.HttpClientUtils;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthBearerClientRequest;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthResourceResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.springframework.stereotype.Component;

/**
 * OAuth2 RP 侧 HTTP 客户端：换票与拉 userInfo（OAuth / OPL 参数名分支）。
 */
@Component
public class OAuth2HttpClient {

    public enum CredentialParam {
        OAUTH,
        OPL
    }

    public enum UserInfoDelivery {
        /** Autumn /oauth2/userInfo 历史契约：query 传 JSON 包裹 token */
        LEGACY,
        /** 标准 Bearer Header */
        BEARER
    }

    public String buildAuthorizeUrl(String authorizeUri, CredentialParam credentialParam, String appOrClientId, String redirectUri, String scope, String state) {
        try {
            String idParam = credentialParam == CredentialParam.OPL ? OplConstants.PARAM_APP_ID : OAuth.OAUTH_CLIENT_ID;
            StringBuilder sb = new StringBuilder(authorizeUri);
            sb.append("?").append(idParam).append("=").append(URLEncoder.encode(appOrClientId, "UTF-8"));
            sb.append("&").append(OAuth.OAUTH_REDIRECT_URI).append("=").append(URLEncoder.encode(redirectUri, "UTF-8"));
            sb.append("&").append(OAuth.OAUTH_RESPONSE_TYPE).append("=code");
            sb.append("&").append(OAuth.OAUTH_SCOPE).append("=").append(URLEncoder.encode(StringUtils.defaultIfBlank(scope, OplConstants.DEFAULT_SCOPE), "UTF-8"));
            if (StringUtils.isNotBlank(state)) {
                sb.append("&").append(OAuth.OAUTH_STATE).append("=").append(URLEncoder.encode(state, "UTF-8"));
            }
            return sb.toString();
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("构建授权URL失败", e);
        }
    }

    public String exchangeAuthorizationCodeRaw(CredentialParam credentialParam, String tokenUri, String appOrClientId, String secret, String code, String redirectUri) {
        Map<String, String> params = new HashMap<>();
        if (credentialParam == CredentialParam.OPL) {
            params.put(OplConstants.PARAM_APP_ID, appOrClientId);
            params.put(OplConstants.PARAM_APP_SECRET, secret);
        } else {
            params.put(OAuth.OAUTH_CLIENT_ID, appOrClientId);
            params.put(OAuth.OAUTH_CLIENT_SECRET, secret);
        }
        params.put(OAuth.OAUTH_GRANT_TYPE, GrantType.AUTHORIZATION_CODE.toString());
        params.put(OAuth.OAUTH_CODE, code);
        params.put(OAuth.OAUTH_REDIRECT_URI, redirectUri);
        return HttpClientUtils.doPost(tokenUri, params);
    }

    public OAuthTokenResponse exchangeAuthorizationCode(CredentialParam credentialParam, String tokenUri, String appOrClientId, String secret, String code, String redirectUri) {
        String body = exchangeAuthorizationCodeRaw(credentialParam, tokenUri, appOrClientId, secret, code, redirectUri);
        OAuthTokenResponse parsed = OAuthTokenResponseParser.parse(body);
        if (StringUtils.isBlank(parsed.getAccessToken())) {
            throw new IllegalStateException(StringUtils.isNotBlank(body) ? body : "换token失败");
        }
        return parsed;
    }

    public String fetchUserInfoBody(String userInfoUri, String tokenOrJsonBody, UserInfoDelivery delivery) {
        if (delivery == UserInfoDelivery.LEGACY) {
            return fetchUserInfoLegacy(userInfoUri, tokenOrJsonBody);
        }
        return fetchUserInfoBearer(userInfoUri, tokenOrJsonBody);
    }

    private String fetchUserInfoLegacy(String userInfoUri, String tokenOrJsonBody) {
        OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
        try {
            OAuthClientRequest authUserRequest = new OAuthBearerClientRequest(userInfoUri).setAccessToken(tokenOrJsonBody).buildQueryMessage();
            OAuthResourceResponse resourceResponse = oAuthClient.resource(authUserRequest, OAuth.HttpMethod.GET, OAuthResourceResponse.class);
            return resourceResponse.getBody();
        } catch (Exception e) {
            throw new IllegalStateException("获取userInfo失败: " + e.getMessage(), e);
        }
    }

    private String fetchUserInfoBearer(String userInfoUri, String accessToken) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + accessToken);
        String body = HttpClientUtils.doGet(userInfoUri, null, headers);
        if (StringUtils.isBlank(body)) {
            throw new IllegalStateException("获取userInfo失败");
        }
        return body;
    }
}
