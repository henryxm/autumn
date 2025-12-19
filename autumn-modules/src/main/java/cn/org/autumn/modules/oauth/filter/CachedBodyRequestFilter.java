package cn.org.autumn.modules.oauth.filter;

import cn.org.autumn.modules.oauth.interceptor.CachedBodyHttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 缓存请求体的Filter
 * 在Filter层包装请求，确保整个请求处理链都能多次读取请求体
 * <p>
 * 优化说明：
 * 1. 只处理需要多次读取请求体的场景（JSON请求）
 * 2. 添加连接状态检查，避免Broken pipe错误
 * 3. 添加请求体大小限制，避免内存溢出
 * 4. 优化异常处理，区分不同类型的错误
 *
 * @author Autumn
 */
@Slf4j
@Component
public class CachedBodyRequestFilter implements Filter {

    /**
     * 最大请求体大小（10MB），超过此大小不缓存，避免内存溢出
     */
    private static final int MAX_BODY_SIZE = 10 * 1024 * 1024;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // 如果已经是包装过的请求，直接传递
        if (httpRequest instanceof CachedBodyHttpServletRequest) {
            chain.doFilter(request, response);
            return;
        }

        // 只处理JSON请求
        String contentType = httpRequest.getContentType();
        if (contentType == null || !contentType.contains("application/json")) {
            chain.doFilter(request, response);
            return;
        }

        // 检查Content-Length，如果超过限制，不缓存
        long contentLength = httpRequest.getContentLengthLong();
        if (contentLength > MAX_BODY_SIZE) {
            if (log.isDebugEnabled()) {
                log.debug("请求体过大（{}字节），跳过缓存，直接传递原始请求", contentLength);
            }
            chain.doFilter(request, response);
            return;
        }

        // 尝试包装请求
        try {
            CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(httpRequest);
            chain.doFilter(wrappedRequest, response);
        } catch (java.net.SocketException e) {
            // Socket异常（包括Broken pipe），通常是客户端提前关闭连接
            // 这种情况下，请求体可能已经被部分读取，无法再次读取
            if (log.isDebugEnabled()) {
                log.debug("客户端连接已关闭，无法缓存请求体: {}", e.getMessage());
            }
            // 直接传递原始请求，让后续处理决定如何处理
            chain.doFilter(request, response);
        } catch (IOException e) {
            // IO异常，可能是连接问题或请求体读取问题
            String errorMsg = e.getMessage();
            if (errorMsg != null && (errorMsg.contains("Broken pipe") || errorMsg.contains("Connection reset"))) {
                // 连接已断开，无法读取请求体
                if (log.isDebugEnabled()) {
                    log.debug("连接已断开，无法缓存请求体: {}", errorMsg);
                }
            } else {
                // 其他IO异常，记录警告
                log.warn("读取请求体失败，使用原始请求: {}", errorMsg);
            }
            chain.doFilter(request, response);
        } catch (Exception e) {
            // 其他异常，记录警告但不影响请求处理
            log.warn("包装请求失败，使用原始请求: {}", e.getMessage());
            chain.doFilter(request, response);
        }
    }
}
