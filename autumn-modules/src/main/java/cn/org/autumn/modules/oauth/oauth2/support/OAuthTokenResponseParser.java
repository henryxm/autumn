package cn.org.autumn.modules.oauth.oauth2.support;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.oltu.oauth2.common.OAuth;

/**
 * 解析 OAuth2 token 端点 JSON 响应体。
 */
public final class OAuthTokenResponseParser {

    private OAuthTokenResponseParser() {
    }

    public static boolean isJsonWrappedToken(String raw) {
        if (StringUtils.isBlank(raw)) {
            return false;
        }
        String trimmed = raw.trim();
        return trimmed.startsWith("{") && trimmed.contains("\"access_token\"");
    }

    public static String extractAccessTokenKey(String raw) {
        if (StringUtils.isBlank(raw)) {
            return "";
        }
        String trimmed = raw.trim();
        if (isJsonWrappedToken(trimmed)) {
            JSONObject json = JSON.parseObject(trimmed);
            if (json != null && StringUtils.isNotBlank(json.getString(OAuth.OAUTH_ACCESS_TOKEN))) {
                return json.getString(OAuth.OAUTH_ACCESS_TOKEN);
            }
            return "";
        }
        if (trimmed.contains("\"") || trimmed.contains("{") || trimmed.contains("}") || trimmed.length() > 100) {
            return "";
        }
        return trimmed;
    }

    public static OAuthTokenResponse parse(String body) {
        OAuthTokenResponse result = new OAuthTokenResponse();
        result.setRawBody(body);
        if (StringUtils.isBlank(body)) {
            return result;
        }
        if (!isJsonWrappedToken(body)) {
            String accessToken = extractAccessTokenKey(body);
            if (StringUtils.isNotBlank(accessToken)) {
                result.setAccessToken(accessToken);
            }
            return result;
        }
        JSONObject json = JSON.parseObject(body.trim());
        if (json == null) {
            return result;
        }
        result.setAccessToken(json.getString(OAuth.OAUTH_ACCESS_TOKEN));
        result.setRefreshToken(json.getString(OAuth.OAUTH_REFRESH_TOKEN));
        result.setTokenType(json.getString(OAuth.OAUTH_TOKEN_TYPE));
        result.setExpiresIn(json.getLongValue("expires_in"));
        return result;
    }
}
