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
 *
 * @author Autumn
 */
@Slf4j
@Component
public class CachedBodyRequestFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String contentType = httpRequest.getContentType();
            if (contentType != null && contentType.contains("application/json")) {
                try {
                    if (httpRequest instanceof CachedBodyHttpServletRequest) {
                        chain.doFilter(request, response);
                        return;
                    }
                    CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(httpRequest);
                    chain.doFilter(wrappedRequest, response);
                    return;
                } catch (Exception e) {
                    log.warn("包装请求失败，使用原始请求: {}", e.getMessage());
                }
            }
        }
        // 非JSON请求或包装失败，直接传递原始请求
        chain.doFilter(request, response);
    }
}
