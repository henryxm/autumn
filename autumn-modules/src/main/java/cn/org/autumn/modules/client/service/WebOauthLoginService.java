package cn.org.autumn.modules.client.service;

import cn.org.autumn.modules.client.dto.WebOauthBindResolveResult;
import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.client.oauth2.WebOauthBindException;
import cn.org.autumn.modules.client.oauth2.WebOauthBindSupport;
import cn.org.autumn.modules.oauth.oauth2.support.OAuth2HttpClient;
import cn.org.autumn.modules.oauth.oauth2.support.OAuthTokenResponseParser;
import cn.org.autumn.modules.usr.dto.UserProfile;
import cn.org.autumn.modules.usr.service.UserProfileService;
import cn.org.autumn.modules.usr.service.UserTokenService;
import com.alibaba.fastjson2.JSON;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 经典 OAuth2 RP 回调编排：换票、拉 userInfo、绑定、登录。 */
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

    @Transactional(rollbackFor = Exception.class)
    public WebOauthBindResolveResult completeOAuthCallback(HttpServletRequest request, WebAuthenticationEntity webAuth, String code) {
        if (webAuth == null) {
            throw new IllegalArgumentException("OAuth 客户端配置不存在");
        }
        if (StringUtils.isBlank(code)) {
            throw new IllegalArgumentException("授权码不能为空");
        }
        String tokenBody = oauth2HttpClient.exchangeAuthorizationCodeRaw(OAuth2HttpClient.CredentialParam.OAUTH, webAuth.getAccessTokenUri(), webAuth.getClientId(), webAuth.getClientSecret(), code, webAuth.getRedirectUri());
        OAuth2HttpClient.UserInfoDelivery delivery = webOauthBindSupport.resolveUserInfoDelivery(webAuth, request);
        UserProfile upstream = fetchUserInfo(webAuth, tokenBody, delivery);
        if (upstream == null || StringUtils.isBlank(upstream.getUuid())) {
            throw WebOauthBindException.invalidUpstream(webAuth);
        }
        WebOauthBindResolveResult result = webOauthBindService.resolveAndBind(webAuth, upstream, request);
        userProfileService.establishSession(result.getProfile());
        userTokenService.saveToken(tokenBody);
        return result;
    }

    public UserProfile fetchUserInfo(WebAuthenticationEntity webAuth, String tokenBody, OAuth2HttpClient.UserInfoDelivery delivery) {
        try {
            String tokenForUserInfo = tokenBody;
            if (delivery == OAuth2HttpClient.UserInfoDelivery.BEARER) {
                tokenForUserInfo = OAuthTokenResponseParser.parse(tokenBody).getAccessToken();
                if (StringUtils.isBlank(tokenForUserInfo)) {
                    return null;
                }
            }
            String userinfo = oauth2HttpClient.fetchUserInfoBody(webAuth.getUserInfoUri(), tokenForUserInfo, delivery);
            return JSON.parseObject(userinfo, UserProfile.class);
        } catch (Exception e) {
            return null;
        }
    }
}
