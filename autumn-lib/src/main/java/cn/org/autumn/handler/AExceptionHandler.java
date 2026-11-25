package cn.org.autumn.handler;

import cn.org.autumn.exception.AException;
import cn.org.autumn.exception.CodeException;
import cn.org.autumn.model.Error;
import cn.org.autumn.model.Response;
import cn.org.autumn.utils.IPUtils;
import cn.org.autumn.utils.ExceptionUtils;
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

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;

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
            log.debug("请求方式:{}, 路径:{}, 代码:{}, 错误:{}", request.getMethod(), request.getRequestURI(), e.getCode(), e.getMessage(), e);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Response.error(e));
    }

    @ExceptionHandler(CodeException.class)
    public ResponseEntity<Response<?>> handleCodeException(CodeException e, HttpServletRequest request, HttpServletResponse response) {
        forceJsonContentType(request, response);
        if (log.isDebugEnabled())
            log.debug("请求方式:{}, 路径:{}, 代码:{}, 错误:{}", request.getMethod(), request.getRequestURI(), e.getCode(), e.getMessage(), e);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Response.error(e));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Response<?>> handleDuplicateKeyException(DuplicateKeyException e, HttpServletRequest request, HttpServletResponse response) {
        forceJsonContentType(request, response);
        if (log.isDebugEnabled())
            log.debug("请求方式:{}, 路径:{}, 错误:{}", request.getMethod(), request.getRequestURI(), e.getMessage(), e);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Response.error(Error.DATABASE_DUPLICATE_KEY));
    }

    /**
     * 处理媒体类型不可接受异常
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Response<?>> handleHttpMediaTypeNotAcceptableException(HttpMediaTypeNotAcceptableException e, HttpServletRequest request, HttpServletResponse response) {
        forceJsonContentType(request, response);
        if (log.isDebugEnabled())
            log.debug("请求方式:{}, 路径:{}, Accept:{}, Content-Type:{}, 错误:{}", request.getMethod(), request.getRequestURI(), request.getHeader("Accept"), request.getHeader("Content-Type"), e.getMessage(), e);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Response.error(Error.NOT_ACCEPTABLE));
    }

    /**
     * 处理HTTP请求方法不支持异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Response<?>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e, HttpServletRequest request, HttpServletResponse response) {
        forceJsonContentType(request, response);
        if (log.isDebugEnabled())
            log.debug("请求方式:{}, 路径:{}, 方法:{}, 支持的方法:{}, 错误:{}", request.getMethod(), request.getRequestURI(), request.getMethod(), e.getSupportedMethods(), e.getMessage(), e);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Response.error(Error.METHOD_NOT_ALLOWED));
    }

    /**
     * 处理HTTP消息不可读异常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Response<?>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e, HttpServletRequest request, HttpServletResponse response) {
        forceJsonContentType(request, response);
        if (log.isDebugEnabled())
            log.debug("请求方式:{}, 路径:{}, Content-Type:{}, 错误:{}", request.getMethod(), request.getRequestURI(), request.getHeader("Content-Type"), e.getMessage(), e);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Response.error(Error.BAD_REQUEST));
    }

    /**
     * 处理 Servlet 异常。
     * <p>
     * 循环视图路径异常仅面向 REST/API（{@code @RestControllerAdvice} 不处理页面渲染链）；
     * 页面请求由 {@link FreemarkerViewExceptionResolver} 负责，此处返回 JSON {@link Error#NOT_FOUND} 且不记 ERROR 日志。
     * <p>
     * DispatcherServlet 常把业务异常包装为 {@code NestedServletException}；若一律 500 会掩盖
     * Shiro 无权限、{@link CodeException} 等，故先解包 cause 再映射。
     */
    @ExceptionHandler(ServletException.class)
    public ResponseEntity<Response<?>> handleServletException(ServletException e, HttpServletRequest request, HttpServletResponse response) {
        forceJsonContentType(request, response);
        boolean isCircularViewPath = ExceptionUtils.isCircularViewPathException(e);
        if (isCircularViewPath) {
            if (log.isDebugEnabled()) {
                log.debug("请求方式:{}, 路径:{}, IP:{}, 错误:循环视图路径异常，已静默处理", request.getMethod(), request.getRequestURI(), IPUtils.getIp(request));
            }
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Response.error(Error.NOT_FOUND));
        }
        ResponseEntity<Response<?>> unwrapped = mapUnwrappedServletCause(e, request, response);
        if (unwrapped != null) {
            return unwrapped;
        }
        if (log.isDebugEnabled()) {
            log.debug("请求方式:{}, 路径:{}, 错误:{}", request.getMethod(), request.getRequestURI(), e.getMessage(), e);
        }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Response.error(Error.INTERNAL_SERVER_ERROR));
    }

    /**
     * 解包 NestedServletException 等包装链：CodeException / AException / Shiro 鉴权失败。
     * 不直接依赖 shiro 类型（autumn-lib 无 shiro），按类名识别。
     */
    private ResponseEntity<Response<?>> mapUnwrappedServletCause(Throwable e, HttpServletRequest request, HttpServletResponse response) {
        Throwable cur = e;
        while (cur != null) {
            if (cur instanceof CodeException) {
                return handleCodeException((CodeException) cur, request, response);
            }
            if (cur instanceof AException) {
                return handleRRException((AException) cur, request, response);
            }
            String name = cur.getClass().getName();
            if ("org.apache.shiro.authz.UnauthenticatedException".equals(name)) {
                if (log.isDebugEnabled()) {
                    log.debug("请求方式:{}, 路径:{}, 错误:未登录", request.getMethod(), request.getRequestURI());
                }
                return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Response.error(Error.UNAUTHORIZED));
            }
            if ("org.apache.shiro.authz.UnauthorizedException".equals(name)
                    || "org.apache.shiro.authz.AuthorizationException".equals(name)
                    || (name.startsWith("org.apache.shiro.authz.") && name.endsWith("AuthorizationException"))) {
                if (log.isDebugEnabled()) {
                    log.debug("请求方式:{}, 路径:{}, 错误:无权限", request.getMethod(), request.getRequestURI());
                }
                return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Response.error(Error.FORBIDDEN));
            }
            Throwable next = cur.getCause();
            if (next == cur) {
                break;
            }
            cur = next;
        }
        return null;
    }

    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response<?>> handleException(Exception e, HttpServletRequest request, HttpServletResponse response) {
        if (response != null && response.isCommitted()) {
            String hint = ExceptionUtils.nextBenignExceptionLogHint("response-committed", request);
            if (request != null && hint != null) {
                log.debug("响应已提交后出现后续异常，已忽略{}。请求方式:{}, 路径:{}, IP:{}, 错误:{}", hint, request.getMethod(), request.getRequestURI(), IPUtils.getIp(request), e.getMessage());
            }
            return null;
        }
        if (ExceptionUtils.isClientDisconnectException(e)) {
            String hint = ExceptionUtils.nextBenignExceptionLogHint("client-disconnect", request);
            if (request != null && hint != null) {
                log.debug("客户端连接中断，已忽略{}。请求方式:{}, 路径:{}, IP:{}, 错误:{}", hint, request.getMethod(), request.getRequestURI(), IPUtils.getIp(request), e.getMessage());
            }
            return null;
        }
        if (ExceptionUtils.isBenignAsyncLifecycleException(e)) {
            String hint = ExceptionUtils.nextBenignExceptionLogHint("async-lifecycle", request);
            if (request != null && hint != null) {
                log.debug("异步请求生命周期边界异常，已忽略{}。请求方式:{}, 路径:{}, IP:{}, 错误:{}", hint, request.getMethod(), request.getRequestURI(), IPUtils.getIp(request), e.getMessage());
            }
            return null;
        }
        AException aException = ExceptionUtils.findAException(e);
        if (aException != null) {
            return handleRRException(aException, request, response);
        }
        forceJsonContentType(request, response);
        if (log.isDebugEnabled() && null != e && null != request) {
            log.debug("请求方式:{}, 路径:{}, IP:{}, 错误:{}", request.getMethod(), request.getRequestURI(), IPUtils.getIp(request), e.getMessage(), e);
        }
        // 使用工具类记录详细的异常信息
        ExceptionUtils.logDetailedException("未处理的异常", e, request);
        // 如果是常见的HTTP异常，返回更友好的错误信息
        if (ExceptionUtils.isCommonHttpException(e)) {
            if (log.isDebugEnabled()) {
                log.debug("常见HTTP异常: {}", ExceptionUtils.formatExceptionForLog(e, request));
            }
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Response.error(Error.BAD_REQUEST));
        }
        if (e instanceof BadSqlGrammarException) {
            log.error("请求失败:{}", e.getMessage());
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
