package cn.org.autumn.modules.oauth.oauth2;

import cn.org.autumn.modules.oauth.entity.ClientDetailsEntity;
import cn.org.autumn.modules.oauth.service.ClientDetailsService;
import cn.org.autumn.modules.oauth.store.TokenStore;
import cn.org.autumn.modules.oauth.store.ValueType;
import cn.org.autumn.modules.spm.service.SuperPositionModelService;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.modules.sys.shiro.RedisShiroSessionDAO;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.usr.dto.UserProfile;
import cn.org.autumn.modules.usr.entity.UserProfileEntity;
import cn.org.autumn.modules.usr.service.UserLoginLogService;
import cn.org.autumn.modules.usr.service.UserProfileService;
import cn.org.autumn.site.PageFactory;
import cn.org.autumn.utils.Utils;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang.StringUtils;
import org.apache.oltu.oauth2.as.issuer.MD5Generator;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuerImpl;
import org.apache.oltu.oauth2.as.request.OAuthAuthzRequest;
import org.apache.oltu.oauth2.as.request.OAuthTokenRequest;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.message.types.ParameterStyle;
import org.apache.oltu.oauth2.common.message.types.ResponseType;
import org.apache.oltu.oauth2.common.utils.OAuthUtils;
import org.apache.oltu.oauth2.rs.request.OAuthAccessResourceRequest;
import org.apache.oltu.oauth2.rs.response.OAuthRSResponse;
import org.apache.shiro.authc.*;
import org.apache.shiro.web.util.SavedRequest;
import org.apache.shiro.web.util.WebUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.Map;

import static com.baomidou.mybatisplus.toolkit.StringUtils.UTF8;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.apache.oltu.oauth2.common.OAuth.HttpMethod.POST;
import static org.apache.oltu.oauth2.common.OAuth.OAUTH_CODE;
import static org.apache.oltu.oauth2.common.OAuth.OAUTH_STATE;
import static org.apache.oltu.oauth2.common.error.OAuthError.OAUTH_ERROR_URI;
import static org.apache.oltu.oauth2.common.error.OAuthError.TokenResponse.*;

@Controller
@RequestMapping("/oauth2")
public class AuthorizationController {

    @Autowired
    ClientDetailsService clientDetailsService;

    @Autowired
    SuperPositionModelService superPositionModelService;

    @Autowired
    SysUserService sysUserService;

    @Autowired
    UserProfileService userProfileService;

    @Autowired
    UserLoginLogService userLoginLogService;

    @Autowired
    RedisShiroSessionDAO redisShiroSessionDAO;

    @Autowired
    PageFactory pageFactory;

    private ResponseEntity error(String description, String error, int errorResponse) throws OAuthSystemException {
        OAuthResponse oAuthResponse = OAuthASResponse
                .errorResponse(errorResponse)
                .setError(error)
                .setErrorDescription(description)
                .buildJSONMessage();
        return new ResponseEntity(oAuthResponse.getBody(), HttpStatus.valueOf(oAuthResponse.getResponseStatus()));
    }

    @RequestMapping("login")
    public Object login(HttpServletRequest request, HttpServletResponse response, String username, String password, boolean rememberMe, Model model) throws UnsupportedEncodingException {
        String error = "";
        String callback = Utils.getCallback(request);
        try {
            if (request.getMethod().equalsIgnoreCase(POST)) {
                SavedRequest savedRequest = WebUtils.getSavedRequest(request);
                String back = callback;
                if (null != savedRequest) {
                    if (StringUtils.isBlank(callback))
                        back = savedRequest.getRequestUrl();
                    else
                        back = callback + "&callback=" + savedRequest.getRequestUrl();
                }
                if (StringUtils.isBlank(back))
                    back = "/";
                sysUserService.login(username, password, rememberMe);
                return "redirect:" + back;
            }
        } catch (UnknownAccountException e) {
            error = e.getMessage();
        } catch (IncorrectCredentialsException e) {
            error = "账号或密码不正确";
        } catch (LockedAccountException e) {
            error = "账号已被锁定,请联系管理员";
        } catch (AuthenticationException e) {
            error = "账户验证失败";
        }
        if (StringUtils.isNotBlank(error)) {
            if (error.length() > 200)
                error = error.substring(0, 200);
            if (StringUtils.isBlank(callback))
                return "redirect:/oauth2/login?error=" + URLEncoder.encode(error, "utf-8");
            else
                return "redirect:/oauth2/login?callback=" + URLEncoder.encode(callback, "utf-8") + "&error=" + URLEncoder.encode(error, "utf-8");
        }
        String perror = request.getParameter("error");
        if (StringUtils.isBlank(perror))
            perror = "";
        model.addAttribute("error", perror);
        return pageFactory.login(request, response, model);
    }

