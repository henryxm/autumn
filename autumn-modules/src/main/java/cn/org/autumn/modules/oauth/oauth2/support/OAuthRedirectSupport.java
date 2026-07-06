package cn.org.autumn.modules.oauth.oauth2.support;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import org.apache.commons.lang3.StringUtils;
import org.apache.oltu.oauth2.common.OAuth;

/**
 * OAuth2 授权重定向 URL 拼装（拒绝授权、授权码回调）。
 */
public final class OAuthRedirectSupport {

    private OAuthRedirectSupport() {
    }

    public static String buildAccessDeniedRedirect(String redirectUri, String state) {
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

    public static String appendCodeAndState(String redirectUri, String code, String state) {
        try {
            String encodedCode = URLEncoder.encode(StringUtils.defaultString(code), "UTF-8");
            String url = redirectUri + (redirectUri.contains("?") ? "&" : "?") + OAuth.OAUTH_CODE + "=" + encodedCode;
            if (StringUtils.isNotBlank(state)) {
                url += "&" + OAuth.OAUTH_STATE + "=" + URLEncoder.encode(state, "UTF-8");
            }
            return url;
        } catch (UnsupportedEncodingException e) {
            return redirectUri;
        }
    }

    public static String appendCodeStateAndCallback(String redirectUri, String code, String state, String callback) {
        try {
            String url = appendCodeAndState(redirectUri, code, state);
            if (StringUtils.isNotBlank(callback)) {
                url += "&callback=" + URLEncoder.encode(callback, "UTF-8");
            }
            return url;
        } catch (UnsupportedEncodingException e) {
            return redirectUri;
        }
    }
}
