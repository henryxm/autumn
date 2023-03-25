package cn.org.autumn.modules.wall.service;

import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.usr.service.UserProfileService;
import cn.org.autumn.modules.wall.entity.RData;
import cn.org.autumn.site.HostFactory;
import cn.org.autumn.site.WallFactory;
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
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    IpBlackService ipBlackService;

    @Autowired
    IpWhiteService ipWhiteService;

    @Autowired
    UrlBlackService urlBlackService;

    @Autowired
    IpVisitService ipVisitService;

    @Autowired
    HostService hostService;

    @Autowired
    SysConfigService sysConfigService;

    @Autowired
    HostFactory hostFactory;

    @Autowired
    WallFactory wallFactory;

    @Autowired
    UserProfileService userProfileService;

    public boolean isEnabled(ServletRequest servletRequest, ServletResponse servletResponse, boolean logEnable, boolean counter) {
        if (!wallFactory.isOpen())
            return true;
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        try {
            String remoteip = request.getHeader("remoteip");
            String host = request.getHeader("host");
            String userAgent = request.getHeader("user-agent");
            String refer = request.getHeader("refer");
            if (logEnable)
                print(request);
            if (hostService.isBlack(host) || !hostFactory.isAllowed(request, response)) {
                if (null != response) {
                    response.setStatus(500);
                }
                return false;
            }

            // 不是白名单的ip，并且是黑名单ip，就直接退出
            if (!ipWhiteService.isWhite(remoteip, userAgent) && ipBlackService.isBlack(remoteip, userAgent)) {
                if (null != response) {
                    response.setStatus(500);
                }
                return false;
            }

            String ip = IPUtils.getIp(request);
            try {
                SysUserEntity userEntity = ShiroUtils.getUserEntity();
                if (null != userEntity) {
                    userProfileService.updateVisitIp(userEntity.getUuid(), ip, userAgent);
                }
            } catch (Exception ignored) {
            }

            if (!ipWhiteService.isWhite(ip, userAgent) && ipBlackService.isBlack(ip, userAgent)) {
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
                urlBlackService.countUrl(uri, ip, userAgent);
            if (!ipWhiteService.isWhite(ip, userAgent)) {
                ipBlackService.countIp(ip, userAgent);
            }
            if (null != remoteip && !remoteip.equals(ip) && !ipWhiteService.isWhite(remoteip, userAgent) && !"null".equals(remoteip)) {
                ipBlackService.countIp(remoteip, userAgent);
            }
            if (counter) {
                RData rData = new RData();
                rData.setHost(host);
                rData.setIp(ip);
                rData.setUri(uri);
                rData.setRefer(refer);
                rData.setUserAgent(userAgent);
                hostService.count(host, rData);
                ipVisitService.count(ip, rData);
            }
        } catch (Exception e) {
            logger.error("黑名单过滤错误，需核查：{}", e.getMessage());
        }
        return true;
    }

    private void print(HttpServletRequest request) {
        if (!logger.isDebugEnabled())
            return;
        Enumeration<String> e = request.getHeaderNames();
        boolean h = e.hasMoreElements();
        StringBuilder stringBuilder = new StringBuilder();
        while (h) {
            String header = e.nextElement();
            String h_value = request.getHeader(header);
            stringBuilder.append(header).append(":").append(h_value);
            stringBuilder.append(",  ");
            h = e.hasMoreElements();
        }
        logger.debug(stringBuilder.toString());
    }
}