    @RequestMapping("authorize")
    public Object applyAuthorize(HttpServletRequest request, HttpServletResponse response, Model model) throws OAuthSystemException, OAuthProblemException {
        if (ShiroUtils.needLogin()) {
            ModelAndView mav1 = new ModelAndView();
            String url = request.getRequestURL().toString();
            String queryString = request.getQueryString();
            if (StringUtils.isNotEmpty(queryString)) {
                url = url + "?" + queryString;
            }
            mav1.addObject("callback", url);
            mav1.setViewName("redirect:/oauth2/login");
            return mav1;
        }

        //构建OAuth请求
        OAuthAuthzRequest oAuthzRequest = new OAuthAuthzRequest(request);
        //获取OAuth客户端Id
        String clientId = oAuthzRequest.getClientId();
        //校验客户端Id是否正确
        ClientDetailsEntity clientDetailsEntity = clientDetailsService.findByClientId(clientId);
        if (clientDetailsEntity == null) {
            return error("无效的客户端ID", INVALID_CLIENT, SC_BAD_REQUEST);
        }

        if (null == clientDetailsEntity.getTrusted() || 0 == clientDetailsEntity.getTrusted()) {
            return error("不受信任的客户端ID", INVALID_CLIENT, SC_BAD_REQUEST);
        }

        if (null != clientDetailsEntity.getArchived() && 1 == clientDetailsEntity.getArchived()) {
            return error("客户端ID已归档，不能使用", INVALID_CLIENT, SC_BAD_REQUEST);
        }

        //生成授权码
        String authCode = null;
        String responseType = oAuthzRequest.getParam(OAuth.OAUTH_RESPONSE_TYPE);
        //ResponseType仅支持CODE和TOKEN
        if (responseType.equals(ResponseType.CODE.toString())) {
            OAuthIssuerImpl oAuthIssuerImpl = new OAuthIssuerImpl(new MD5Generator());
            authCode = oAuthIssuerImpl.authorizationCode();
            clientDetailsService.putAuthCode(authCode, ShiroUtils.getUserEntity());
        }

        //构建OAuth响应
        OAuthASResponse.OAuthAuthorizationResponseBuilder builder = OAuthASResponse.authorizationResponse(request, HttpServletResponse.SC_FOUND);

        //设置授权码
        builder.setCode(authCode);

        //获取客户端重定向地址
        String redirectURI = oAuthzRequest.getParam(OAuth.OAUTH_REDIRECT_URI);

        if (StringUtils.isEmpty(redirectURI) || StringUtils.isEmpty(clientDetailsEntity.getRedirectUri())) {
            return error("redirect_uri should not be empty.", OAUTH_ERROR_URI, SC_BAD_REQUEST);
        }

        if (!redirectURI.equalsIgnoreCase(clientDetailsEntity.getRedirectUri())) {
            return error("redirect_uri does not match the uri in system.", OAUTH_ERROR_URI, SC_BAD_REQUEST);
        }
        ModelAndView mav = new ModelAndView();
        mav.addObject(OAUTH_CODE, authCode);
        String state = request.getParameter(OAUTH_STATE);
        if (StringUtils.isNotEmpty(state))
            mav.addObject(OAUTH_STATE, state);
        String callback = request.getParameter("callback");
        if (null == callback)
            callback = "";
        else
            callback = "?callback=" + callback;
        mav.setViewName("redirect:" + redirectURI + callback);
        return mav;
    }

