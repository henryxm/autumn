package cn.org.autumn.utils;

import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 异常工具类
 * 用于帮助调试和定位异常问题
 */
@Slf4j
public class ExceptionUtils {

    /**
     * 获取异常的完整堆栈跟踪信息
     */
    public static String getStackTrace(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * 获取请求的详细信息
     */
    public static Map<String, Object> getRequestInfo(HttpServletRequest request) {
        Map<String, Object> info = new HashMap<>();
        if (request == null) {
            return info;
        }

        info.put("method", request.getMethod());
        info.put("uri", request.getRequestURI());
        info.put("url", request.getRequestURL().toString());
        info.put("queryString", request.getQueryString());
        info.put("remoteAddr", request.getRemoteAddr());
        info.put("remoteHost", request.getRemoteHost());
        info.put("remotePort", request.getRemotePort());
        info.put("userAgent", request.getHeader("User-Agent"));
        info.put("accept", request.getHeader("Accept"));
        info.put("contentType", request.getHeader("Content-Type"));
        info.put("contentLength", request.getContentLength());

        // 获取所有请求头
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            headers.put(headerName, headerValue);
        }
        info.put("headers", headers);

        return info;
    }

    /**
     * 记录详细的异常信息
     */
    public static void logDetailedException(String message, Exception e, HttpServletRequest request) {
        if (log.isDebugEnabled()) {
            log.error("=== 异常详细信息 ===");
            log.error("消息: {}", message);
            log.error("异常类型: {}", e.getClass().getName());
            log.error("异常消息: {}", e.getMessage());

            if (request != null) {
                Map<String, Object> requestInfo = getRequestInfo(request);
                log.error("请求信息: {}", requestInfo);
            }

            log.error("堆栈跟踪:");
            log.error(getStackTrace(e));
            log.error("=== 异常详细信息结束 ===");
        }
    }

    /**
     * 判断是否为常见的HTTP异常
     */
    public static boolean isCommonHttpException(Exception e) {
        return e instanceof org.springframework.web.HttpMediaTypeNotAcceptableException ||
                e instanceof org.springframework.web.HttpRequestMethodNotSupportedException ||
                e instanceof org.springframework.http.converter.HttpMessageNotReadableException ||
                e instanceof org.springframework.web.HttpMediaTypeNotSupportedException ||
                e instanceof org.springframework.web.bind.MissingServletRequestParameterException;
    }

    /**
     * 判断是否为循环视图路径异常
     */
    public static boolean isCircularViewPathException(Exception e) {
        if (e == null) {
            return false;
        }
        String message = e.getMessage();
        return message != null && message.contains("Circular view path");
    }

    /**
     * 获取异常的根本原因
     */
    public static Throwable getRootCause(Throwable throwable) {
        if (throwable == null) {
            return null;
        }

        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }

    /**
     * 格式化异常信息用于日志输出
     */
    public static String formatExceptionForLog(Exception e, HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("异常类型: ").append(e.getClass().getSimpleName()).append("\n");
        sb.append("异常消息: ").append(e.getMessage()).append("\n");

        if (request != null) {
            sb.append("请求方法: ").append(request.getMethod()).append("\n");
            sb.append("请求URI: ").append(request.getRequestURI()).append("\n");
            sb.append("客户端IP: ").append(request.getRemoteAddr()).append("\n");
            sb.append("User-Agent: ").append(request.getHeader("User-Agent")).append("\n");
        }

        return sb.toString();
    }
} 