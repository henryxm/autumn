package cn.org.autumn.service;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 通用透明反向代理控制器
 * <p>
 * 完全通用的透明反向代理，可以代理所有 HTTP/HTTPS 请求
 * 类似 nginx 反向代理，客户端传递目标 URL 和认证信息，服务端负责转发
 * 支持：
 * - 所有 HTTP 方法（GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS 等）
 * - 流式和非流式响应
 * - 完整的请求头转发
 * - 二进制数据（图片、文件等）
 * - 表单数据
 * - JSON/XML 数据
 * - CORS 跨域支持
 *
 * @author Autumn
 * &#064;date  2026-03
 */
@Slf4j
@Service
public class BaseHttpProxyService {

    public static final String proxy = "/http/proxy/v1";

    /**
     * 默认目标基础 URL（可以动态覆盖）
     */
    @Getter
    @Setter
    private static String base = "";

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public Object proxyAllRequests(String target, HttpServletRequest request, HttpServletResponse response) {
        // 直接从 InputStream 读取原始请求体，避免 Spring Converter 导致数据丢失
        byte[] requestBody = readRequestBody(request);
        // 构建完整的代理路径
        String proxyPath = buildProxyPath(request);
        if (log.isDebugEnabled()) {
            // 记录详细请求日志（生产环境可以调整日志级别）
            log.debug("===== 代理请求开始 =====");
            log.debug("方法:{}", request.getMethod());
            log.debug("路径:{}", proxyPath);
            log.debug("目标URL:{}", determineTargetUrl(target, request, proxyPath));
            log.debug("Content-Type: {}", request.getContentType());
            log.debug("Content-Length:{}, 实际读取字节数:{}", request.getContentLength(), requestBody.length);
            // 记录关键请求头（包括 Cookie、Authorization 等）
            log.debug("===== 请求头 =====");
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerValue = request.getHeader(headerName);
                // 对敏感信息进行脱敏
                if ("Cookie".equalsIgnoreCase(headerName) || "Authorization".equalsIgnoreCase(headerName)) {
                    log.debug("{}: {}", headerName, maskSensitiveValue(headerValue));
                } else {
                    log.debug("{}: {}", headerName, headerValue);
                }
            }

            // 将原始请求体写入文件，并在日志中仅输出 sessionId
            String sessionId = null;
            try {
                sessionId = request.getSession(true).getId();
            } catch (IllegalStateException e) {
                // 获取 session 失败时，使用随机 ID，避免影响主流程
                sessionId = UUID.randomUUID().toString();
            }

            if (requestBody.length > 0) {
                try {
                    String userHome = System.getProperty("user.home");
                    Path dir = Paths.get(userHome, "proxy");
                    if (!dir.toFile().exists())
                        Files.createDirectories(dir);
                    Path file = dir.resolve(sessionId);
                    Files.write(file, requestBody);
                    log.debug("请求体已写入文件，sessionId: {}", sessionId);
                } catch (Exception e) {
                    log.warn("写入请求体到文件失败，sessionId: {}", sessionId, e);
                }
            } else {
                log.debug("请求体为空，sessionId: {}", sessionId);
            }
        }

