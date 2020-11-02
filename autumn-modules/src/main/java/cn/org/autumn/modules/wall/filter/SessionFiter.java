package cn.org.autumn.modules.wall.filter;

import cn.org.autumn.modules.wall.service.HostService;
import cn.org.autumn.modules.wall.service.IpBlackService;
import cn.org.autumn.modules.wall.service.IpWhiteService;
import cn.org.autumn.modules.wall.service.UrlBlackService;
import cn.org.autumn.utils.IPUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.FilterConfig;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;

@WebFilter(urlPatterns = "/*", filterName = "SessionFiter")
public class SessionFiter implements Filter {
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    IpBlackService ipBlackService;

    @Autowired
    IpWhiteService ipWhiteService;

    @Autowired
    UrlBlackService urlBlackService;

    @Autowired
    HostService hostService;

    @Override
    public void init(FilterConfig filterConfig) {
    }

    private void print(HttpServletRequest request) {
        Enumeration<String> e = request.getHeaderNames();
        boolean h = h = e.hasMoreElements();
        StringBuilder stringBuilder = new StringBuilder();
        while (h) {
            String header = e.nextElement();
            String h_value = request.getHeader(header);
            stringBuilder.append(header + ":" + h_value);
            stringBuilder.append(",  ");
            h = e.hasMoreElements();
        }
        logger.info(stringBuilder.toString());
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        try {
            String remoteip = request.getHeader("remoteip");
            String host = request.getHeader("host");
            //print(request);

            if (hostService.isBlack(host))
                return;

            /**
             * 不是白名单的ip，并且是黑名单ip，就直接退出
             */
            if (!ipWhiteService.isWhite(remoteip) && ipBlackService.isBlack(remoteip))
                return;

            String ip = IPUtils.getIp(request);
            if (!ipWhiteService.isWhite(ip) && ipBlackService.isBlack(ip))
                return;

            /**
             * 黑名单地址
             */
            String uri = request.getRequestURL().toString();
            String q = request.getQueryString();
            if (StringUtils.isNotEmpty(q)) {
                uri = uri + "?" + q;
            }
            if (urlBlackService.isBlack(uri))
                return;
            else
                urlBlackService.countUrl(uri, ip);

            if (!ipWhiteService.isWhite(ip)) {
                ipBlackService.countIp(ip);
            }

            if (null != remoteip && !remoteip.equals(ip) && !ipWhiteService.isWhite(remoteip) && !"null".equals(remoteip)) {
                ipBlackService.countIp(remoteip);
            }

            hostService.countHost(host);
        } catch (Exception e) {
            logger.error("黑名单过滤错误，需核查：", e);
        }

        HttpServletResponse response = (HttpServletResponse) servletResponse;
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

    @Override
    public void destroy() {

    }
}
