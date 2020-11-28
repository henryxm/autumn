package cn.org.autumn.modules.wall.filter;

import cn.org.autumn.modules.wall.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter(urlPatterns = "/*", filterName = "SessionFiter")
public class SessionFiter implements Filter {
    protected Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    WallService wallService;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        try {
            HttpServletRequest request = (HttpServletRequest) servletRequest;
            HttpServletResponse response = (HttpServletResponse) servletResponse;
            boolean enable = wallService.isEnabled(servletRequest, servletResponse, true);
            if (enable) {
                String originHeader = request.getHeader("Origin");
                response.setHeader("Access-Control-Allow-Origin", originHeader);
                response.setHeader("Access-Control-Allow-Methods", "PUT, POST, GET, OPTIONS, DELETE");
                response.setHeader("Access-Control-Max-Age", "0");
                response.setHeader("Access-Control-Allow-Headers", "Authorization,Origin, No-Cache, X-Requested-With, If-Modified-Since, Pragma, Last-Modified, Cache-Control, Expires, Content-Type, X-E4M-With,userId,token");
                response.setHeader("Access-Control-Allow-Credentials", "true");
                response.setHeader("XDomainRequestAllowed", "1");
                response.setHeader("XDomainRequestAllowed", "1");
                filterChain.doFilter(request, response);
            }
        } catch (Exception e) {
            logger.error("SessionFiter:" + e.getMessage());
        }
    }
}
