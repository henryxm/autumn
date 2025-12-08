package cn.org.autumn.handler;

import cn.org.autumn.exception.AException;
import cn.org.autumn.exception.CodeException;
import cn.org.autumn.utils.IPUtils;
import cn.org.autumn.utils.R;
import cn.org.autumn.utils.ExceptionUtils;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

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
    public R handleRRException(AException e, HttpServletRequest request) {
        if (log.isDebugEnabled())
            log.debug("AException:{}", new Gson().toJson(e));
        R r = new R();
        r.put("code", e.getCode());
        r.put("msg", e.getMessage());
        log.debug("AException at {} {}", request.getMethod(), request.getRequestURI(), e);
        return r;
    }

    @ExceptionHandler(CodeException.class)
    public R handleCodeException(CodeException e, HttpServletRequest request) {
        if (log.isDebugEnabled())
            log.debug("CodeException:{}", new Gson().toJson(e));
        R r = new R();
        r.put("code", e.getCode());
        r.put("msg", e.getMessage());
        log.debug("CodeException at {} {}", request.getMethod(), request.getRequestURI(), e);
        return r;
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public R handleDuplicateKeyException(DuplicateKeyException e, HttpServletRequest request) {
        if (log.isDebugEnabled())
            log.debug("DuplicateKeyException at {} {}", request.getMethod(), request.getRequestURI(), e);
        return R.error("数据库中已存在该记录");
    }

    /**
     * 处理媒体类型不可接受异常
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public R handleHttpMediaTypeNotAcceptableException(HttpMediaTypeNotAcceptableException e, HttpServletRequest request) {
        if (log.isDebugEnabled())
            log.debug("HttpMediaTypeNotAcceptableException at {} {} - Accept: {}, Content-Type: {}", request.getMethod(), request.getRequestURI(), request.getHeader("Accept"), request.getHeader("Content-Type"), e);
        return R.error(406, "请求的媒体类型不被支持");
    }

    /**
     * 处理HTTP请求方法不支持异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public R handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        if (log.isDebugEnabled())
            log.debug("HttpRequestMethodNotSupportedException at {} {} - Method: {}, Supported: {}", request.getMethod(), request.getRequestURI(), request.getMethod(), e.getSupportedMethods(), e);
        return R.error(405, "请求方法不支持: " + request.getMethod());
    }

    /**
     * 处理HTTP消息不可读异常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R handleHttpMessageNotReadableException(HttpMessageNotReadableException e, HttpServletRequest request) {
        if (log.isDebugEnabled())
            log.debug("HttpMessageNotReadableException at {} {} - Content-Type: {}",
                    request.getMethod(), request.getRequestURI(),
                    request.getHeader("Content-Type"), e);
        return R.error(400, "请求数据格式错误");
    }

    /**
     * 处理Servlet异常（包括循环视图路径异常）
     * 屏蔽循环视图路径异常的日志输出，但记录到监控中
     */
    @ExceptionHandler(ServletException.class)
    public R handleServletException(ServletException e, HttpServletRequest request) {
        String message = e.getMessage();
        // 判断是否是循环视图路径异常
        boolean isCircularViewPath = ExceptionUtils.isCircularViewPathException(e);
        if (isCircularViewPath) {
            // 循环视图路径异常：静默处理，不输出日志
            // 只在debug模式下记录基本信息用于调试
            if (log.isDebugEnabled()) {
                log.debug("检测到循环视图路径异常 - URI: {}, IP: {}, 已静默处理", request.getRequestURI(), IPUtils.getIp(request));
            }
            // 返回友好的错误信息，不暴露异常详情
            return R.error(400, "视图解析错误");
        } else {
            // 其他Servlet异常：正常记录
            if (log.isDebugEnabled()) {
                log.debug("ServletException at {} {} - Message: {}", request.getMethod(), request.getRequestURI(), message, e);
            }
            return R.error(500, "服务器内部错误");
        }
    }

    @ExceptionHandler(Exception.class)
    public R handleException(Exception e, HttpServletRequest request) {
        if (log.isDebugEnabled() && null != e && null != request)
            log.error("访问IP:{}, 错误:{}", IPUtils.getIp(request), e.getMessage());
        // 使用工具类记录详细的异常信息
        ExceptionUtils.logDetailedException("未处理的异常", e, request);
        // 如果是常见的HTTP异常，返回更友好的错误信息
        if (ExceptionUtils.isCommonHttpException(e)) {
            if (log.isDebugEnabled())
                log.debug("常见HTTP异常: {}", ExceptionUtils.formatExceptionForLog(e, request));
            return R.error("格式错误");
        }
        // 返回通用错误信息，避免暴露敏感信息
        return R.error("访问错误");
    }
}
