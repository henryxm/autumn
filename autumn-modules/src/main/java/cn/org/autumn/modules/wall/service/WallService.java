package cn.org.autumn.modules.wall.service;

import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.oauth.service.SecurityRequestService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.usr.service.UserProfileService;
import cn.org.autumn.modules.wall.entity.RData;
import cn.org.autumn.site.HostFactory;
import cn.org.autumn.site.WallFactory;
import cn.org.autumn.utils.IPUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;

@Slf4j
@Component
public class WallService {

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

    @Autowired
    SecurityRequestService securityRequestService;

    @Autowired
    JumpService jumpService;

    public boolean isEnabled(ServletRequest servletRequest, ServletResponse servletResponse, boolean counter, boolean shield) throws IOException {
        if (!wallFactory.isOpen())
            return true;
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String ip = IPUtils.getIp(request);
        if (IPUtils.isInternalKeepIp(ip))
            return true;
        String userAgent = request.getHeader("user-agent");
        String agent = request.getHeader(SecurityRequestService.AGENT_HEADER);
        String uri = request.getRequestURI();
        if (shieldService.isAttack() && needStrongValidate(uri)) {
            String auth = request.getHeader(SecurityRequestService.AUTH_HEADER);
            String timestamp = request.getHeader(SecurityRequestService.TIMESTAMP_HEADER);
            String nonce = request.getHeader(SecurityRequestService.NONCE_HEADER);
            String signature = request.getHeader(SecurityRequestService.SIGNATURE_HEADER);
            String query = request.getQueryString();
            String uriWithQuery = StringUtils.isBlank(query) ? uri : (uri + "?" + query);
            if (!securityRequestService.verifyStrong(userAgent, agent, auth, request.getMethod(), uriWithQuery, timestamp, nonce, signature)) {
                if (shieldService.isPrint()) {
                    log.info("强力校验拦截: ip={}, uri={}, auth={}, agentHeader={}, ts={}, nonce={}", ip, uriWithQuery, auth, agent, timestamp, nonce);
                }
                if (null != response) {
                    response.setStatus(403);
                }
                ipBlackService.countIp(ip, userAgent);
                return false;
            }
        }
        if (shieldService.isAttack()) {
            if (ipBlackService.isBlack(ip)) {
                if (null != response) {
                    response.setStatus(500);
                }
                return false;
            }
        }
        if (!ipWhiteService.isWhite(ip)) {
            if (shieldService.shield(request.getRequestURI(), ip)) {
                if (!shield)
                    return false;
                response.setStatus(200);
                response.reset();
                String html = shieldService.getHtml();
                byte[] data = html.getBytes();
                IOUtils.write(data, response.getOutputStream());
                ipBlackService.countIp(ip, userAgent);
                return false;
            }
        }
        try {
            String remoteip = request.getHeader("remoteip");
            String host = request.getHeader("host");
            if (null != host && host.contains(":"))
                host = host.split(":")[0];
            String refer = request.getHeader("refer");
            print(request);
            String jump = jumpService.getJump(host, request.getRequestURI());
            if (StringUtils.isNotBlank(jump)) {
                response.setStatus(200);
                response.reset();
                String html = jumpService.getHtml(jump);
                byte[] data = html.getBytes();
                IOUtils.write(data, response.getOutputStream());
                ipBlackService.countIp(ip, userAgent);
                return false;
            }
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

            String url = request.getRequestURL().toString();
            String q = request.getQueryString();
            if (StringUtils.isNotEmpty(q)) {
                url = url + "?" + q;
            }
            if (urlBlackService.isBlack(url)) {
                if (null != response) {
                    response.setStatus(500);
                }
                return false;
            } else
                urlBlackService.countUrl(url, ip, userAgent);
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
                rData.setUri(url);
                rData.setRefer(refer);
                rData.setUserAgent(userAgent);
                hostService.count(host, rData);
                ipVisitService.count(ip, rData);
            }
        } catch (Exception e) {
            log.debug("黑名单过滤错误:{}", e.getMessage());
        }
        return true;
    }

    private boolean needStrongValidate(String uri) {
        if (StringUtils.isBlank(uri)) {
            return false;
        }
        if (isBypassUri(uri)) {
            return false;
        }
        return shieldService.matchUri(uri);
    }

    private boolean isBypassUri(String uri) {
        return uri.startsWith("/rsa/api/v1/init")
                || uri.startsWith("/rsa/api/v1/public-key")
                || uri.startsWith("/rsa/api/v1/client/public-key")
                || uri.startsWith("/rsa/api/v1/aes-key")
                || uri.startsWith("/rsa/api/v1/endpoints")
                || uri.startsWith("/wall/api")
                || uri.startsWith("/wall/shield")
                || uri.startsWith("/shield");
    }

    private void print(HttpServletRequest request) {
        if (!log.isDebugEnabled())
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
        log.debug(stringBuilder.toString());
    }
}