package cn.org.autumn.modules.client.oauth2;

import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.client.service.WebAuthenticationService;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.usr.dto.UserProfile;
import cn.org.autumn.modules.usr.service.UserProfileService;
import cn.org.autumn.modules.usr.service.UserTokenService;
import cn.org.autumn.utils.HttpClientUtils;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang.StringUtils;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthBearerClientRequest;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthResourceResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("client")
public class ClientOauth2Controller {
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    WebAuthenticationService webAuthenticationService;

    @Autowired
    UserProfileService userProfileService;

    @Autowired
    SysConfigService sysConfigService;

    @Autowired
    UserTokenService userTokenService;

    @RequestMapping("oauth2/callback")
    public Object defaultCodeCallback(HttpServletRequest request) throws OAuthSystemException {
        String authCode = request.getParameter(OAuth.OAUTH_CODE);
        if (StringUtils.isEmpty(authCode)) {
            OAuthResponse response =
                    OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                            .setError(OAuthError.OAUTH_ERROR)
                            .setErrorDescription("Code should not be empty.")
                            .buildJSONMessage();
            return new ResponseEntity(response.getBody(), HttpStatus.valueOf(response.getResponseStatus()));
        }

        WebAuthenticationEntity webAuthenticationEntity = webAuthenticationService.findByClientId(sysConfigService.getOauth2LoginClientId());
        String accessToken = getAccessToken(webAuthenticationEntity, authCode);
        UserProfile userProfile = getUserInfo(webAuthenticationEntity, accessToken);
        if (null != userProfile) {
            userProfileService.login(userProfile);
            userTokenService.saveToken(accessToken);
            logger.debug(userProfile.toString());
        }
        return "redirect:/";
    }

    private String getAccessToken(WebAuthenticationEntity webAuthClientEntity, String oauthCode) {
        String clientSecret = webAuthClientEntity.getClientSecret();
        String redirectUrl = webAuthClientEntity.getRedirectUri();
        Map<String, String> paramMap = new HashMap<>();
        String state = webAuthClientEntity.getState();
        String scope = webAuthClientEntity.getScope();
        if (null == state)
            state = "state";
        if (StringUtils.isEmpty(scope))
            scope = "all";
        paramMap.put(OAuth.OAUTH_STATE, state);
        paramMap.put(OAuth.OAUTH_SCOPE, scope);
        paramMap.put(OAuth.OAUTH_REDIRECT_URI, webAuthClientEntity.getRedirectUri());
        paramMap.put(OAuth.OAUTH_GRANT_TYPE, String.valueOf(GrantType.AUTHORIZATION_CODE));
        paramMap.put(OAuth.OAUTH_CLIENT_ID, webAuthClientEntity.getClientId());
        paramMap.put(OAuth.OAUTH_CODE, oauthCode);
        paramMap.put(OAuth.OAUTH_CLIENT_SECRET, clientSecret);
        String accessToken = HttpClientUtils.doPost(webAuthClientEntity.getAccessTokenUri(), paramMap);
        logger.debug(accessToken);
        return accessToken;
    }

    public UserProfile getUserInfo(WebAuthenticationEntity webAuthClientEntity, String accessToken) {
        String userInfo = webAuthClientEntity.getUserInfoUri();
        OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
        OAuthClientRequest authUserRequest;
        try {
            authUserRequest = new OAuthBearerClientRequest(userInfo).setAccessToken(accessToken).buildQueryMessage();
            OAuthResourceResponse resourceResponse = oAuthClient.resource(authUserRequest, OAuth.HttpMethod.GET, OAuthResourceResponse.class);
            String userinfo = resourceResponse.getBody();
            UserProfile userProfile = JSON.parseObject(userinfo, UserProfile.class);
            return userProfile;
        } catch (Exception e) {
            logger.error("getUserInfo error：" + e.getMessage());
        }
        return null;
    }

}
