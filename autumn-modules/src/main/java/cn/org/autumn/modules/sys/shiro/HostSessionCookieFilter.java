package cn.org.autumn.modules.sys.shiro;

import cn.org.autumn.modules.sys.service.SysConfigService;
import java.io.IOException;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 每个请求在线程上下文中解析 SessionId Cookie 参数，供 {@link HostAwareSessionIdCookie} 使用。
 */
@Component
public class HostSessionCookieFilter implements Filter {

    @Autowired
    private SysConfigService sysConfigService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest)) {
            chain.doFilter(request, response);
            return;
        }
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        try {
            String requestHost = HostSessionCookieSupport.resolveRequestHost(httpRequest);
            List<String> siteDomains = sysConfigService.getSiteDomainList();
            String clusterRoot = sysConfigService.getClusterRootDomain();
            HostSessionCookieContext.set(HostSessionCookieSupport.resolveHolder(requestHost, siteDomains, clusterRoot));
            chain.doFilter(request, response);
        } finally {
            HostSessionCookieContext.clear();
        }
    }
}
