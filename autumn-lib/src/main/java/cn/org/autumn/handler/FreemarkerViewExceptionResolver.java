package cn.org.autumn.handler;

import cn.org.autumn.config.ApplicationInitializationProgress;
import cn.org.autumn.model.Error;
import cn.org.autumn.model.Response;
import com.alibaba.fastjson.JSON;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;

/**
 * 统一处理 FreeMarker 视图渲染异常，避免将堆栈直接暴露到浏览器。
 * <p>
 * 在 {@link ApplicationInitializationProgress} 仍处于启动阶段（未完成 ApplicationRunner 初始化）时，
 * 不因重试次数用尽而落到 500，持续引导至 loading 页，避免启动窗口期出现裸 FM 错误页。
 */
@Slf4j
@Component
public class FreemarkerViewExceptionResolver extends AbstractHandlerExceptionResolver {

    @Autowired(required = false)
    private ApplicationInitializationProgress applicationInitializationProgress;

    public FreemarkerViewExceptionResolver() {
        setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
    }

    @Override
    protected ModelAndView doResolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Throwable freemarkerThrowable = extractFreemarkerThrowable(ex);
        if (freemarkerThrowable == null) {
            return null;
        }
        if (response.isCommitted()) {
            return new ModelAndView();
        }
        logFreemarkerException(request, ex, freemarkerThrowable);
        if (isApiRequest(request)) {
            writeApiError(response);
            return new ModelAndView();
        }
        String requestUri = request.getRequestURI();
        if (isLoadingPageRequest(requestUri)) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return new ModelAndView("500");
        }
        int retryCount = getRetryCount(request);
        boolean bootstrapping = isApplicationBootstrapping();
        if (!bootstrapping && retryCount >= 3) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return new ModelAndView("500");
        }
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        return new ModelAndView("redirect:" + buildLoadingRedirectTarget(request, retryCount + 1));
    }

    private boolean isLoadingPageRequest(String requestUri) {
        if (StringUtils.isBlank(requestUri)) {
            return false;
        }
        int q = requestUri.indexOf('?');
        String path = q >= 0 ? requestUri.substring(0, q) : requestUri;
        return StringUtils.endsWithIgnoreCase(path, "/loading.html");
    }

    /**
     * 与 {@link cn.org.autumn.config.PostApplicationRunner} 及多语言缓存就绪对齐：
     * Init/Load/Upgrade/Refresh 完成前，或 {@link ApplicationInitializationProgress#isLanguageCacheReady()} 仍为 false 时，
     * 页面模型（如 {@code lang}）可能不完整，Freemarker 易报错，此时持续引导 loading 而非满次数后 500。
     */
    private boolean isApplicationBootstrapping() {
        if (applicationInitializationProgress == null) {
            return false;
        }
        ApplicationInitializationProgress.Phase p = applicationInitializationProgress.getPhase();
        if (p == ApplicationInitializationProgress.Phase.WIZARD || p == ApplicationInitializationProgress.Phase.FAILED) {
            return false;
        }
        switch (p) {
            case IDLE:
            case INIT:
            case LOAD:
            case UPGRADE:
            case REFRESH:
                return true;
            case DONE:
                return !applicationInitializationProgress.isLanguageCacheReady();
            default:
                return false;
        }
    }

    private Throwable extractFreemarkerThrowable(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        Throwable current = throwable;
        while (current != null && current.getCause() != current) {
            if (isFreemarkerRelated(current)) {
                return current;
            }
            current = current.getCause();
        }
        return isFreemarkerRelated(throwable) ? throwable : null;
    }

    private static boolean isFreemarkerRelated(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof TemplateException) {
            return true;
        }
        String className = t.getClass().getName();
        if (className.startsWith("freemarker.")) {
            return true;
        }
        String msg = StringUtils.defaultString(t.getMessage(), "");
        if (msg.contains("freemarker.")) {
            return true;
        }
        if (msg.contains("FreeMarker")) {
            return true;
        }
        return false;
    }

    private void logFreemarkerException(HttpServletRequest request, Exception ex, Throwable freemarkerThrowable) {
        String method = request != null ? request.getMethod() : "-";
        String uri = request != null ? request.getRequestURI() : "-";
        String query = request != null ? StringUtils.defaultString(request.getQueryString(), "") : "";
        String accept = request != null ? StringUtils.defaultString(request.getHeader("Accept"), "") : "";
        if (freemarkerThrowable instanceof TemplateException) {
            TemplateException te = (TemplateException) freemarkerThrowable;
            log.error("FreeMarker view render failed, method={}, uri={}{}{}, accept={}, template={}, line={}, column={}, blamedExpression={}, message={}", method, uri, StringUtils.isBlank(query) ? "" : "?", query, accept, StringUtils.defaultString(te.getTemplateSourceName(), "-"), te.getLineNumber(), te.getColumnNumber(), StringUtils.defaultString(te.getBlamedExpressionString(), "-"), StringUtils.defaultString(te.getMessage(), "-"), ex);
            return;
        }
        log.error("FreeMarker resource render failed, method={}, uri={}{}{}, accept={}, exceptionType={}, message={}", method, uri, StringUtils.isBlank(query) ? "" : "?", query, accept, freemarkerThrowable.getClass().getName(), StringUtils.defaultString(freemarkerThrowable.getMessage(), "-"), ex);
    }

    private boolean isApiRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String accept = request.getHeader("Accept");
        if (StringUtils.isNotBlank(accept) && accept.contains("application/json")) {
            return true;
        }
        String contentType = request.getContentType();
        if (StringUtils.isNotBlank(contentType) && contentType.contains("application/json")) {
            return true;
        }
        String requestURI = request.getRequestURI();
        if (StringUtils.isNotBlank(requestURI) && (requestURI.contains("/api/") || requestURI.endsWith(".json"))) {
            return true;
        }
        String requestedWith = request.getHeader("X-Requested-With");
        return StringUtils.isNotBlank(requestedWith) && "XMLHttpRequest".equalsIgnoreCase(requestedWith);
    }

    /**
     * {@code _fmRetry} 放在 loading 外层查询串，由 loading 页在跳转回目标 URL 时再附带，
     * 避免仅依赖 target 内嵌参数时因 query 解析/丢失导致重试计数失效或误跳首页。
     */
    private String buildLoadingRedirectTarget(HttpServletRequest request, int nextRetryCount) {
        String path = "/";
        if (request != null) {
            path = StringUtils.defaultIfEmpty(request.getRequestURI(), "/");
        }
        String query = request != null ? request.getQueryString() : null;
        query = removeParam(query, "_fmRetry");
        String target = path;
        if (StringUtils.isNotBlank(query)) {
            target = path + "?" + query;
        }
        try {
            return "/loading.html?_fmRetry=" + nextRetryCount + "&target=" + URLEncoder.encode(target, "UTF-8");
        } catch (Exception ignored) {
            return "/loading.html?_fmRetry=" + nextRetryCount;
        }
    }

    private int getRetryCount(HttpServletRequest request) {
        if (request == null) {
            return 0;
        }
        String retry = request.getParameter("_fmRetry");
        if (StringUtils.isBlank(retry)) {
            return 0;
        }
        try {
            int n = Integer.parseInt(retry);
            return n < 0 ? 0 : n;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String removeParam(String query, String key) {
        if (StringUtils.isBlank(query) || StringUtils.isBlank(key)) {
            return query;
        }
        String[] pairs = query.split("&");
        StringBuilder builder = new StringBuilder();
        for (String pair : pairs) {
            if (StringUtils.isBlank(pair)) {
                continue;
            }
            int index = pair.indexOf('=');
            String name = index >= 0 ? pair.substring(0, index) : pair;
            if (key.equals(name)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("&");
            }
            builder.append(pair);
        }
        return builder.toString();
    }

    private void writeApiError(HttpServletResponse response) {
        if (response == null) {
            return;
        }
        try {
            if (!response.isCommitted()) {
                response.resetBuffer();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setCharacterEncoding("UTF-8");
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(JSON.toJSONString(Response.error(Error.INTERNAL_SERVER_ERROR)));
                response.getWriter().flush();
            }
        } catch (IOException ioException) {
            log.error("FreeMarker API error response write failed: {}", ioException.getMessage(), ioException);
        }
    }
}