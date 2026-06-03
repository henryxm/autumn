package cn.org.autumn.modules.spm.filter;

import cn.org.autumn.config.Config;
import cn.org.autumn.model.Error;
import cn.org.autumn.model.Response;
import cn.org.autumn.modules.oauth.service.ClientDetailsService;
import cn.org.autumn.modules.spm.service.SuperPositionModelService;
import cn.org.autumn.modules.bot.service.RobotTokenService;
import cn.org.autumn.modules.bot.shiro.RobotAccessTokenToken;
import cn.org.autumn.modules.sys.shiro.ClientIpSessionSupport;
import cn.org.autumn.modules.sys.shiro.OauthAccessTokenToken;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.wall.service.WallService;
import cn.org.autumn.site.PathFactory;
import cn.org.autumn.utils.IPUtils;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.message.types.ParameterStyle;
import org.apache.oltu.oauth2.rs.request.OAuthAccessResourceRequest;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authc.FormAuthenticationFilter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
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
        if (StringUtils.isNotEmpty(accessToken)) {
            if (accessToken.startsWith(RobotTokenService.TOKEN_PREFIX))
                return new RobotAccessTokenToken(accessToken);
            return new OauthAccessTokenToken(accessToken);
        }
        return super.createToken(request, response);
    }

    public static String getAccessToken(ServletRequest request) {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String robotHeader = httpRequest.getHeader("X-Robot-Token");
            if (StringUtils.isNotBlank(robotHeader))
                return robotHeader.trim();
        }
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
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) resp;
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
        if (ShiroUtils.isLogin()) {
            try {
                Session session = ShiroUtils.getSubject().getSession(false);
                if (session != null) ClientIpSessionSupport.syncHost(session, httpServletRequest);
            } catch (Exception ignored) {
            }
            return true;
        }
        return super.isAccessAllowed(request, response, mappedValue);
    }

    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
        boolean access = onAccessDeniedInternal(request, response);
        if (!access) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            // 判断是否为API请求
            if (isApiRequest(httpRequest)) {
                // API请求，返回JSON错误响应
                return handleUnauthorized(httpRequest, (HttpServletResponse) response);
            }
        }
        return access;
    }

    protected boolean onAccessDeniedInternal(ServletRequest request, ServletResponse response) throws Exception {
        String accessToken = getAccessToken(request);
        if (StringUtils.isEmpty(accessToken)) {
            return super.onAccessDenied(request, response);
        }
        if (accessToken.startsWith(RobotTokenService.TOKEN_PREFIX))
            return executeLogin(request, response);
        if (null == clientDetailsService)
            clientDetailsService = (ClientDetailsService) Config.getBean("clientDetailsService");
        if (null != clientDetailsService) {
            boolean valid = clientDetailsService.isValidAccessToken(accessToken);
            if (valid)
                return executeLogin(request, response);
        }
        return super.onAccessDenied(request, response);
    }

    /**
     * 判断是否为API请求
     * 通过检查Accept头、Content-Type头或请求路径来判断
     *
     * @param request HTTP请求
     * @return true-是API请求，false-是HTML请求
     */
    private boolean isApiRequest(HttpServletRequest request) {
        // 1. 检查Accept头，如果包含application/json，认为是API请求
        String accept = request.getHeader("Accept");
        if (StringUtils.isNotBlank(accept) && accept.contains("application/json")) {
            return true;
        }
        // 2. 检查Content-Type头，如果是application/json，认为是API请求
        String contentType = request.getContentType();
        if (StringUtils.isNotBlank(contentType) && contentType.contains("application/json")) {
            return true;
        }
        // 3. 检查请求路径，如果包含/api/，认为是API请求
        String requestURI = request.getRequestURI();
        if (StringUtils.isNotBlank(requestURI) && requestURI.contains("/api/")) {
            return true;
        }
        // 4. 检查请求路径，如果以.json结尾，认为是API请求
        if (StringUtils.isNotBlank(requestURI) && requestURI.endsWith(".json")) {
            return true;
        }
        // 5. 检查X-Requested-With头，如果是XMLHttpRequest，认为是AJAX请求（API请求）
        String requestedWith = request.getHeader("X-Requested-With");
        if (StringUtils.isNotBlank(requestedWith) && "XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
            return true;
        }
        return false;
    }

    /**
     * 处理API未授权请求
     * 返回JSON格式的错误响应
     *
     * @param request  HTTP请求
     * @param response HTTP响应
     * @return false，表示不继续处理请求
     */
    /**
     * 管理端 {@code index.html} 通过 iframe 加载 {@code main.html} 等子页。未登录时若仅对 iframe 内请求做 302 登录，
     * 浏览器只在 iframe 跟随重定向，导致「外壳 + 内嵌登录」的怪异布局。此处改为返回极小 HTML，强制在顶层窗口跳转登录页。
     */
    @Override
    protected void saveRequestAndRedirectToLogin(ServletRequest request, ServletResponse response) throws IOException {
        saveRequest(request);
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        if (httpResponse.isCommitted()) {
            return;
        }
        String loginUrl = getLoginUrl();
        writeTopWindowLoginRedirect((HttpServletRequest) request, httpResponse, loginUrl);
    }

    private static void writeTopWindowLoginRedirect(HttpServletRequest request, HttpServletResponse response, String loginUrl) throws IOException {
        response.resetBuffer();
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/html;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        String target = loginUrl == null ? "" : loginUrl.trim();
        if (target.isEmpty()) {
            target = request.getContextPath() + "/login";
        } else if (!target.startsWith("http://") && !target.startsWith("https://") && !target.startsWith("/")) {
            target = request.getContextPath() + "/" + target;
        } else if (target.startsWith("/")) {
            target = request.getContextPath() + target;
        }
        String asJson = JSON.toJSONString(target);
        PrintWriter w = response.getWriter();
        w.write("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>Redirect</title></head><body>");
        w.write("<script>(function(){var u=");
        w.write(asJson);
        w.write(";try{if(window.top!==window.self){window.top.location.replace(u);}else{window.location.replace(u);}}catch(e){window.location.replace(u);}})();</script>");
        w.write("</body></html>");
        w.flush();
    }

    private boolean handleUnauthorized(HttpServletRequest request, HttpServletResponse response) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("请求未授权:{}, IP:{}, 代理:{}", request.getRequestURI(), IPUtils.getIp(request), request.getHeader("user-agent"));
            }
            // 设置响应头
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            // 写入响应
            PrintWriter writer = response.getWriter();
            writer.write(JSON.toJSONString(Response.error(Error.UNAUTHORIZED)));
            writer.flush();
            writer.close();
            return false; // 不继续处理请求
        } catch (IOException e) {
            log.error("未授权响应:{}", e.getMessage());
            return false;
        }
    }

    protected boolean onLoginSuccess(AuthenticationToken token, Subject subject, ServletRequest request, ServletResponse response) throws Exception {
        if (token instanceof OauthAccessTokenToken || token instanceof RobotAccessTokenToken)
            return true;
        return super.onLoginSuccess(token, subject, request, response);
    }

    @Override
    protected boolean onLoginFailure(AuthenticationToken token, AuthenticationException e, ServletRequest request, ServletResponse response) {
        if (token instanceof OauthAccessTokenToken || token instanceof RobotAccessTokenToken) {
            throw e;
        }
        return super.onLoginFailure(token, e, request, response);
    }
}
