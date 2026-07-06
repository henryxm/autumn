package cn.org.autumn.modules.spm.filter;

import cn.org.autumn.config.Config;
import cn.org.autumn.model.AccountAuthConfig;
import cn.org.autumn.model.Error;
import cn.org.autumn.model.Response;
import cn.org.autumn.modules.bot.service.RobotTokenService;
import cn.org.autumn.modules.bot.shiro.RobotAccessTokenToken;
import cn.org.autumn.modules.oauth.oauth2.support.OAuthAccessTokenResolver;
import cn.org.autumn.modules.oauth.service.ClientDetailsService;
import cn.org.autumn.modules.spm.service.SuperPositionModelService;
import cn.org.autumn.modules.sys.controller.SysAuthSupport;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.sys.shiro.ClientIpSessionSupport;
import cn.org.autumn.modules.sys.shiro.OauthAccessTokenToken;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.wall.service.WallService;
import cn.org.autumn.site.PathFactory;
import cn.org.autumn.utils.IPUtils;
import cn.org.autumn.utils.HttpNavigationUtils;
import cn.org.autumn.utils.WebPathUtils;
import com.alibaba.fastjson.JSON;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authc.FormAuthenticationFilter;
import org.apache.shiro.web.util.SavedRequest;
import org.apache.shiro.web.util.WebUtils;

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
            if (StringUtils.isNotBlank(robotHeader)) {
                return robotHeader.trim();
            }
            return OAuthAccessTokenResolver.resolve(httpRequest, OAuthAccessTokenResolver.Policy.PERMISSIVE);
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
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        if (tryAccessTokenLogin(request, response))
            return true;
        // API/AJAX 须先于 super.onAccessDenied：父类会 saveRequest，污染登录成功后的 SavedRequest
        if (isApiOrAjaxRequest(httpRequest))
            return handleUnauthorized(httpRequest, (HttpServletResponse) response);
        return super.onAccessDenied(request, response);
    }

    /**
     * 双保险：即便其它过滤器链误调 {@code saveRequest}，也不持久化 API/REST 到 SavedRequest。
     */
    @Override
    protected void saveRequest(ServletRequest request) {
        if (request instanceof HttpServletRequest && !WebPathUtils.shouldPersistSavedRequest((HttpServletRequest) request))
            return;
        super.saveRequest(request);
    }

    private static boolean isApiOrAjaxRequest(HttpServletRequest request) {
        return HttpNavigationUtils.isApiOrAjaxRequest(request);
    }

    /**
     * Bearer / Robot Token 免表单登录；失败时不写入 SavedRequest。
     */
    private boolean tryAccessTokenLogin(ServletRequest request, ServletResponse response) throws Exception {
        String accessToken = getAccessToken(request);
        if (StringUtils.isEmpty(accessToken)) return false;
        if (accessToken.startsWith(RobotTokenService.TOKEN_PREFIX)) return executeLogin(request, response);
        if (null == clientDetailsService)
            clientDetailsService = (ClientDetailsService) Config.getBean("clientDetailsService");
        if (null != clientDetailsService && clientDetailsService.isValidAccessToken(accessToken))
            return executeLogin(request, response);
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
                log.debug("Unauthorized request: uri={}, IP={}, agent={}", request.getRequestURI(), IPUtils.getIp(request), request.getHeader("user-agent"));
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
            log.error("Unauthorized response:{}", e.getMessage());
            return false;
        }
    }

    protected boolean onLoginSuccess(AuthenticationToken token, Subject subject, ServletRequest request, ServletResponse response) throws Exception {
        if (token instanceof OauthAccessTokenToken || token instanceof RobotAccessTokenToken)
            return true;
        return super.onLoginSuccess(token, subject, request, response);
    }

    /**
     * 登录成功重定向前二次净化 SavedRequest，防止其它过滤器链误写入 REST/API 地址。
     */
    @Override
    protected void issueSuccessRedirect(ServletRequest request, ServletResponse response) throws Exception {
        if (!(request instanceof HttpServletRequest)) {
            super.issueSuccessRedirect(request, response);
            return;
        }
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        SavedRequest savedRequest = WebUtils.getSavedRequest(httpRequest);
        if (savedRequest == null || StringUtils.isBlank(savedRequest.getRequestUrl())) {
            super.issueSuccessRedirect(request, response);
            return;
        }
        String raw = savedRequest.getRequestUrl();
        String safe = WebPathUtils.safePostLoginRedirect(httpRequest, raw);
        if (raw.equals(safe)) {
            super.issueSuccessRedirect(request, response);
            return;
        }
        WebUtils.getAndClearSavedRequest(httpRequest);
        String target = resolvePostLoginRedirectTarget(httpRequest);
        WebUtils.issueRedirect(request, response, target, null, false, false);
    }

    private static String resolvePostLoginRedirectTarget(HttpServletRequest request) {
        if (null == superPositionModelService)
            superPositionModelService = (SuperPositionModelService) Config.getBean("superPositionModelService");
        AccountAuthConfig authConfig = null;
        try {
            SysConfigService sysConfigService = (SysConfigService) Config.getBean("sysConfigService");
            if (sysConfigService != null)
                authConfig = sysConfigService.getAccountAuthConfig();
        } catch (Exception ignored) {
        }
        return SysAuthSupport.resolvePostLoginRedirect(request, superPositionModelService, authConfig);
    }

    @Override
    protected boolean onLoginFailure(AuthenticationToken token, AuthenticationException e, ServletRequest request, ServletResponse response) {
        if (token instanceof OauthAccessTokenToken || token instanceof RobotAccessTokenToken) {
            throw e;
        }
        return super.onLoginFailure(token, e, request, response);
    }
}