    @RequestMapping(value = "token", method = RequestMethod.POST)
    public HttpEntity applyAccessToken(HttpServletRequest request) throws OAuthSystemException, OAuthProblemException {
        //构建OAuth请求
        OAuthTokenRequest tokenRequest = new OAuthTokenRequest(request);
        //获取OAuth客户端Id
        String clientId = tokenRequest.getClientId();
        //校验客户端Id是否正确
        ClientDetailsEntity authClient = clientDetailsService.findByClientId(clientId);
        if (authClient == null) {
            return error("无效的客户端Id", INVALID_CLIENT, SC_BAD_REQUEST);
        }

        if (null == authClient.getTrusted() || 0 == authClient.getTrusted()) {
            return error("不受信任的客户端ID", INVALID_CLIENT, SC_BAD_REQUEST);
        }

        if (null != authClient.getArchived() && 1 == authClient.getArchived()) {
            return error("客户端ID已归档，不能使用", INVALID_CLIENT, SC_BAD_REQUEST);
        }

        //检查客户端安全KEY是否正确
        if (!clientDetailsService.isValidClientSecret(tokenRequest.getClientSecret())) {
            return error("客户端安全KEY认证失败！", UNAUTHORIZED_CLIENT, SC_UNAUTHORIZED);
        }
        String grantType = tokenRequest.getParam(OAuth.OAUTH_GRANT_TYPE);
        if (StringUtils.isBlank(grantType))
            return error("非法授权", INVALID_GRANT, SC_BAD_REQUEST);

        if (!authClient.granted(grantType))
            return error("未获得授权", INVALID_GRANT, SC_BAD_REQUEST);

        String authCode = tokenRequest.getParam(OAuth.OAUTH_CODE);
        TokenStore tokenStore = null;
        //验证类型，有AUTHORIZATION_CODE/PASSWORD/REFRESH_TOKEN/CLIENT_CREDENTIALS
        //1. 授权码获取Token模式
        if (tokenRequest.getParam(OAuth.OAUTH_GRANT_TYPE).equals(GrantType.AUTHORIZATION_CODE.toString())) {
            if (!clientDetailsService.isValidCode(authCode)) {
                return error("错误的授权码", INVALID_GRANT, SC_BAD_REQUEST);
            }
            tokenStore = clientDetailsService.get(ValueType.authCode, authCode);
        }

        //2. 使用Refresh Token 获取Token模式
        if (tokenRequest.getParam(OAuth.OAUTH_GRANT_TYPE).equals(GrantType.REFRESH_TOKEN.toString())) {
            if (!clientDetailsService.isValidRefreshToken(tokenRequest.getRefreshToken())) {
                return error("错误的Refresh Token", INVALID_GRANT, SC_BAD_REQUEST);
            }
            tokenStore = clientDetailsService.get(ValueType.refreshToken, tokenRequest.getRefreshToken());
        }

        //3.客户端证书授权模式
        if (tokenRequest.getParam(OAuth.OAUTH_GRANT_TYPE).equals(GrantType.CLIENT_CREDENTIALS.toString())) {
            SysUserEntity sysUserEntity = sysUserService.getByUsername(authClient.getClientId());
            if (null == sysUserEntity) {
                clientDetailsService.clientToUser(null);
                sysUserEntity = sysUserService.getByUsername(authClient.getClientId());
            }
            tokenStore = new TokenStore(sysUserEntity);
        }

        //4.密码授权模式
        if (tokenRequest.getParam(OAuth.OAUTH_GRANT_TYPE).equals(GrantType.PASSWORD.toString())) {
            String username = tokenRequest.getUsername();
            String password = tokenRequest.getPassword();
            sysUserService.login(username, password, false);
            boolean isLogin = ShiroUtils.isLogin();
            if (isLogin) {
                SysUserEntity sysUserEntity = sysUserService.getByUsername(username);
                tokenStore = new TokenStore(sysUserEntity);
            }
        }

        //5. 简化授权模式
        if (tokenRequest.getParam(OAuth.OAUTH_GRANT_TYPE).equals(GrantType.IMPLICIT.toString())) {

        }

        if (null == tokenStore) {
            return error("非法授权", INVALID_GRANT, SC_BAD_REQUEST);
        }

        //生成访问令牌
        OAuthIssuerImpl authIssuerImpl = new OAuthIssuerImpl(new MD5Generator());
        String accessToken = authIssuerImpl.accessToken();
        String refreshToken = authIssuerImpl.refreshToken();

        /**
         * accessToken  过期时间：24 * 60 * 60L      一天
         * refreshToken 过期时间：7 * 24 * 60 * 60L  一周
         * 如果服务器重启或者Redis重启后，将立即过期
         */
        clientDetailsService.putToken(accessToken, refreshToken, tokenStore);

        //生成OAuth响应
        OAuthResponse response = OAuthASResponse
                .tokenResponse(HttpServletResponse.SC_OK)
                .setAccessToken(accessToken)
                .setRefreshToken(refreshToken)
                .setTokenType("bearer")
                .setExpiresIn(String.valueOf(TokenStore.getExpireIn()))
                .setScope("basic")
                .buildJSONMessage();
        return new ResponseEntity(response.getBody(), HttpStatus.valueOf(response.getResponseStatus()));
    }

