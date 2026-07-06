package cn.org.autumn.modules.client.oauth2;

import cn.org.autumn.config.ApplicationInitializationProgress;
import cn.org.autumn.modules.oauth.oauth2.support.OAuth2HttpClient;
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
import cn.org.autumn.utils.IPUtils;
import cn.org.autumn.utils.R;
import cn.org.autumn.utils.WebPathUtils;
import com.alibaba.fastjson.JSON;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("client")
@Slf4j
public class ClientOauth2Controller {

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

    @Autowired(required = false)
    ApplicationInitializationProgress applicationInitializationProgress;

    @Autowired
    OAuth2HttpClient oauth2HttpClient;

    @RequestMapping("oauth2/callback")
    public Object defaultCodeCallback(HttpServletRequest request, HttpServletResponse response, Model model) throws OAuthSystemException {
        if (log.isDebugEnabled()) {
            String query = request.getQueryString();
            if (StringUtils.isNotBlank(query))
                query = "?" + query;
            else
                query = "";
            log.debug("Client OAuth login:{}{}", request.getRequestURL().toString(), query);
        }
        String oauthError = request.getParameter("error");
        if (StringUtils.isNotBlank(oauthError)) {
            return authCallbackErrorPage(request, response, model, oauthError, request.getParameter("error_description"));
        }
        String authCode = request.getParameter(OAuth.OAUTH_CODE);
        if (StringUtils.isEmpty(authCode)) {
            return authCallbackErrorPage(request, response, model, OAuthError.OAUTH_ERROR, "未收到授权码，请重新发起授权。");
        }
        if (ShiroUtils.needLogin()) {
            String host = request.getHeader("host");
            if (log.isDebugEnabled())
                log.debug("Login domain:{}", host);
            WebAuthenticationEntity webAuthenticationEntity = webAuthenticationService.getByClientId(host);
            if (null == webAuthenticationEntity)
                webAuthenticationEntity = webAuthenticationService.getByClientId(sysConfigService.getOauth2LoginClientId(host));
            String accessToken = getAccessToken(webAuthenticationEntity, authCode);
            UserProfile userProfile = getUserInfo(webAuthenticationEntity, accessToken);
            if (null != userProfile) {
                userProfileService.login(userProfile);
                userTokenService.saveToken(accessToken);
                if (log.isDebugEnabled())
                    log.debug("Login user:{}", userProfile);
            }
        }
        String callback = WebPathUtils.safeOauthCallbackForClient(request, request.getParameter("callback"));
        if (StringUtils.isBlank(callback)) {
            callback = WebPathUtils.forBrowser(request, "/oauth2/success");
        }
        if (log.isDebugEnabled())
            log.debug("Callback URL:{}", callback);
        return pageFactory.direct(request, response, model, callback);
    }

    private Object authCallbackErrorPage(HttpServletRequest request, HttpServletResponse response, Model model, String error, String description) {
        model.addAttribute("oauthError", error);
        model.addAttribute("loginUrl", WebPathUtils.forBrowser(request, "/login"));
        if ("access_denied".equalsIgnoreCase(error)) {
            model.addAttribute("title", "授权已取消");
            model.addAttribute("message", "您已取消授权，应用无法获取您的账号信息。如需使用相关功能，请重新发起授权并点击确认。");
        } else if (StringUtils.isNotBlank(description)) {
            model.addAttribute("title", "授权失败");
            model.addAttribute("message", description);
        } else {
            model.addAttribute("title", "授权未完成");
            model.addAttribute("message", "授权未能完成，请稍后重试。");
        }
        return pageFactory.authCallbackError(request, response, model);
    }

    private String getAccessToken(WebAuthenticationEntity webAuthClientEntity, String oauthCode) {
        String state = webAuthClientEntity.getState();
        String scope = webAuthClientEntity.getScope();
        if (null == state) {
            state = "state";
        }
        if (StringUtils.isEmpty(scope)) {
            scope = "all";
        }
        String body = oauth2HttpClient.exchangeAuthorizationCodeRaw(OAuth2HttpClient.CredentialParam.OAUTH, webAuthClientEntity.getAccessTokenUri(), webAuthClientEntity.getClientId(), webAuthClientEntity.getClientSecret(), oauthCode, webAuthClientEntity.getRedirectUri());
        log.debug(body);
        return body;
    }

    public UserProfile getUserInfo(WebAuthenticationEntity webAuthClientEntity, String accessToken) {
        try {
            String userinfo = oauth2HttpClient.fetchUserInfoBody(webAuthClientEntity.getUserInfoUri(), accessToken, OAuth2HttpClient.UserInfoDelivery.LEGACY);
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
        if (applicationInitializationProgress != null) {
            ApplicationInitializationProgress.Phase p = applicationInitializationProgress.getPhase();
            if (p == ApplicationInitializationProgress.Phase.FAILED) {
                return R.error(500, "application startup failed");
            }
            if (p != ApplicationInitializationProgress.Phase.WIZARD && !applicationInitializationProgress.isLanguageCacheReady()) {
                return R.error(503, "language cache not ready");
            }
        }
        Map<String, Object> o = healthFactory.getHealth();
        if (null != o && !o.isEmpty())
            return R.ok().put("data", o);
        else
            return R.ok();
    }

    @ResponseBody
    @RequestMapping(value = {"/version"})
    public R version() {
        Map<String, Object> o = versionFactory.getVersion();
        if (null != o && !o.isEmpty())
            return R.ok().put("data", o);
        else
            return R.ok();
    }
}
