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
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
    public Response<?> handleRRException(AException e, HttpServletRequest request, HttpServletResponse response) {
        ensureJsonContentType(response);
        if (log.isDebugEnabled())
            log.debug("请求方式:{}, 路径:{}, 代码:{}, 错误:{}", request.getMethod(), request.getRequestURI(), e.getCode(), e.getMessage(), e);
        return Response.error(e);
    }

    @ExceptionHandler(CodeException.class)
    public Response<?> handleCodeException(CodeException e, HttpServletRequest request, HttpServletResponse response) {
        ensureJsonContentType(response);
        if (log.isDebugEnabled())
            log.debug("请求方式:{}, 路径:{}, 代码:{}, 错误:{}", request.getMethod(), request.getRequestURI(), e.getCode(), e.getMessage(), e);
        return Response.error(e);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public Response<?> handleDuplicateKeyException(DuplicateKeyException e, HttpServletRequest request, HttpServletResponse response) {
        ensureJsonContentType(response);
        if (log.isDebugEnabled())
            log.debug("请求方式:{}, 路径:{}, 错误:{}", request.getMethod(), request.getRequestURI(), e.getMessage(), e);
        return Response.error(Error.DATABASE_DUPLICATE_KEY);
    }

    /**
     * 处理媒体类型不可接受异常
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public Response<?> handleHttpMediaTypeNotAcceptableException(HttpMediaTypeNotAcceptableException e, HttpServletRequest request, HttpServletResponse response) {
        ensureJsonContentType(response);
        if (log.isDebugEnabled())
            log.debug("请求方式:{}, 路径:{}, Accept:{}, Content-Type:{}, 错误:{}", request.getMethod(), request.getRequestURI(), request.getHeader("Accept"), request.getHeader("Content-Type"), e.getMessage(), e);
        return Response.error(Error.NOT_ACCEPTABLE);
    }

    /**
     * 处理HTTP请求方法不支持异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Response<?> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e, HttpServletRequest request, HttpServletResponse response) {
        ensureJsonContentType(response);
        if (log.isDebugEnabled())
            log.debug("请求方式:{}, 路径:{}, 方法:{}, 支持的方法:{}, 错误:{}", request.getMethod(), request.getRequestURI(), request.getMethod(), e.getSupportedMethods(), e.getMessage(), e);
        return Response.error(Error.METHOD_NOT_ALLOWED);
    }

    /**
     * 处理HTTP消息不可读异常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Response<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException e, HttpServletRequest request, HttpServletResponse response) {
        ensureJsonContentType(response);
        if (log.isDebugEnabled())
            log.debug("请求方式:{}, 路径:{}, Content-Type:{}, 错误:{}", request.getMethod(), request.getRequestURI(), request.getHeader("Content-Type"), e.getMessage(), e);
        return Response.error(Error.BAD_REQUEST);
    }

    /**
     * 处理Servlet异常（包括循环视图路径异常）
     * 屏蔽循环视图路径异常的日志输出，但记录到监控中
     */
    @ExceptionHandler(ServletException.class)
    public Response<?> handleServletException(ServletException e, HttpServletRequest request, HttpServletResponse response) {
        ensureJsonContentType(response);
        // 判断是否是循环视图路径异常
        boolean isCircularViewPath = ExceptionUtils.isCircularViewPathException(e);
        if (isCircularViewPath) {
            // 循环视图路径异常：静默处理，不输出日志
            // 只在debug模式下记录基本信息用于调试
            if (log.isDebugEnabled()) {
                log.debug("请求方式:{}, 路径:{}, IP:{}, 错误:循环视图路径异常，已静默处理", request.getMethod(), request.getRequestURI(), IPUtils.getIp(request));
            }
            // 返回友好的错误信息，不暴露异常详情
            return Response.error(Error.BAD_REQUEST);
        } else {
            // 其他Servlet异常：正常记录
            if (log.isDebugEnabled()) {
                log.debug("请求方式:{}, 路径:{}, 错误:{}", request.getMethod(), request.getRequestURI(), e.getMessage(), e);
            }
            return Response.error(Error.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public Response<?> handleException(Exception e, HttpServletRequest request, HttpServletResponse response) {
        ensureJsonContentType(response);
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
            return Response.error(Error.BAD_REQUEST);
        }
        // 返回通用错误信息，避免暴露敏感信息
        return Response.error(Error.UNKNOWN_ERROR);
    }

    /**
     * 统一异常响应类型，避免上游预设 Content-Type 导致消息转换失败。
     */
    private void ensureJsonContentType(HttpServletResponse response) {
        if (response != null) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        }
    }
}
