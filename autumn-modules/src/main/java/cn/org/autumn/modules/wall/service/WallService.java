package cn.org.autumn.modules.wall.service;

import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.utils.IPUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;

@Component
public class WallService {
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    IpBlackService ipBlackService;

    @Autowired
    IpWhiteService ipWhiteService;

    @Autowired
    UrlBlackService urlBlackService;

    @Autowired
    HostService hostService;

    @Autowired
    SysConfigService sysConfigService;

    public boolean isEnabled(ServletRequest servletRequest, ServletResponse servletResponse, boolean logEnable) {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        try {
            String remoteip = request.getHeader("remoteip");
            String host = request.getHeader("host");
            if (logEnable)
                print(request);
            if (hostService.isBlack(host)) {
                if (null != response) {
                    response.setStatus(500);
                }
                return false;
            }

            /**
             * 不是白名单的ip，并且是黑名单ip，就直接退出
             */
            if (!ipWhiteService.isWhite(remoteip) && ipBlackService.isBlack(remoteip)) {
                if (null != response) {
                    response.setStatus(500);
                }
                return false;
            }

            String ip = IPUtils.getIp(request);
            if (!ipWhiteService.isWhite(ip) && ipBlackService.isBlack(ip)) {
                if (null != response) {
                    response.setStatus(500);
                }
                return false;
            }

            /**
             * 黑名单地址
             */
            String uri = request.getRequestURL().toString();
            String q = request.getQueryString();
            if (StringUtils.isNotEmpty(q)) {
                uri = uri + "?" + q;
            }
            if (urlBlackService.isBlack(uri)) {
                if (null != response) {
                    response.setStatus(500);
                }
                return false;
            } else
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
        return true;
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
        logger.debug(stringBuilder.toString());
    }
}
