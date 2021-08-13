package cn.org.autumn.modules.spm.filter;

import cn.org.autumn.config.Config;
import cn.org.autumn.modules.oauth.service.ClientDetailsService;
import cn.org.autumn.modules.spm.service.SuperPositionModelService;
import cn.org.autumn.modules.sys.shiro.OauthAccessTokenToken;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.wall.service.WallService;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang.StringUtils;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.message.types.ParameterStyle;
import org.apache.oltu.oauth2.rs.request.OAuthAccessResourceRequest;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authc.FormAuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class SpmFilter extends FormAuthenticationFilter {

    private static final Logger log = LoggerFactory.getLogger(SpmFilter.class);

    private static WallService wallService;
    private static SuperPositionModelService superPositionModelService;
    private static ClientDetailsService clientDetailsService;

    protected boolean isEnabled(ServletRequest request, ServletResponse response) {
        if (null == wallService)
            wallService = (WallService) Config.getBean("wallService");
        return wallService.isEnabled(request, response, false);
    }

    protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) {
        String accessToken = getAccessToken(request);
        if (StringUtils.isNotEmpty(accessToken))
            return new OauthAccessTokenToken(accessToken);
        return super.createToken(request, response);
    }

    public String getAccessToken(ServletRequest request) {
        try {
            OAuthAccessResourceRequest oauthRequest = new OAuthAccessResourceRequest((HttpServletRequest) request, ParameterStyle.QUERY, ParameterStyle.HEADER);
            String accessToken = oauthRequest.getAccessToken();
            Object resp = JSON.parse(accessToken);
            Map map = (Map) resp;
            String accessTokenKey = "";
            if (map.containsKey(OAuth.OAUTH_ACCESS_TOKEN)) {
                accessTokenKey = (String) map.get(OAuth.OAUTH_ACCESS_TOKEN);
            }
            return accessTokenKey;
        } catch (Exception e) {
            if (request instanceof HttpServletRequest) {
                HttpServletRequest httpServletRequest = (HttpServletRequest) request;
                log.debug(httpServletRequest.getServletPath());
            }
            log.debug("getAccessToken:", e);
        }
        return "";
    }

    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        if (null != request) {
            if (null == superPositionModelService)
                superPositionModelService = (SuperPositionModelService) Config.getBean("superPositionModelService");
            if (null != superPositionModelService && !superPositionModelService.needLogin(httpServletRequest, httpServletResponse))
                return true;
        }
        if (ShiroUtils.isLogin())
            return true;
        return super.isAccessAllowed(request, response, mappedValue);
    }

    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
        String accessToken = getAccessToken(request);
        if (StringUtils.isEmpty(accessToken)) {
            return super.onAccessDenied(request, response);
        }
        if (null == clientDetailsService)
            clientDetailsService = (ClientDetailsService) Config.getBean("clientDetailsService");
        assert clientDetailsService != null;
        boolean valid = clientDetailsService.isValidAccessToken(accessToken);
        if (valid)
            return executeLogin(request, response);
        return super.onAccessDenied(request, response);
    }

    protected boolean onLoginSuccess(AuthenticationToken token, Subject subject, ServletRequest request, ServletResponse response) throws Exception {
        if (token instanceof OauthAccessTokenToken)
            return true;
        return super.onLoginSuccess(token, subject, request, response);
    }

    @Override
    protected boolean onLoginFailure(AuthenticationToken token, AuthenticationException e, ServletRequest request, ServletResponse response) {
        if (token instanceof OauthAccessTokenToken) {
            throw e;
        }
        return super.onLoginFailure(token, e, request, response);
    }
}
