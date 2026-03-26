package cn.org.autumn.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

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

    public static final String DEFAULT = "/http/proxy/v1";

    /**
     * 默认目标基础 URL（可以动态覆盖）
     */
    @Getter
    @Setter
    private static String BASE = "";

    /**
     * OpenAI 兼容流式对话请求补齐 {@code stream_options} 时，允许缓冲的最大 body 字节数（防 OOM）。
     */
    private static final int OPENAI_CHAT_BODY_MAX_BYTES = 16 * 1024 * 1024;

    public Object proxy(String prefix, String base, String target, HttpServletRequest request, HttpServletResponse response) {
        // 构建完整的代理路径
        String proxyPath = buildProxyPath(prefix, request);
        int contentLength = request.getContentLength();
        boolean hasBody = hasBody(request, contentLength);
        InputStream bodyStream = null;
        if (hasBody) {
            try {
                bodyStream = request.getInputStream();
                if (shouldBufferForOpenAiStreamOptions(request, proxyPath)) {
                    byte[] raw = readAllBytesCapped(bodyStream, OPENAI_CHAT_BODY_MAX_BYTES);
                    closeQuietly(bodyStream);
                    byte[] patched = injectOpenAiStreamOptionsIfNeeded(raw);
                    bodyStream = new ByteArrayInputStream(patched);
                    contentLength = patched.length;
                }
            } catch (IOException e) {
                if (log.isDebugEnabled())
                    log.debug("读取失败:{}", e.getMessage(), e);
                else
                    log.error("读取失败:{}", e.getMessage());
                return createErrorResponse(response, "读取失败", "body_read_error", 400);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("===== 代理请求开始 =====");
            log.debug("方法:{}", request.getMethod());
            log.debug("路径:{}", proxyPath);
            log.debug("目标URL:{}", determineTargetUrl(base, target, request, proxyPath));
            log.debug("Content-Type: {}", request.getContentType());
            log.debug("Content-Length:{}, hasBody:{}", contentLength, hasBody);
            log.debug("===== 请求头 =====");
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerValue = request.getHeader(headerName);
                if ("Cookie".equalsIgnoreCase(headerName) || "Authorization".equalsIgnoreCase(headerName)) {
                    log.debug("{}: {}", headerName, maskSensitiveValue(headerValue));
                } else {
                    log.debug("{}: {}", headerName, headerValue);
                }
            }
        }
        String sessionId = getSessionId(request);
        try {
            String finalTargetUrl = determineTargetUrl(base, target, request, proxyPath);
            if (StringUtils.isBlank(finalTargetUrl)) {
                return createErrorResponse(response, "缺少目标", "missing_target_url", 400);
            }
            if (log.isDebugEnabled()) {
                log.debug("最终目标 URL: {}", finalTargetUrl);
            }
            // 不再在“请求阶段”区分流式/非流式/二进制，统一按透明反向代理处理：
            // - 请求体：使用 bodyStream 直接流式写入上游，同时可保存副本到 user.home/proxy
            // - 响应体：直接从上游 InputStream 拷贝到下游 HttpServletResponse OutputStream，同时可保存副本
            handle(finalTargetUrl, request, bodyStream, contentLength, response, sessionId);
            if (log.isDebugEnabled()) {
                log.debug("===== 代理请求完成 =====");
            }
            // 响应已直接写入 HttpServletResponse，Controller 层返回 null 即可
            return null;
        } catch (Exception e) {
            if (isClientDisconnect(e)) {
                if (log.isDebugEnabled()) {
                    log.debug("流式响应过程中客户端断开连接，视为正常结束");
                }
                return null;
            }
            if (log.isDebugEnabled())
                log.debug("代理失败:{}", e.getMessage(), e);
            else
                log.error("代理失败:{}", e.getMessage());
            if (!response.isCommitted()) {
                return createErrorResponse(response, "请求失败：" + e.getMessage(), "error", 500);
            }
            return null;
        }
    }

    /**
     * 判断异常是否由客户端/对端关闭连接导致（Connection reset by peer、Broken pipe、AsyncRequestNotUsableException 等），
     * 此类情况不应视为服务端错误。
     */
    protected static boolean isClientDisconnect(Throwable t) {
        for (Throwable x = t; x != null; x = x.getCause()) {
            String msg = x.getMessage();
            if (msg != null && (msg.contains("Connection reset by peer") || msg.contains("Broken pipe"))) {
                return true;
            }
            String name = x.getClass().getName();
            if (name.contains("AsyncRequestNotUsableException") || name.contains("ClientAbortException")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否缓冲 body 以便注入 OpenAI 兼容的 {@code stream_options}（POST + chat/completions + JSON）。
     */
    protected boolean shouldBufferForOpenAiStreamOptions(HttpServletRequest request, String proxyPath) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        if (StringUtils.isBlank(proxyPath) || !proxyPath.endsWith("/chat/completions")) {
            return false;
        }
        String ct = request.getContentType();
        return ct != null && ct.toLowerCase(Locale.ROOT).contains("application/json");
    }

    /**
     * 读取输入流直到 EOF，超过 maxBytes 则抛出 {@link IOException}。
     */
    protected static byte[] readAllBytesCapped(InputStream in, int maxBytes) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) {
            if (baos.size() + n > maxBytes) {
                throw new IOException("request body exceeds max bytes: " + maxBytes);
            }
            baos.write(buf, 0, n);
        }
        return baos.toByteArray();
    }

    /**
     * 对流式 {@code /chat/completions} 请求补齐 {@code stream_options}（与 OpenAI 一致）：
     * 无 {@code stream_options} 时增加 {@code include_usage:true}；已有对象但缺少 {@code include_usage} 时补上 {@code true}。
     * 非 JSON、非流式、解析失败时原样返回。
     */
    protected byte[] injectOpenAiStreamOptionsIfNeeded(byte[] body) {
        if (body == null || body.length == 0) {
            return body;
        }
        try {
            JSONObject root = JSON.parseObject(new String(body, StandardCharsets.UTF_8));
            if (root == null || root.isEmpty()) {
                return body;
            }
            if (!root.getBooleanValue("stream")) {
                return body;
            }
            Object so = root.get("stream_options");
            if (so == null) {
                JSONObject opts = new JSONObject();
                opts.put("include_usage", true);
                root.put("stream_options", opts);
                if (log.isDebugEnabled()) {
                    log.debug("已注入 stream_options.include_usage=true（原请求无 stream_options）");
                }
                return JSON.toJSONString(root).getBytes(StandardCharsets.UTF_8);
            }
            if (so instanceof JSONObject) {
                JSONObject opts = (JSONObject) so;
                if (!opts.containsKey("include_usage")) {
                    opts.put("include_usage", true);
                    if (log.isDebugEnabled()) {
                        log.debug("已注入 stream_options.include_usage=true（原 stream_options 未包含 include_usage）");
                    }
                    return JSON.toJSONString(root).getBytes(StandardCharsets.UTF_8);
                }
            }
            return body;
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("跳过 stream_options 注入（非 JSON 或解析失败）: {}", e.getMessage());
            }
            return body;
        }
    }

    /**
     * 是否有请求体（根据方法和 Content-Length/Transfer-Encoding 判断）
     */
    protected boolean hasBody(HttpServletRequest request, int contentLength) {
        String method = request.getMethod();
        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)) {
            return false;
        }
        if (contentLength == 0) {
            return false;
        }
        if (contentLength > 0) {
            return true;
        }
        String te = request.getHeader("Transfer-Encoding");
        return te != null && te.toLowerCase().contains("chunked");
    }

    /**
     * 脱敏敏感信息
     */
    protected String maskSensitiveValue(String value) {
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
     * 统一的透明反向代理处理：
     * - 不区分流式 / 非流式 / 二进制，完全依赖上游响应头、状态码和 body。
     * - 请求体通过 bodyPeek + bodyStream 流式写入上游。
     * - 响应体从上游 InputStream 直接复制到下游 HttpServletResponse OutputStream。
     * <p>
     * 这样可以最大程度保证“透明”和“完整性”，避免因为错误的流式判断导致请求体或响应体被截断。
     */
    protected void handle(String target, HttpServletRequest request, InputStream bodyStream, int length, HttpServletResponse response, String sessionId) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("目标URL: {}", target);
        }
        HttpURLConnection connection = null;
        try {
            connection = connection(target, request, bodyStream, length, sessionId);
            if (log.isDebugEnabled()) {
                log.debug("上游连接已建立");
            }
            int statusCode = connection.getResponseCode();
            if (log.isDebugEnabled()) {
                log.debug("上游响应状态码：{}", statusCode);
            }
            // 设置下游状态码
            response.setStatus(statusCode);
            // 复制上游响应头到下游
            copyResponseHeaders(connection, response);
            // 透传响应体，并保存副本到 user.home/proxy/{sessionId}-response（副本失败不影响主流）
            InputStream upstream = (statusCode >= 400) ? connection.getErrorStream() : connection.getInputStream();
            if (upstream != null) {
                OutputStream out = response.getOutputStream();
                OutputStream copy = getResponseCopyStream(getResponseCopyPath(sessionId, "response"));
                byte[] buffer = new byte[8192];
                int n;
                long total = 0L;
                boolean clientClosed = false;
                try {
                    while ((n = upstream.read(buffer)) != -1) {
                        try {
                            out.write(buffer, 0, n);
                            out.flush();
                        } catch (IOException e) {
                            if (isClientDisconnect(e)) {
                                if (log.isDebugEnabled()) {
                                    log.debug("透传响应时客户端已断开，已发送约 {} 字节", total);
                                }
                                clientClosed = true;
                                break;
                            }
                            throw e;
                        }
                        if (copy != null) {
                            try {
                                copy.write(buffer, 0, n);
                                copy.flush();
                            } catch (IOException e) {
                                log.warn("保存响应副本失败，sessionId: {}", sessionId, e);
                                closeQuietly(copy);
                                copy = null;
                            }
                        }
                        total += n;
                    }
                    if (!clientClosed && log.isDebugEnabled()) {
                        log.debug("已透传响应体：{} 字节", total);
                    }
                } finally {
                    closeQuietly(copy);
                }
            } else if (log.isDebugEnabled()) {
                log.debug("上游响应体为空");
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            closeQuietly(bodyStream);
        }
    }

    protected static void closeQuietly(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                if (log.isDebugEnabled())
                    log.error("关闭输入:{}", e.getMessage(), e);
            }
        }
    }

    protected static void closeQuietly(OutputStream out) {
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                if (log.isDebugEnabled())
                    log.error("关闭输出:{}", e.getMessage(), e);
            }
        }
    }

    /**
     * 获取当前请求的 sessionId，用于保存请求/响应副本的文件名；无 session 时使用 UUID。
     */
    protected static String getSessionId(HttpServletRequest request) {
        try {
            return request.getSession(true).getId();
        } catch (IllegalStateException e) {
            return UUID.randomUUID().toString();
        }
    }

    /**
     * user.home/proxy 目录下按 sessionId 命名的副本文件名，suffix 为 "request" 或 "response"。
     */
    protected Path getRequestCopyPath(String sessionId, String suffix) {
        String dir = System.getProperty("user.home");
        return Paths.get(dir, "proxy", sessionId + "." + suffix);
    }

    protected Path getResponseCopyPath(String sessionId, String suffix) {
        return getRequestCopyPath(sessionId, suffix);
    }

    /**
     * 打开写入 proxy 目录的副本流；目录不存在会先创建。失败时返回 null，不影响主流程。
     */
    protected OutputStream getRequestCopyStream(Path filePath) {
        if (filePath == null) return null;
        try {
            Files.createDirectories(filePath.getParent());
            return Files.newOutputStream(filePath);
        } catch (IOException e) {
            if (log.isDebugEnabled())
                log.debug("无法创建代理副本文件: {}", filePath, e);
            return null;
        }
    }

    protected OutputStream getResponseCopyStream(Path filePath) {
        return getRequestCopyStream(filePath);
    }

    /**
     * 创建 HTTP 连接并发送请求体：将 bodyStream 流式写入上游，同时可选保存副本到 user.home/proxy/{sessionId}-request。
     *
     * @param bodyStream 请求体输入流，可为 null（表示无 body）
     * @param length     请求头 Content-Length，-1 表示未知或 chunked
     * @param sessionId  会话 ID，用于保存请求体副本文件名
     */
    protected HttpURLConnection connection(String targetUrl, HttpServletRequest request, InputStream bodyStream, int length, String sessionId) throws Exception {
        boolean hasBody = (bodyStream != null);
        URL url = new URL(targetUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(request.getMethod());
        connection.setDoInput(true);
        connection.setDoOutput(hasBody);
        connection.setConnectTimeout(60000);
        connection.setReadTimeout(300000);
        connection.setInstanceFollowRedirects(false);
        copyRequestHeaders(request, connection);
        if (hasBody) {
            OutputStream out = connection.getOutputStream();
            OutputStream copy = getRequestCopyStream(getRequestCopyPath(sessionId, "request"));
            byte[] buf = new byte[8192];
            int n, total = 0;
            try {
                while ((n = bodyStream.read(buf)) != -1) {
                    total += n;
                    out.write(buf, 0, n);
                    if (copy != null) {
                        try {
                            copy.write(buf, 0, n);
                            copy.flush();
                        } catch (IOException e) {
                            log.warn("保存请求副本失败，sessionId: {}", sessionId, e);
                            closeQuietly(copy);
                            copy = null;
                        }
                    }
                }
                out.flush();
                if (log.isDebugEnabled()) {
                    log.debug("请求长度:{}, 实际长度:{}", length, total);
                }
            } finally {
                closeQuietly(copy);
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
    protected void copyRequestHeaders(HttpServletRequest request, HttpURLConnection connection) {
        String clientIp = getClientIp(request);
        String targetHost = extractHost(connection.getURL().getHost());
        // 复制所有请求头（完全透明）
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            // 跳过绝对不能转发的头
            if (shouldSkipHeader(headerName)) {
                if (log.isDebugEnabled())
                    log.debug("跳过请求头：{}", headerName);
                continue;
            }
            // 特殊处理 Referer - 修改为目标服务器域名
            if ("Referer".equalsIgnoreCase(headerName)) {
                String referer = request.getHeader(headerName);
                // 将 Referer 中的代理服务器地址替换为目标服务器地址
                String modifiedReferer = modifyReferer(referer, targetHost);
                connection.setRequestProperty(headerName, modifiedReferer);
                if (log.isDebugEnabled())
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
        if (log.isDebugEnabled())
            log.debug("添加代理头 - X-Forwarded-For: {}, X-Real-IP: {}, X-Forwarded-Proto: {}", clientIp, clientIp, getProtocol(request));
    }

    /**
     * 获取客户端真实 IP
     */
    protected String getClientIp(HttpServletRequest request) {
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
    protected String modifyReferer(String originalReferer, String targetHost) {
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
            if (port > 0 && !((scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443))) {
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
    protected String getProtocol(HttpServletRequest request) {
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
    protected String extractHost(String host) {
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
    protected void copyResponseHeaders(HttpURLConnection connection, HttpServletResponse response) {
        // 复制所有响应头（完全透明）
        Map<String, List<String>> headers = connection.getHeaderFields();
        headers.forEach((name, values) -> {
            if (shouldSkipResponseHeader(name) || values == null) {
                return;
            }
            for (String value : values) {
                response.addHeader(name, value);
                if (log.isTraceEnabled())
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
    protected boolean shouldSkipResponseHeader(String headerName) {
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
    protected String buildProxyPath(String prefix, HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        // 移除上下文路径
        if (StringUtils.isNotBlank(contextPath) && requestUri.startsWith(contextPath)) {
            requestUri = requestUri.substring(contextPath.length());
        }
        if (StringUtils.isBlank(prefix)) {
            prefix = DEFAULT;
        }
        // 移除代理前缀
        if (requestUri.startsWith(prefix)) {
            requestUri = requestUri.substring(prefix.length());
        }
        return requestUri;
    }

    /**
     * 确定目标 URL
     */
    protected String determineTargetUrl(String base, String target, HttpServletRequest request, String proxyPath) {
        // 1. 优先使用参数中的目标 URL
        if (StringUtils.isNotBlank(target)) {
            return target;
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
        if (StringUtils.isBlank(base))
            base = BASE;
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
    protected boolean shouldSkipHeader(String headerName) {
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
        if (lowerName.startsWith("x-target-") || lowerName.startsWith("x-base-") || lowerName.equals("target")) {
            return true;
        }
        // 其他所有头都透明转发
        // 包括：Cookie, Authorization, User-Agent, Referer, Origin, X-* 等
        return skipHeaders.contains(headerName);
    }

    /**
     * 创建错误响应
     */
    protected Map<String, Object> createErrorResponse(HttpServletResponse response, String message, String type, int statusCode) {
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
    public Object options(String prefix, String base, String target, HttpServletRequest request, HttpServletResponse response) {
        // 构建目标 URL
        String proxyPath = buildProxyPath(prefix, request);
        String finalTargetUrl = determineTargetUrl(target, base, request, proxyPath);
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
        return proxy(prefix, base, target, request, response);
    }
}