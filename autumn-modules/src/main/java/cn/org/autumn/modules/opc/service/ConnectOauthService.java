package cn.org.autumn.modules.opc.service;

import cn.org.autumn.modules.opc.dto.OpcTokenResult;
import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import cn.org.autumn.opc.OpcConstants;
import cn.org.autumn.opl.OplConstants;
import cn.org.autumn.opl.model.OpenUserInfoSnapshot;
import cn.org.autumn.utils.HttpClientUtils;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** 远程 OPL OAuth HTTP 客户端。 */
@Service
public class ConnectOauthService {

    @Autowired
    private ConnectAppService connectAppService;

    public String buildAuthorizeUrl(ConnectAppEntity app, String state) {
        connectAppService.fillDefaultUris(app);
        if (StringUtils.isBlank(state)) {
            state = UUID.randomUUID().toString().replace("-", "");
        }
        try {
            StringBuilder sb = new StringBuilder(app.getAuthorizeUri());
            sb.append("?").append(OplConstants.PARAM_APP_ID).append("=").append(URLEncoder.encode(app.getAppId(), "UTF-8"));
            sb.append("&").append(OAuth.OAUTH_REDIRECT_URI).append("=").append(URLEncoder.encode(app.getRedirectUri(), "UTF-8"));
            sb.append("&").append(OAuth.OAUTH_RESPONSE_TYPE).append("=code");
            sb.append("&").append(OAuth.OAUTH_SCOPE).append("=").append(URLEncoder.encode(StringUtils.defaultIfBlank(app.getScope(), OplConstants.DEFAULT_SCOPE), "UTF-8"));
            sb.append("&").append(OAuth.OAUTH_STATE).append("=").append(URLEncoder.encode(state, "UTF-8"));
            return sb.toString();
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("构建授权URL失败", e);
        }
    }

    public OpcTokenResult exchangeCode(ConnectAppEntity app, String code) {
        connectAppService.fillDefaultUris(app);
        Map<String, String> params = new HashMap<>();
        params.put(OplConstants.PARAM_APP_ID, app.getAppId());
        params.put(OplConstants.PARAM_APP_SECRET, connectAppService.requirePlainSecret(app));
        params.put(OAuth.OAUTH_GRANT_TYPE, GrantType.AUTHORIZATION_CODE.toString());
        params.put(OAuth.OAUTH_CODE, code);
        params.put(OAuth.OAUTH_REDIRECT_URI, app.getRedirectUri());
        String body = HttpClientUtils.doPost(app.getTokenUri(), params);
        if (StringUtils.isBlank(body)) {
            throw new IllegalStateException("换token失败");
        }
        JSONObject json = JSON.parseObject(body);
        if (json == null || StringUtils.isBlank(json.getString(OAuth.OAUTH_ACCESS_TOKEN))) {
            throw new IllegalStateException(json == null ? "换token失败" : json.getString("error_description"));
        }
        OpcTokenResult result = new OpcTokenResult();
        result.setAccessToken(json.getString(OAuth.OAUTH_ACCESS_TOKEN));
        result.setRefreshToken(json.getString(OAuth.OAUTH_REFRESH_TOKEN));
        result.setTokenType(json.getString(OAuth.OAUTH_TOKEN_TYPE));
        result.setExpiresIn(json.getLongValue("expires_in"));
        return result;
    }

    public OpenUserInfoSnapshot fetchUserInfo(ConnectAppEntity app, String accessToken) {
        connectAppService.fillDefaultUris(app);
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + accessToken);
        String body = HttpClientUtils.doGet(app.getUserInfoUri(), null, headers);
        if (StringUtils.isBlank(body)) {
            throw new IllegalStateException("获取userInfo失败");
        }
        return JSON.parseObject(body, OpenUserInfoSnapshot.class);
    }
}