        try {
            // 确定目标 URL（优先级：参数 > Header > 默认）
            String finalTargetUrl = determineTargetUrl(target, request, proxyPath);
            if (StringUtils.isBlank(finalTargetUrl)) {
                return createErrorResponse(response, "缺少目标 URL", "missing_target_url", 400);
            }
            if (log.isDebugEnabled())
                log.debug("最终目标 URL: {}", finalTargetUrl);
            // 判断是否是流式请求
            boolean isStreaming = isStreamingRequest(requestBody, request);
            // 判断是否是二进制请求
            boolean isBinary = isBinaryRequest(request);
            Object result;
            if (isStreaming) {
                // 流式请求
                if (log.isDebugEnabled())
                    log.debug("检测到流式请求，使用 SSE 处理");
                result = handleStreamingRequest(finalTargetUrl, request, requestBody, response);
            } else if (isBinary) {
                // 二进制请求
                if (log.isDebugEnabled())
                    log.debug("检测到二进制请求，使用二进制处理");
                result = handleBinaryRequest(finalTargetUrl, request, requestBody, response);
            } else {
                // 普通请求
                if (log.isDebugEnabled())
                    log.debug("使用标准处理");
                result = handleStandardRequest(finalTargetUrl, request, requestBody, response);
            }
            if (log.isDebugEnabled())
                log.debug("===== 代理请求完成 =====");
            return result;
        } catch (Exception e) {
            log.error("===== 代理请求失败 =====", e);
            return createErrorResponse(response, "代理请求失败：" + e.getMessage(), "proxy_error", 500);
        }
    }

    /**
     * 脱敏敏感信息
     */
    private String maskSensitiveValue(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        // 只显示前 10 个字符和后 10 个字符
        if (value.length() > 20) {
            return value.substring(0, 10) + "..." + value.substring(value.length() - 10);
        }
        return "***";
    }

    /**
     * 处理标准请求（文本/JSON/XML 等）- 添加详细日志
     */
    private Object handleStandardRequest(String targetUrl, HttpServletRequest request, byte[] requestBody, HttpServletResponse response) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("===== 开始处理标准请求 =====");
            log.debug("目标 URL: {}", targetUrl);
        }

        HttpURLConnection connection = null;
        try {
            connection = createConnection(targetUrl, request, requestBody);
            if (log.isDebugEnabled())
                log.debug("连接已建立");

            // 获取响应码
            int statusCode = connection.getResponseCode();
            if (log.isDebugEnabled())
                log.debug("响应状态码：{}", statusCode);

            // 记录响应头
            if (log.isDebugEnabled()) {
                log.debug("===== 响应头 =====");
                Map<String, List<String>> headers = connection.getHeaderFields();
                headers.forEach((name, values) -> {
                    if (name != null && values != null) {
                        values.forEach(value -> log.debug("{}: {}", name, value));
                    }
                });
            }

            // 复制响应头
            copyResponseHeaders(connection, response);

            // 读取响应
            InputStream inputStream = (statusCode >= 400) ? connection.getErrorStream() : connection.getInputStream();
            byte[] responseData;
            if (inputStream != null) {
                responseData = readAllBytes(inputStream);
                if (log.isDebugEnabled())
                    log.debug("响应体长度：{} 字节", responseData.length);

                // 记录响应体（如果是 JSON，格式化输出）
                String responseStr = new String(responseData, StandardCharsets.UTF_8);
                if (log.isDebugEnabled()) {
                    if (responseStr.startsWith("{") || responseStr.startsWith("[")) {
                        log.debug("===== 响应体 (JSON) =====\n{}", responseStr);
                    } else {
                        log.debug("===== 响应体 =====\n{}", responseStr.length() > 1000 ? responseStr.substring(0, 1000) + "..." : responseStr);
                    }
                }

                return responseData;
            } else {
                if (log.isDebugEnabled())
                    log.debug("响应体：空");
                return new byte[0];
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 处理二进制请求（图片、文件等）
     */
    private Object handleBinaryRequest(String targetUrl, HttpServletRequest request, byte[] requestBody, HttpServletResponse response) throws Exception {
        if (log.isDebugEnabled())
            log.debug("处理二进制请求：{}", targetUrl);
        HttpURLConnection connection = null;
        try {
            connection = createConnection(targetUrl, request, requestBody);

            // 获取响应码
            int statusCode = connection.getResponseCode();
            if (log.isDebugEnabled())
                log.debug("响应状态码：{}", statusCode);

            response.setStatus(statusCode);

            // 复制响应头
            copyResponseHeaders(connection, response);

            // 设置内容类型
            String contentType = connection.getContentType();
            if (StringUtils.isNotBlank(contentType)) {
                response.setContentType(contentType);
            }

            // 读取响应
            InputStream inputStream = (statusCode >= 400) ? connection.getErrorStream() : connection.getInputStream();
            if (inputStream != null) {
                byte[] responseData = readAllBytes(inputStream);
                if (log.isDebugEnabled())
                    log.debug("响应体长度：{} 字节", responseData.length);
                return responseData;
            } else {
                if (log.isDebugEnabled())
                    log.debug("响应体：空");
                return new byte[0];
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 处理流式请求 - 添加详细日志
     */
    private SseEmitter handleStreamingRequest(String targetUrl, HttpServletRequest request, byte[] requestBody, HttpServletResponse response) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("===== 开始处理流式请求 =====");
            log.debug("目标 URL: {}", targetUrl);
        }

        // 设置 SSE 响应头
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");

        SseEmitter emitter = new SseEmitter(0L);

        executorService.execute(() -> {
            HttpURLConnection connection = null;
            int messageCount = 0;
            try {
                connection = createConnection(targetUrl, request, requestBody);
                if (log.isDebugEnabled())
                    log.debug("流式请求连接已建立");

                // 读取流式响应
                int statusCode = connection.getResponseCode();
                if (log.isDebugEnabled())
                    log.debug("流式响应状态码：{}", statusCode);

                if (statusCode >= 400) {
                    log.error("流式请求错误，状态码：{}", statusCode);
                    // 错误响应
                    try (InputStream errorStream = connection.getErrorStream()) {
                        if (errorStream != null) {
                            String errorBody = new String(readAllBytes(errorStream), StandardCharsets.UTF_8);
                            if (log.isDebugEnabled())
                                log.debug("错误响应体：{}", errorBody);
                            emitter.send(errorBody, MediaType.TEXT_PLAIN);
                        }
                    }
                    emitter.complete();
                    return;
                }

                // 逐行读取流式响应
                if (log.isDebugEnabled())
                    log.debug("开始读取流式响应数据");

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        messageCount++;

                        // 记录原始响应行
                        if (log.isDebugEnabled()) {
                            log.debug("[原始响应] 行 {}: {}", messageCount, line);
                        }

                        // 检查客户端是否断开连接
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            if (log.isDebugEnabled())
                                log.warn("流式传输被中断，客户端可能已断开连接");
                            break;
                        }

                        // 完全透明地转发每一行，包括换行符
                        // 目标服务器返回的格式已经是 SSE 标准格式（每行以 \n 结尾）
                        // readLine() 会去掉换行符
                        if (log.isDebugEnabled())
                            log.debug("[转发数据] 发送原始行：{}", line);
                        // 只转发非空行，空行会导致 SSE 解析错误
                        if (!line.trim().isEmpty()) {
                            //emitter.send时会自动附带data:前缀和换行符:\n
                            if (line.startsWith("data:"))
                                line = line.substring(5);
                            emitter.send(line, MediaType.TEXT_PLAIN);
                        }
                    }
                    if (log.isDebugEnabled())
                        log.debug("流式响应读取完成，总共 {} 行", messageCount);
                }

                // 注意：不再手动发送 [DONE]，因为目标服务器已经发送了
                if (log.isDebugEnabled())
                    log.debug("流式请求处理完成");
                emitter.complete();

            } catch (Exception e) {
                log.error("===== 流式处理失败 =====", e);
                try {
                    String errorJson = "{\"error\": {\"message\": \"" + e.getMessage() + "\"}}";
                    if (log.isDebugEnabled())
                        log.debug("流式处理失败:{}", e.getMessage());
                    emitter.send(errorJson, MediaType.TEXT_PLAIN);
                    emitter.completeWithError(e);
                } catch (IOException ex) {
                    log.error("发送错误消息失败", ex);
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                    if (log.isDebugEnabled())
                        log.debug("连接已关闭");
                }
            }
        });

        if (log.isDebugEnabled())
            log.debug("===== 流式请求初始化完成 =====");
        return emitter;
    }

    /**
     * 创建 HTTP 连接
     */
    private HttpURLConnection createConnection(String targetUrl, HttpServletRequest request, byte[] requestBody) throws Exception {
        URL url = new URL(targetUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        // 基本设置
        connection.setRequestMethod(request.getMethod());
        connection.setDoInput(true);
        connection.setDoOutput(requestBody != null && requestBody.length > 0);
        connection.setConnectTimeout(60000); // 60 秒连接超时
        connection.setReadTimeout(300000);   // 5 分钟读取超时
        connection.setInstanceFollowRedirects(false); // 不自动重定向
        // 复制请求头
        copyRequestHeaders(request, connection);
        // 发送请求体
        if (requestBody != null && requestBody.length > 0) {
            try (OutputStream os = connection.getOutputStream()) {
                os.write(requestBody);
                os.flush();
            }
        }
        return connection;
    }

    /**
     * 复制请求头 - 完全透明模式
     * <p>
     * 为了完全透明，只过滤掉绝对不能转发的头，其他所有头都原样转发
     * 包括：Cookie, Authorization, User-Agent, Referer, Origin 等
     * <p>
     * 特殊处理：
     * - Referer: 修改为目标服务器的域名，让目标服务器认为请求直接来自客户端
     * - X-Forwarded-For: 传递客户端真实 IP
     * - X-Real-IP: 传递客户端真实 IP
     */
    private void copyRequestHeaders(HttpServletRequest request, HttpURLConnection connection) {
        String clientIp = getClientIp(request);
        String targetHost = extractHost(connection.getURL().getHost());

        // 复制所有请求头（完全透明）
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();

            // 跳过绝对不能转发的头
            if (shouldSkipHeader(headerName)) {
                log.debug("跳过请求头：{}", headerName);
                continue;
            }

            // 特殊处理 Referer - 修改为目标服务器域名
            if ("Referer".equalsIgnoreCase(headerName)) {
                String referer = request.getHeader(headerName);
                // 将 Referer 中的代理服务器地址替换为目标服务器地址
                String modifiedReferer = modifyReferer(referer, targetHost);
                connection.setRequestProperty(headerName, modifiedReferer);
                log.debug("修改 Referer: {} -> {}", referer, modifiedReferer);
                continue;
            }

            // 跳过代理特定的头，由我们重新设置
            if (headerName.toLowerCase().startsWith("x-forwarded-") || headerName.toLowerCase().startsWith("x-real-ip")) {
                continue;
            }

            String headerValue = request.getHeader(headerName);
            if (StringUtils.isNotBlank(headerValue)) {
                // 完全透明地转发所有头
                connection.setRequestProperty(headerName, headerValue);
                if (log.isTraceEnabled()) {
                    log.trace("转发请求头：{} = {}", headerName, headerValue);
                }
            }
        }

        // 添加客户端真实 IP 信息（标准代理头）
        connection.setRequestProperty("X-Forwarded-For", clientIp);
        connection.setRequestProperty("X-Real-IP", clientIp);
        connection.setRequestProperty("X-Forwarded-Proto", getProtocol(request));

        log.debug("添加代理头 - X-Forwarded-For: {}, X-Real-IP: {}, X-Forwarded-Proto: {}", clientIp, clientIp, getProtocol(request));
    }

    /**
     * 获取客户端真实 IP
     */
    private String getClientIp(HttpServletRequest request) {
        // 按优先级检查各种 IP 头
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 如果有多个 IP（逗号分隔），取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    /**
     * 修改 Referer，将代理服务器地址替换为目标服务器地址
     */
    private String modifyReferer(String originalReferer, String targetHost) {
        if (StringUtils.isBlank(originalReferer)) {
            return originalReferer;
        }

        try {
            URI uri = new URI(originalReferer);
            String scheme = uri.getScheme();
            @SuppressWarnings("unused")
            String host = uri.getHost();  // 保留变量，可能将来使用
            int port = uri.getPort();
            String path = uri.getPath();
            String query = uri.getQuery();

            // 如果是相对路径，直接返回
            if (scheme == null) {
                return originalReferer;
            }

            // 构建新的 Referer，使用目标服务器的主机
            StringBuilder newReferer = new StringBuilder();
            newReferer.append(scheme).append("://").append(targetHost);

            if (port > 0 &&
                    !((scheme.equals("http") && port == 80) ||
                            (scheme.equals("https") && port == 443))) {
                newReferer.append(":").append(port);
            }

            if (StringUtils.isNotBlank(path)) {
                newReferer.append(path);
            }

            if (StringUtils.isNotBlank(query)) {
                newReferer.append("?").append(query);
            }

            return newReferer.toString();

        } catch (Exception e) {
            log.warn("修改 Referer 失败，使用原始值：{}", originalReferer, e);
            return originalReferer;
        }
    }

    /**
     * 获取协议（http 或 https）
     */
    private String getProtocol(HttpServletRequest request) {
        String scheme = request.getScheme();
        String forwardedProto = request.getHeader("X-Forwarded-Proto");

        if (StringUtils.isNotBlank(forwardedProto)) {
            return forwardedProto.toLowerCase();
        }

        return scheme.toLowerCase();
    }

    /**
     * 提取主机名
     */
    private String extractHost(String host) {
        return host != null ? host : "";
    }

    /**
     * 复制响应头 - 完全透明模式
     * <p>
     * 完全透明地转发所有响应头，包括：
     * - Set-Cookie（完全透明传递）
     * - WWW-Authenticate（认证相关）
     * - 所有其他响应头
     */
    private void copyResponseHeaders(HttpURLConnection connection, HttpServletResponse response) {
        // 复制所有响应头（完全透明）
        Map<String, List<String>> headers = connection.getHeaderFields();
        headers.forEach((name, values) -> {
            if (shouldSkipResponseHeader(name) || values == null) {
                return;
            }
            for (String value : values) {
                response.addHeader(name, value);
                log.trace("转发响应头：{} = {}", name, value);
            }
        });
        // 注意：不添加 CORS 头，让目标服务器的 CORS 设置完全透明传递
        // 如果确实需要 CORS，应该由目标服务器设置
        // response.setHeader("Access-Control-Allow-Origin", "*");
        // response.setHeader("Access-Control-Allow-Methods", "*");
        // response.setHeader("Access-Control-Allow-Headers", "*");
    }

    /**
     * 是否跳过某个响应头
     */
    private boolean shouldSkipResponseHeader(String headerName) {
        if (headerName == null) {
            return true;
        }
        // 这些响应头不应该被转发（由代理或 Servlet 容器自动处理）
        Set<String> skipHeaders = new HashSet<>(Arrays.asList(
                "Transfer-Encoding",
                "Content-Encoding",      // 由容器处理
                "Connection",
                "Keep-Alive",
                "Proxy-Authenticate"     // 代理特定
        ));
        return skipHeaders.contains(headerName);
    }

    /**
     * 构建代理路径
     */
    private String buildProxyPath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        // 移除上下文路径
        if (StringUtils.isNotBlank(contextPath) && requestUri.startsWith(contextPath)) {
            requestUri = requestUri.substring(contextPath.length());
        }
        // 移除代理前缀
        if (requestUri.startsWith(proxy)) {
            requestUri = requestUri.substring(proxy.length());
        }
        return requestUri;
    }

    /**
     * 确定目标 URL
     */
    private String determineTargetUrl(String targetParam, HttpServletRequest request, String proxyPath) {
        // 1. 优先使用参数中的目标 URL
        if (StringUtils.isNotBlank(targetParam)) {
            return targetParam;
        }
        // 2. 从 Header 获取目标 URL
        String targetHeader = request.getHeader("X-Target-URL");
        if (StringUtils.isNotBlank(targetHeader)) {
            return targetHeader;
        }
        // 3. 从 Base-URL Header 获取
        String baseHeader = request.getHeader("X-Base-URL");
        if (StringUtils.isNotBlank(baseHeader)) {
            return baseHeader + proxyPath;
        }
        // 4. 使用默认 Base URL
        if (StringUtils.isNotBlank(base)) {
            return base + proxyPath;
        }
        // 5. 如果都没有，期望 proxyPath 是完整的 URL
        if (proxyPath.startsWith("http://") || proxyPath.startsWith("https://")) {
            return proxyPath;
        }
        return null;
    }

    /**
     * 判断是否是流式请求
     */
    private boolean isStreamingRequest(byte[] requestBody, HttpServletRequest request) {
        // 检查 Accept 头
        String accept = request.getHeader("Accept");
        if (StringUtils.isNotBlank(accept) && accept.contains("text/event-stream")) {
            return true;
        }
        // 检查请求体
        if (requestBody != null && requestBody.length > 0) {
            String body = new String(requestBody, StandardCharsets.UTF_8);
            return body.contains("\"stream\":true") || body.contains("\"stream\": true");
        }
        return false;
    }

    /**
     * 判断是否是二进制请求
     */
    private boolean isBinaryRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (StringUtils.isBlank(contentType)) {
            return false;
        }
        // 常见的二进制内容类型
        return contentType.contains("image/") ||
                contentType.contains("audio/") ||
                contentType.contains("video/") ||
                contentType.contains("application/octet-stream") ||
                contentType.contains("application/pdf") ||
                contentType.contains("application/zip") ||
                contentType.contains("multipart/");
    }

    /**
     * 是否跳过某个请求头
     * <p>
     * 为了完全透明代理，只过滤掉绝对不能转发的头：
     * - 与连接相关的头（由 HttpURLConnection 自动处理）
     * - 代理特定的头（避免混淆）
     * <p>
     * 以下头会被**保留和转发**：
     * - Cookie（完全透明传递）
     * - Authorization（完全透明传递）
     * - User-Agent（完全透明传递）
     * - Referer（完全透明传递）
     * - Origin（完全透明传递）
     * - 所有其他自定义头
     */
    private boolean shouldSkipHeader(String headerName) {
        if (headerName == null) {
            return true;
        }
        // 这些头绝对不能被转发（由 HttpURLConnection 自动处理或会导致问题）
        Set<String> skipHeaders = new HashSet<>(Arrays.asList(
                // 连接相关 - 由 HttpURLConnection 自动管理
                "Host",                      // 由目标服务器决定
                "Content-Length",            // 自动计算
                "Transfer-Encoding",         // 自动处理
                "Expect",                    // 可能导致 100-continue 问题
                "Proxy-Connection",          // 代理特定
                "Upgrade",                   // 协议升级（WebSocket 等特殊处理）
                "TE",                        // 传输编码
                "Connection",                // 连接控制

                // HTTP/2 特定
                "HTTP2-Settings",

                // 代理特定 - 避免混淆
                "Proxy-Authorization"        // 这是代理本身的认证，不是目标的
        ));
        String lowerName = headerName.toLowerCase();
        // 跳过所有 x-forwarded-* 头（这些是上游代理添加的）
        if (lowerName.startsWith("x-forwarded-")) {
            return true;
        }
        // 跳过所有 proxy-* 头
        if (lowerName.startsWith("proxy-")) {
            return true;
        }
        // 跳过代理配置相关的头
        if (lowerName.startsWith("x-target-") ||
                lowerName.startsWith("x-base-") ||
                lowerName.equals("target")) {
            return true;
        }
        // 其他所有头都透明转发
        // 包括：Cookie, Authorization, User-Agent, Referer, Origin, X-* 等
        return skipHeaders.contains(headerName);
    }

    /**
     * 读取所有字节
     */
    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return new byte[0];
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int n;
        while ((n = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, n);
        }
        return buffer.toByteArray();
    }

    /**
     * 直接从 HttpServletRequest 的 InputStream 读取原始请求体
     * 这样可以避免 Spring HttpMessageConverter 处理导致的数据丢失
     */
    private byte[] readRequestBody(HttpServletRequest request) {
        try {
            String contentLength = request.getHeader("Content-Length");
            if (contentLength != null && "0".equals(contentLength.trim())) {
                return new byte[0];
            }

            InputStream inputStream = request.getInputStream();
            if (inputStream == null) {
                return new byte[0];
            }

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[8192];
            int n;
            while ((n = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, n);
            }

            byte[] result = buffer.toByteArray();
            if (log.isTraceEnabled()) {
                log.trace("从 InputStream 读取请求体，总字节数：{}", result.length);
            }
            return result;
        } catch (IOException e) {
            log.error("读取请求体失败", e);
            return new byte[0];
        }
    }

    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(HttpServletResponse response, String message, String type, int statusCode) {
        response.setStatus(statusCode);
        response.setContentType("application/json; charset=UTF-8");
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("error", message);
        error.put("error_type", type);
        error.put("status_code", statusCode);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }

    /**
     * OPTIONS 预检请求处理 - 完全透明模式
     * <p>
     * 对于 OPTIONS 请求，先转发到目标服务器获取响应，
     * 然后完全透明地返回给客户端
     */
    public Object handleOptions(String target, HttpServletRequest request, HttpServletResponse response) {
        // 构建目标 URL
        String proxyPath = buildProxyPath(request);
        String finalTargetUrl = determineTargetUrl(target, request, proxyPath);
        if (StringUtils.isBlank(finalTargetUrl)) {
            // 如果没有目标 URL，返回默认的 CORS 头
            response.setStatus(HttpServletResponse.SC_OK);
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "*");
            response.setHeader("Access-Control-Max-Age", "86400");
            return null;
        }
        // 转发 OPTIONS 请求到目标服务器
        return proxyAllRequests(target, request, response);
    }
}