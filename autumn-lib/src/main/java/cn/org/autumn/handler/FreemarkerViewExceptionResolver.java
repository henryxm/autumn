package cn.org.autumn.handler;

import cn.org.autumn.model.Error;
import cn.org.autumn.model.Response;
import com.alibaba.fastjson.JSON;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
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
 */
@Slf4j
@Component
public class FreemarkerViewExceptionResolver extends AbstractHandlerExceptionResolver {

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
        if (StringUtils.endsWithIgnoreCase(requestUri, "/loading.html")) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return new ModelAndView("500");
        }

        int retryCount = getRetryCount(request);
        if (retryCount >= 3) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return new ModelAndView("500");
        }

        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        return new ModelAndView("redirect:" + buildLoadingRedirectTarget(request, retryCount + 1));
    }

    private Throwable extractFreemarkerThrowable(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != current) {
            if (current instanceof TemplateException) {
                return current;
            }
            String className = current.getClass().getName();
            if (className.startsWith("freemarker.")) {
                return current;
            }
            current = current.getCause();
        }
        return null;
    }

    private void logFreemarkerException(HttpServletRequest request, Exception ex, Throwable freemarkerThrowable) {
        String method = request != null ? request.getMethod() : "-";
        String uri = request != null ? request.getRequestURI() : "-";
        String query = request != null ? StringUtils.defaultString(request.getQueryString(), "") : "";
        String accept = request != null ? StringUtils.defaultString(request.getHeader("Accept"), "") : "";

        if (freemarkerThrowable instanceof TemplateException) {
            TemplateException te = (TemplateException) freemarkerThrowable;
            log.error(
                    "FreeMarker view render failed, method={}, uri={}{}{}, accept={}, template={}, line={}, column={}, blamedExpression={}, message={}",
                    method,
                    uri,
                    StringUtils.isBlank(query) ? "" : "?",
                    query,
                    accept,
                    StringUtils.defaultString(te.getTemplateSourceName(), "-"),
                    te.getLineNumber(),
                    te.getColumnNumber(),
                    StringUtils.defaultString(te.getBlamedExpressionString(), "-"),
                    StringUtils.defaultString(te.getMessage(), "-"),
                    ex
            );
            return;
        }
        log.error(
                "FreeMarker resource render failed, method={}, uri={}{}{}, accept={}, exceptionType={}, message={}",
                method,
                uri,
                StringUtils.isBlank(query) ? "" : "?",
                query,
                accept,
                freemarkerThrowable.getClass().getName(),
                StringUtils.defaultString(freemarkerThrowable.getMessage(), "-"),
                ex
        );
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

    private String buildLoadingRedirectTarget(HttpServletRequest request, int nextRetryCount) {
        String target = "/";
        if (request != null) {
            target = StringUtils.defaultIfEmpty(request.getRequestURI(), "/");
            String query = request.getQueryString();
            query = removeParam(query, "_fmRetry");
            if (StringUtils.isNotBlank(query)) {
                target = target + "?" + query;
            }
            target = target + (target.contains("?") ? "&" : "?") + "_fmRetry=" + nextRetryCount;
        }
        try {
            return "/loading.html?target=" + URLEncoder.encode(target, "UTF-8");
        } catch (Exception ignored) {
            return "/loading.html";
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
            return Integer.parseInt(retry);
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
