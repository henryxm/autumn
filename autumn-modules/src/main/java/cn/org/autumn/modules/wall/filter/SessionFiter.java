package cn.org.autumn.modules.wall.filter;

import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.wall.service.WallService;
import cn.org.autumn.modules.wall.support.CorsOriginSupport;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@WebFilter(urlPatterns = "/*", filterName = "SessionFiter", asyncSupported = true)
public class SessionFiter implements Filter {

    @Autowired
    WallService wallService;

    @Autowired(required = false)
    SysConfigService sysConfigService;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        boolean enable = wallService.isEnabled(servletRequest, servletResponse, true, false);
        if (enable) {
            String originHeader = request.getHeader("Origin");
            String siteBase = null;
            List<String> allowedOrigins = Collections.emptyList();
            try {
                if (sysConfigService != null) {
                    siteBase = sysConfigService.getBaseUrl();
                    allowedOrigins = sysConfigService.getSecurityAuthConfig().corsOriginList();
                }
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.debug("CORS: failed to read site base / origin allowlist: {}", e.getMessage());
                }
            }
            if (StringUtils.isNotBlank(originHeader)
                    && CorsOriginSupport.isAllowed(originHeader, siteBase, allowedOrigins)) {
                response.setHeader("Access-Control-Allow-Origin", originHeader);
                response.setHeader("Access-Control-Allow-Credentials", "true");
                response.setHeader("Access-Control-Allow-Methods", "PUT, POST, GET, OPTIONS, DELETE");
                response.setHeader("Access-Control-Max-Age", "0");
                response.setHeader("Access-Control-Allow-Headers", "Authorization,Origin, No-Cache, X-Requested-With, If-Modified-Since, Pragma, Last-Modified, Cache-Control, Expires, Content-Type, X-E4M-With,userId,token,X-Encrypt-Session,X-Encrypt-Auth,X-Encrypt-Agent,X-Encrypt-Timestamp,X-Encrypt-Nonce,X-Encrypt-Signature,X-CSRF-Token");
                response.setHeader("Vary", "Origin");
            }
            filterChain.doFilter(request, response);
        }
    }
}
