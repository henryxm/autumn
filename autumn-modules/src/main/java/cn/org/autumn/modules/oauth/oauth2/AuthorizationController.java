package cn.org.autumn.modules.oauth.oauth2;

import cn.org.autumn.modules.oauth.entity.ClientDetailsEntity;
import cn.org.autumn.modules.oauth.service.ClientDetailsService;
import cn.org.autumn.modules.spm.entity.SuperPositionModelEntity;
import cn.org.autumn.modules.spm.service.SuperPositionModelService;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.user.entity.UserEntity;
import cn.org.autumn.site.RootSite;
import cn.org.autumn.utils.R;
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
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.Map;

import static com.baomidou.mybatisplus.toolkit.StringUtils.UTF8;
import static org.apache.oltu.oauth2.common.OAuth.HttpMethod.POST;
import static org.apache.oltu.oauth2.common.OAuth.OAUTH_CODE;
import static org.apache.oltu.oauth2.common.OAuth.OAUTH_STATE;


@Controller
@RequestMapping("/oauth2")
public class AuthorizationController {

    @Autowired
    ClientDetailsService clientDetailsService;

    @Autowired
    SuperPositionModelService superPositionModelService;

    @Autowired
    SysUserService sysUserService;

    private String getCallBack(HttpServletRequest request) {
        try {
            String refer = request.getHeader("referer");
            URL uri = new URL(refer);
            String query = uri.getQuery();
            String[] dd = query.split("&");
            for (String b : dd) {
                if (b.startsWith("callback=")) {
                    String[] a = b.split("=");
                    if (a.length == 2)
                        return URLDecoder.decode(a[1], UTF8);
                }
            }
        } catch (MalformedURLException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    @RequestMapping("login")
    public Object login(HttpServletRequest request, String username, String password) {
        try {
            if (request.getMethod().equalsIgnoreCase(POST)) {
                String callback = getCallBack(request);
                if (!StringUtils.isEmpty(callback)) {
                    Subject subject = ShiroUtils.getSubject();
                    UsernamePasswordToken token = new UsernamePasswordToken(username, password);
                    subject.login(token);
                    return "redirect:" + callback;
                }
            }
        } catch (UnknownAccountException e) {
            return R.error(e.getMessage());
        } catch (IncorrectCredentialsException e) {
            return R.error("账号或密码不正确");
        } catch (LockedAccountException e) {
            return R.error("账号已被锁定,请联系管理员");
        } catch (AuthenticationException e) {
            return R.error("账户验证失败");
        }
        return "oauth2/login";
    }

    @RequestMapping("authorize")
    public Object applyAuthorize(HttpServletRequest request) throws OAuthSystemException, OAuthProblemException {
        boolean isLogin = ShiroUtils.isLogin();
        if (!isLogin) {
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
            OAuthResponse response =
                    OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                            .setError(OAuthError.TokenResponse.INVALID_CLIENT)
                            .setErrorDescription("无效的客户端ID")
                            .buildJSONMessage();
            return new ResponseEntity(response.getBody(), HttpStatus.valueOf(response.getResponseStatus()));
        }
        SysUserEntity userEntity = ShiroUtils.getUserEntity();
        //生成授权码
        String authCode = null;
        String responseType = oAuthzRequest.getParam(OAuth.OAUTH_RESPONSE_TYPE);
        //ResponseType仅支持CODE和TOKEN
        if (responseType.equals(ResponseType.CODE.toString())) {
            OAuthIssuerImpl oAuthIssuerImpl = new OAuthIssuerImpl(new MD5Generator());
            authCode = oAuthIssuerImpl.authorizationCode();
            clientDetailsService.addAuthCode(authCode, userEntity.getUsername());
        }

        //构建OAuth响应
        OAuthASResponse.OAuthAuthorizationResponseBuilder builder = OAuthASResponse.authorizationResponse(request, HttpServletResponse.SC_FOUND);

        //设置授权码
        builder.setCode(authCode);

        //获取客户端重定向地址
        String redirectURI = oAuthzRequest.getParam(OAuth.OAUTH_REDIRECT_URI);

        if (StringUtils.isEmpty(redirectURI) || StringUtils.isEmpty(clientDetailsEntity.getRedirectUri())) {
            OAuthResponse response =
                    OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                            .setError(OAuthError.OAUTH_ERROR_URI)
                            .setErrorDescription("redirect_uri should not be empty.")
                            .buildJSONMessage();
            return new ResponseEntity(response.getBody(), HttpStatus.valueOf(response.getResponseStatus()));
        }

        if (!redirectURI.equalsIgnoreCase(clientDetailsEntity.getRedirectUri())) {
            OAuthResponse response =
                    OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                            .setError(OAuthError.OAUTH_ERROR_URI)
                            .setErrorDescription("redirect_uri does not match the uri in system.")
                            .buildJSONMessage();
            return new ResponseEntity(response.getBody(), HttpStatus.valueOf(response.getResponseStatus()));
        }
        ModelAndView mav = new ModelAndView();
        mav.addObject(OAUTH_CODE, authCode);
        String state = request.getParameter(OAUTH_STATE);
        if (StringUtils.isNotEmpty(state))
            mav.addObject(OAUTH_STATE, state);
        mav.setViewName("redirect:" + redirectURI);
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
            OAuthResponse oAuthResponse = OAuthASResponse
                    .errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                    .setError(OAuthError.TokenResponse.INVALID_CLIENT)
                    .setErrorDescription("无效的客户端Id")
                    .buildJSONMessage();
            return new ResponseEntity(oAuthResponse.getBody(), HttpStatus.valueOf(oAuthResponse.getResponseStatus()));
        }

        //检查客户端安全KEY是否正确
        if (!clientDetailsService.checkClientSecret(tokenRequest.getClientSecret())) {
            OAuthResponse response = OAuthResponse.errorResponse(HttpServletResponse.SC_UNAUTHORIZED)
                    .setError(OAuthError.TokenResponse.UNAUTHORIZED_CLIENT)
                    .setErrorDescription("客户端安全KEY认证失败！")
                    .buildJSONMessage();
            return new ResponseEntity(response.getBody(), HttpStatus.valueOf(response.getResponseStatus()));
        }

        String authCode = tokenRequest.getParam(OAuth.OAUTH_CODE);
        //验证类型，有AUTHORIZATION_CODE/PASSWORD/REFRESH_TOKEN/CLIENT_CREDENTIALS
        if (tokenRequest.getParam(OAuth.OAUTH_GRANT_TYPE).equals(GrantType.AUTHORIZATION_CODE.toString())) {
            if (!clientDetailsService.checkAuthCode(authCode)) {
                OAuthResponse response = OAuthResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                        .setError(OAuthError.TokenResponse.INVALID_GRANT)
                        .setErrorDescription("错误的授权码")
                        .buildJSONMessage();
                return new ResponseEntity(response.getBody(), HttpStatus.valueOf(response.getResponseStatus()));
            }
        }

        //生成访问令牌
        OAuthIssuerImpl authIssuerImpl = new OAuthIssuerImpl(new MD5Generator());
        String accessToken = authIssuerImpl.accessToken();
        String refreshToken = authIssuerImpl.refreshToken();

        clientDetailsService.addAccessToken(accessToken, clientDetailsService.getUsernameByAuthCode(authCode));

        //生成OAuth响应
        OAuthResponse response = OAuthASResponse
                .tokenResponse(HttpServletResponse.SC_OK)
                .setAccessToken(accessToken)
                .setRefreshToken(refreshToken)
                .setExpiresIn(String.valueOf(clientDetailsService.getExpireIn()))
                .buildJSONMessage();
        return new ResponseEntity(response.getBody(), HttpStatus.valueOf(response.getResponseStatus()));
    }


    @RequestMapping(value = "/userInfo", produces = "text/html;charset=UTF-8")
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
            if (!clientDetailsService.checkAccessToken(accessTokenKey)) {
                // 如果不存在/过期了，返回未验证错误，需重新验证
                OAuthResponse oauthResponse = OAuthRSResponse.errorResponse(HttpServletResponse.SC_UNAUTHORIZED)
                        .setRealm("Apache Oltu").setError(OAuthError.ResourceResponse.INVALID_TOKEN)
                        .buildHeaderMessage();

                HttpHeaders headers = new HttpHeaders();
                headers.add(OAuth.HeaderType.WWW_AUTHENTICATE,
                        oauthResponse.getHeader(OAuth.HeaderType.WWW_AUTHENTICATE));
                return new ResponseEntity(headers, HttpStatus.UNAUTHORIZED);
            }

            // 返回用户名
            String username = clientDetailsService.getUsernameByAccessToken(accessTokenKey);
            return new ResponseEntity(username, HttpStatus.OK);
        } catch (OAuthProblemException e) {
            e.printStackTrace();

            // 检查是否设置了错误码
            String errorCode = e.getError();
            if (OAuthUtils.isEmpty(errorCode)) {
                OAuthResponse oauthResponse = OAuthRSResponse.errorResponse(HttpServletResponse.SC_UNAUTHORIZED)
                        .buildHeaderMessage();

                HttpHeaders headers = new HttpHeaders();
                headers.add(OAuth.HeaderType.WWW_AUTHENTICATE,
                        oauthResponse.getHeader(OAuth.HeaderType.WWW_AUTHENTICATE));

                return new ResponseEntity(headers, HttpStatus.UNAUTHORIZED);
            }

            OAuthResponse oauthResponse = OAuthRSResponse
                    .errorResponse(HttpServletResponse.SC_UNAUTHORIZED)
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
