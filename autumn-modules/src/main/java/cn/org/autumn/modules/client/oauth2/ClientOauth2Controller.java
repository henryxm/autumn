package cn.org.autumn.modules.client.oauth2;

import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.client.service.WebAuthenticationService;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.usr.dto.UserProfile;
import cn.org.autumn.modules.usr.service.UserProfileService;
import cn.org.autumn.modules.usr.service.UserTokenService;
import cn.org.autumn.site.HealthFactory;
import cn.org.autumn.site.PageFactory;
import cn.org.autumn.site.VersionFactory;
import cn.org.autumn.utils.HttpClientUtils;
import cn.org.autumn.utils.IPUtils;
import cn.org.autumn.utils.R;
import com.alibaba.fastjson2.JSON;
import org.apache.commons.lang3.StringUtils;
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
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("client")
public class ClientOauth2Controller {
    final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    WebAuthenticationService webAuthenticationService;

    @Autowired
    UserProfileService userProfileService;

    @Autowired
    SysConfigService sysConfigService;

    @Autowired
    UserTokenService userTokenService;

    @Autowired
    PageFactory pageFactory;

    @Autowired
    HealthFactory healthFactory;

    @Autowired
    VersionFactory versionFactory;

    @RequestMapping("oauth2/callback")
    public Object defaultCodeCallback(HttpServletRequest request, HttpServletResponse response, Model model) throws OAuthSystemException {
        if (log.isDebugEnabled()) {
            String query = request.getQueryString();
            if (StringUtils.isNotBlank(query))
                query = "?" + query;
            else
                query = "";
            log.debug("客户端授权登录:{}{}", request.getRequestURL().toString(), query);
        }
        String authCode = request.getParameter(OAuth.OAUTH_CODE);
        if (StringUtils.isEmpty(authCode)) {
            OAuthResponse oAuthResponse =
                    OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                            .setError(OAuthError.OAUTH_ERROR)
                            .setErrorDescription("Code should not be empty.")
                            .buildJSONMessage();
            return new ResponseEntity(oAuthResponse.getBody(), HttpStatus.valueOf(oAuthResponse.getResponseStatus()));
        }
        if (ShiroUtils.needLogin()) {
            String host = request.getHeader("host");
            if (log.isDebugEnabled())
                log.debug("登录域名:{}", host);
            WebAuthenticationEntity webAuthenticationEntity = webAuthenticationService.getByClientId(host);
            if (null == webAuthenticationEntity)
                webAuthenticationEntity = webAuthenticationService.getByClientId(sysConfigService.getOauth2LoginClientId(host));
            String accessToken = getAccessToken(webAuthenticationEntity, authCode);
            UserProfile userProfile = getUserInfo(webAuthenticationEntity, accessToken);
            if (null != userProfile) {
                userProfileService.login(userProfile);
                userTokenService.saveToken(accessToken);
                if (log.isDebugEnabled())
                    log.debug("登录用户:{}", userProfile);
            }
        }
        String callback = request.getParameter("callback");

        if (StringUtils.isBlank(callback)
                || "null".equalsIgnoreCase(callback)
                || callback.endsWith(".js")
                || callback.endsWith(".css"))
            callback = "/";

        if (!callback.contains("?spm=") && !callback.endsWith(".html")) {
            callback = "/";
        }
        if (log.isDebugEnabled())
            log.debug("回调地址:{}", callback);
        return pageFactory.direct(request, response, model, callback);
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
        log.debug(accessToken);
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
            return JSON.parseObject(userinfo, UserProfile.class);
        } catch (Exception e) {
            log.error("getUserInfo error：" + e.getMessage());
        }
        return null;
    }

    @ResponseBody
    @RequestMapping(value = {"/myip"})
    public String myip(HttpServletRequest request) {
        return IPUtils.getIp(request);
    }

    @ResponseBody
    @RequestMapping(value = {"/health"})
    public R health() {
        Map<String, Object> o = healthFactory.getHealth();
        if (null != o && !o.isEmpty())
            return R.ok().put("data", o);
        else
            return R.ok();
    }

    @ResponseBody
    @RequestMapping(value = {"/version"})
    public R version() {
        Map<String, String> o = versionFactory.getVersions();
        if (null != o && !o.isEmpty())
            return R.ok().put("data", o);
        else
            return R.ok();
    }
}