    @RequestMapping(value = "/userInfo", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public HttpEntity authUserInfo(HttpServletRequest request) throws OAuthSystemException {
        try {
            // 构建OAuth资源请求
            OAuthAccessResourceRequest oauthRequest = new OAuthAccessResourceRequest(request, ParameterStyle.QUERY);
            String accessToken = oauthRequest.getAccessToken();

            Object resp = JSON.parse(accessToken);
            Map map = (Map) resp;
            String accessTokenKey = "";
            if (map.containsKey(OAuth.OAUTH_ACCESS_TOKEN)) {
                accessTokenKey = (String) map.get(OAuth.OAUTH_ACCESS_TOKEN);
            }

            // 验证访问令牌
            if (!clientDetailsService.isValidAccessToken(accessTokenKey)) {
                // 如果不存在/过期了，返回未验证错误，需重新验证
                OAuthResponse oauthResponse = OAuthRSResponse.errorResponse(SC_UNAUTHORIZED)
                        .setRealm("Apache Oltu").setError(OAuthError.ResourceResponse.INVALID_TOKEN)
                        .buildHeaderMessage();

                HttpHeaders headers = new HttpHeaders();
                headers.add(OAuth.HeaderType.WWW_AUTHENTICATE,
                        oauthResponse.getHeader(OAuth.HeaderType.WWW_AUTHENTICATE));
                return new ResponseEntity(headers, HttpStatus.UNAUTHORIZED);
            }

            //返回用户名
            TokenStore tokenStore = clientDetailsService.get(ValueType.accessToken, accessTokenKey);

            Object username = tokenStore.getValue();

            if (username instanceof SysUserEntity) {
                SysUserEntity sysUserEntity = (SysUserEntity) username;
                UserProfileEntity userProfileEntity = userProfileService.from(sysUserEntity, null, null);
                UserProfile userProfile = UserProfile.from(userProfileEntity);
                username = userProfile;
                userLoginLogService.login(userProfileEntity);
            }

            return new ResponseEntity(JSON.toJSONString(username), HttpStatus.OK);
        } catch (OAuthProblemException e) {
            // 检查是否设置了错误码
            String errorCode = e.getError();
            if (OAuthUtils.isEmpty(errorCode)) {
                OAuthResponse oauthResponse = OAuthRSResponse.errorResponse(SC_UNAUTHORIZED).buildHeaderMessage();

                HttpHeaders headers = new HttpHeaders();
                headers.add(OAuth.HeaderType.WWW_AUTHENTICATE,
                        oauthResponse.getHeader(OAuth.HeaderType.WWW_AUTHENTICATE));

                return new ResponseEntity(headers, HttpStatus.UNAUTHORIZED);
            }

            OAuthResponse oauthResponse = OAuthRSResponse
                    .errorResponse(SC_UNAUTHORIZED)
                    .setError(e.getError())
                    .setErrorDescription(e.getDescription())
                    .setErrorUri(e.getUri())
                    .buildHeaderMessage();

            HttpHeaders headers = new HttpHeaders();
            headers.add(OAuth.HeaderType.WWW_AUTHENTICATE,
                    oauthResponse.getHeader(OAuth.HeaderType.WWW_AUTHENTICATE));

            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }
    }
}