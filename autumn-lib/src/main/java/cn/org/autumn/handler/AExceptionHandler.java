package cn.org.autumn.handler;

import cn.org.autumn.exception.AException;
import cn.org.autumn.exception.CodeException;
import cn.org.autumn.model.Error;
import cn.org.autumn.model.Response;
import cn.org.autumn.utils.ExceptionUtils;
import cn.org.autumn.utils.IPUtils;
import java.util.Collections;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.HandlerMapping;

/**
 * 异常处理器
 */
@Slf4j
@RestControllerAdvice
public class AExceptionHandler {
    /**
     * 处理自定义异常
     */
    @ExceptionHandler(AException.class)
    public ResponseEntity<Response<?>> handleRRException(AException e, HttpServletRequest request, HttpServletResponse response) {
        forceJsonContentType(request, response);
        if (log.isDebugEnabled())
            log.debug("request method: {}, path: {}, code: {}, error: {}", request.getMethod(), request.getRequestURI(), e.getCode(), e.getMessage(), e);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Response.error(e));
    }

    @ExceptionHandler(CodeException.class)
    public ResponseEntity<Response<?>> handleCodeException(CodeException e, HttpServletRequest request, HttpServletResponse response) {
        forceJsonContentType(request, response);
        if (log.isDebugEnabled())
            log.debug("request method: {}, path: {}, code: {}, error: {}", request.getMethod(), request.getRequestURI(), e.getCode(), e.getMessage(), e);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Response.error(e));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Response<?>> handleDuplicateKeyException(DuplicateKeyException e, HttpServletRequest request, HttpServletResponse response) {
        forceJsonContentType(request, response);
        if (log.isDebugEnabled())
            log.debug("request method: {}, path: {}, error: {}", request.getMethod(), request.getRequestURI(), e.getMessage(), e);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Response.error(Error.DATABASE_DUPLICATE_KEY));
    }

    /**
     * 处理媒体类型不可接受异常
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Response<?>> handleHttpMediaTypeNotAcceptableException(HttpMediaTypeNotAcceptableException e, HttpServletRequest request, HttpServletResponse response) {
        forceJsonContentType(request, response);
        if (log.isDebugEnabled())
            log.debug("request method: {}, path: {}, Accept: {}, Content-Type: {}, error: {}", request.getMethod(), request.getRequestURI(), request.getHeader("Accept"), request.getHeader("Content-Type"), e.getMessage(), e);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Response.error(Error.NOT_ACCEPTABLE));
    }

    /**
     * 处理HTTP请求方法不支持异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Response<?>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e, HttpServletRequest request, HttpServletResponse response) {
        forceJsonContentType(request, response);
        if (log.isDebugEnabled())
            log.debug("request method: {}, path: {}, method: {}, supported methods: {}, error: {}", request.getMethod(), request.getRequestURI(), request.getMethod(), e.getSupportedMethods(), e.getMessage(), e);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Response.error(Error.METHOD_NOT_ALLOWED));
    }

    /**
     * 处理HTTP消息不可读异常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Response<?>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e, HttpServletRequest request, HttpServletResponse response) {
        forceJsonContentType(request, response);
        if (log.isDebugEnabled())
            log.debug("request method: {}, path: {}, Content-Type: {}, error: {}", request.getMethod(), request.getRequestURI(), request.getHeader("Content-Type"), e.getMessage(), e);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Response.error(Error.BAD_REQUEST));
    }

    /**
     * 处理 Servlet 异常。
     * <p>
     * 循环视图路径异常仅面向 REST/API（{@code @RestControllerAdvice} 不处理页面渲染链）；
     * 页面请求由 {@link FreemarkerViewExceptionResolver} 负责，此处返回 JSON {@link Error#NOT_FOUND} 且不记 ERROR 日志。
     */
    @ExceptionHandler(ServletException.class)
    public ResponseEntity<Response<?>> handleServletException(ServletException e, HttpServletRequest request, HttpServletResponse response) {
        forceJsonContentType(request, response);
        boolean isCircularViewPath = ExceptionUtils.isCircularViewPathException(e);
        if (isCircularViewPath) {
            if (log.isDebugEnabled()) {
                log.debug("request method: {}, path: {}, IP: {}, error: circular view path, silently handled", request.getMethod(), request.getRequestURI(), IPUtils.getIp(request));
            }
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Response.error(Error.NOT_FOUND));
        } else {
            if (log.isDebugEnabled()) {
                log.debug("request method: {}, path: {}, error: {}", request.getMethod(), request.getRequestURI(), e.getMessage(), e);
            }
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Response.error(Error.INTERNAL_SERVER_ERROR));
        }
    }

    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response<?>> handleException(Exception e, HttpServletRequest request, HttpServletResponse response) {
        if (response != null && response.isCommitted()) {
            String hint = ExceptionUtils.nextBenignExceptionLogHint("response-committed", request);
            if (request != null && hint != null) {
                log.debug("Post-commit exception ignored{}; request method: {}, path: {}, IP: {}, error: {}", hint, request.getMethod(), request.getRequestURI(), IPUtils.getIp(request), e.getMessage());
            }
            return null;
        }
        if (ExceptionUtils.isClientDisconnectException(e)) {
            String hint = ExceptionUtils.nextBenignExceptionLogHint("client-disconnect", request);
            if (request != null && hint != null) {
                log.debug("Client disconnect ignored{}; request method: {}, path: {}, IP: {}, error: {}", hint, request.getMethod(), request.getRequestURI(), IPUtils.getIp(request), e.getMessage());
            }
            return null;
        }
        if (ExceptionUtils.isBenignAsyncLifecycleException(e)) {
            String hint = ExceptionUtils.nextBenignExceptionLogHint("async-lifecycle", request);
            if (request != null && hint != null) {
                log.debug("Async request lifecycle boundary exception ignored{}; request method: {}, path: {}, IP: {}, error: {}", hint, request.getMethod(), request.getRequestURI(), IPUtils.getIp(request), e.getMessage());
            }
            return null;
        }
        AException aException = ExceptionUtils.findAException(e);
        if (aException != null) {
            return handleRRException(aException, request, response);
        }
        forceJsonContentType(request, response);
        if (log.isDebugEnabled() && null != e && null != request) {
            log.debug("request method: {}, path: {}, IP: {}, error: {}", request.getMethod(), request.getRequestURI(), IPUtils.getIp(request), e.getMessage(), e);
        }
        // 使用工具类记录详细的异常信息
        ExceptionUtils.logDetailedException("Unhandled exception", e, request);
        // 如果是常见的HTTP异常，返回更友好的错误信息
        if (ExceptionUtils.isCommonHttpException(e)) {
            if (log.isDebugEnabled()) {
                log.debug("Common HTTP exception: {}", ExceptionUtils.formatExceptionForLog(e, request));
            }
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Response.error(Error.BAD_REQUEST));
        }
        if (e instanceof BadSqlGrammarException) {
            log.error("Request failed: {}", e.getMessage());
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Response.error(Error.NOT_IMPLEMENTED));
        }
        // 返回通用错误信息，避免暴露敏感信息
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Response.error(Error.UNKNOWN_ERROR));
    }

    /**
     * 统一异常响应类型，避免上游预设 Content-Type 导致消息转换失败。
     */
    private void forceJsonContentType(HttpServletRequest request, HttpServletResponse response) {
        if (request != null) {
            request.setAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, Collections.singleton(MediaType.APPLICATION_JSON));
        }
        if (response != null) {
            if (!response.isCommitted()) {
                response.reset();
            }
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        }
    }
}
