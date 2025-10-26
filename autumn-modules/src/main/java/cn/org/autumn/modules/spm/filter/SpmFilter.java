package cn.org.autumn.modules.spm.filter;

import cn.org.autumn.config.Config;
import cn.org.autumn.modules.oauth.service.ClientDetailsService;
import cn.org.autumn.modules.spm.service.SuperPositionModelService;
import cn.org.autumn.modules.sys.shiro.OauthAccessTokenToken;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.wall.service.WallService;
import cn.org.autumn.site.PathFactory;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.message.types.ParameterStyle;
import org.apache.oltu.oauth2.rs.request.OAuthAccessResourceRequest;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authc.FormAuthenticationFilter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@Slf4j
public class SpmFilter extends FormAuthenticationFilter implements PathFactory.Path {
    private static WallService wallService;
    private static SuperPositionModelService superPositionModelService;
    private static ClientDetailsService clientDetailsService;

    protected boolean isEnabled(ServletRequest request, ServletResponse response) throws IOException {
        if (null == wallService)
            wallService = (WallService) Config.getBean("wallService");
        if (null != wallService)
            return wallService.isEnabled(request, response, false, true);
        return true;
    }

    protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) {
        String accessToken = getAccessToken(request);
        if (StringUtils.isNotEmpty(accessToken))
            return new OauthAccessTokenToken(accessToken);
        return super.createToken(request, response);
    }

    public static String getAccessToken(ServletRequest request) {
        try {
            OAuthAccessResourceRequest oauthRequest = new OAuthAccessResourceRequest((HttpServletRequest) request, ParameterStyle.QUERY, ParameterStyle.HEADER);
            String accessToken = oauthRequest.getAccessToken();
            if (StringUtils.isBlank(accessToken))
                return "";
            accessToken = accessToken.trim();
            if (accessToken.startsWith("{") && accessToken.endsWith("}") && accessToken.contains("\"access_token\":")) {
                if (log.isDebugEnabled())
                    log.debug("数据令牌:{}", accessToken);
                Object resp = JSON.parse(accessToken);
                Map map = (Map) resp;
                String accessTokenKey = "";
                if (map.containsKey(OAuth.OAUTH_ACCESS_TOKEN)) {
                    accessTokenKey = (String) map.get(OAuth.OAUTH_ACCESS_TOKEN);
                }
                return accessTokenKey;
            } else {
                if (log.isDebugEnabled())
                    log.debug("字符令牌:{}", accessToken);
                if (accessToken.contains("\"") || accessToken.contains("{") || accessToken.contains("}") || accessToken.length() > 100)
                    return "";
                else {
                    return accessToken;
                }
            }
        } catch (Throwable e) {
            if (request instanceof HttpServletRequest) {
                HttpServletRequest httpServletRequest = (HttpServletRequest) request;
                log.debug(httpServletRequest.getServletPath());
            }
            log.debug("令牌解析:{}", e.getMessage());
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
