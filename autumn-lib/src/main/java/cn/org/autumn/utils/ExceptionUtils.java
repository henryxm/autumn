package cn.org.autumn.utils;

import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 异常工具类
 * 用于帮助调试和定位异常问题
 */
@Slf4j
public class ExceptionUtils {
    private static final long BENIGN_LOG_WINDOW_MS = 60_000L;
    private static final int BENIGN_LOG_KEY_MAX_SIZE = 10_000;
    private static final ConcurrentHashMap<String, BenignLogCounter> BENIGN_LOG_COUNTERS = new ConcurrentHashMap<>();

    private static class BenignLogCounter {
        private volatile long windowStartMs;
        private final AtomicInteger count = new AtomicInteger(0);
    }

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
     * 判断是否为客户端中途断开连接导致的异常。
     * 这类异常通常不是服务端业务错误，不应继续尝试写错误响应。
     */
    public static boolean isClientDisconnectException(Throwable t) {
        for (Throwable x = t; x != null; x = x.getCause()) {
            String message = x.getMessage();
            if (message != null) {
                if (message.contains("Broken pipe") ||
                        message.contains("Connection reset by peer") ||
                        message.contains("An established connection was aborted by the software in your host machine") ||
                        message.contains("connection abort") ||
                        message.contains("Connection aborted")) {
                    return true;
                }
            }
            String className = x.getClass().getName();
            if (className.contains("AsyncRequestNotUsableException") ||
                    className.contains("ClientAbortException") ||
                    className.contains("AbortedException") ||
                    className.contains("EofException")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否为异步请求生命周期中的“预期内”异常（超时/已完成后写入等）。
     * 这些异常在高并发、长连接、流式响应场景下常见，通常不代表业务逻辑故障。
     */
    public static boolean isBenignAsyncLifecycleException(Throwable t) {
        for (Throwable x = t; x != null; x = x.getCause()) {
            String className = x.getClass().getName();
            if (className.contains("AsyncRequestTimeoutException") ||
                    className.contains("AsyncRequestNotUsableException")) {
                return true;
            }
            String message = x.getMessage();
            if (message != null) {
                if (message.contains("ResponseBodyEmitter has already completed") ||
                        message.contains("The response object has been recycled and is no longer associated with this facade")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 对“预期内异常”（例如客户端断开连接）做限频日志，避免日志刷屏影响问题判断。
     * 维度：category + requestUri；窗口：1分钟。
     */
    public static boolean shouldLogBenignException(String category, HttpServletRequest request) {
        return nextBenignExceptionLogHint(category, request) != null;
    }

    /**
     * 返回预期内异常的日志提示：
     * - 返回 null：当前应抑制日志；
     * - 返回空串：当前输出一条常规日志；
     * - 返回非空串：当前输出一条日志，并携带上一窗口抑制计数提示。
     */
    public static String nextBenignExceptionLogHint(String category, HttpServletRequest request) {
        String uri = request != null ? request.getRequestURI() : "unknown";
        String key = category + "|" + uri;
        if (BENIGN_LOG_COUNTERS.size() > BENIGN_LOG_KEY_MAX_SIZE) {
            BENIGN_LOG_COUNTERS.clear();
        }
        BenignLogCounter counter = BENIGN_LOG_COUNTERS.computeIfAbsent(key, k -> new BenignLogCounter());
        long now = System.currentTimeMillis();
        synchronized (counter) {
            if (counter.windowStartMs == 0L) {
                counter.windowStartMs = now;
                counter.count.set(1);
                return "";
            }
            if (now - counter.windowStartMs >= BENIGN_LOG_WINDOW_MS) {
                int previousCount = counter.count.get();
                counter.windowStartMs = now;
                counter.count.set(1);
                int suppressed = Math.max(0, previousCount - 1);
                if (suppressed > 0) {
                    return "（过去60秒同类日志已抑制 " + suppressed + " 次）";
                }
                return "";
            }
            int current = counter.count.incrementAndGet();
            if (current == 1) {
                return "";
            }
            return null;
        }
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