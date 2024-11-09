package cn.org.autumn.modules.wall.service;

import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.usr.service.UserProfileService;
import cn.org.autumn.modules.wall.entity.RData;
import cn.org.autumn.site.HostFactory;
import cn.org.autumn.site.WallFactory;
import cn.org.autumn.utils.IPUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
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

    @Autowired
    ShieldService shieldService;

    public boolean isEnabled(ServletRequest servletRequest, ServletResponse servletResponse, boolean logEnable, boolean counter, boolean shield) throws IOException {
        if (!wallFactory.isOpen())
            return true;
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        if (shieldService.shield(request)) {
            if (!shield)
                return false;
            response.setStatus(200);

            response.reset();
            String html = "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "  <meta charset=\"utf-8\">\n" +
                    "  <title>人机检测</title>\n" +
                    "  <style>\n" +
                    "    .container {\n" +
                    "      display: flex;\n" +
                    "      align-items: center;\n" +
                    "      justify-content: center;\n" +
                    "    }\n" +
                    "    .submit {\n" +
                    "      height: 60px;\n" +
                    "      width: 200px;\n" +
                    "      font-size: 40px;\n" +
                    "      margin-top: 100px;\n" +
                    "    }\n" +
                    "    .icon {\n" +
                    "      display: inline-block;\n" +
                    "      fill: var(--cb-color-text-brand, #ff6a00);\n" +
                    "      margin-top: -1px;\n" +
                    "      height: 40px;\n" +
                    "      width: 80px;\n" +
                    "      margin-top: 100px;\n" +
                    "    }\n" +
                    "  </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "<form class=\"container\" action=\"/shield/test\" method=\"POST\">\n" +
                    "  <svg class=\"icon\">\n" +
                    "    <path d=\"M21.4 22.4h21.2v-4.8H21.4z\"\n" +
                    "          data-spm-anchor-id=\"5176.2020520102.console-base_top-nav.i0.51e31eb9LNAfzA\"></path>\n" +
                    "    <path d=\"M53.3 0H39.2l3.4 4.8L52.9 8c1.9.6 3.1 2.3 3.1 4.3v15.4c0 2-1.2 3.7-3.1 4.3l-10.3 3.2-3.4 4.8h14.1c6 0 10.7-4.8 10.7-10.7V10.7C64 4.7 59.2 0 53.3 0M10.7 0h14.1l-3.4 4.8L11.1 8A4.5 4.5 0 0 0 8 12.3v15.4c0 2 1.2 3.7 3.1 4.3l10.3 3.2 3.4 4.8H10.7C4.7 40 0 35.2 0 29.3V10.7C0 4.7 4.8 0 10.7 0\"></path>\n" +
                    "  </svg>\n" +
                    "  <input type=\"submit\" class=\"submit\" value=\"人机检测\">\n" +
                    "</form>\n" +
                    "</body>\n" +
                    "</html>\n";

            byte[] data = html.getBytes();
            IOUtils.write(data, response.getOutputStream());
            return false;
        }
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
            logger.debug("黑名单过滤错误:{}", e.getMessage());
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