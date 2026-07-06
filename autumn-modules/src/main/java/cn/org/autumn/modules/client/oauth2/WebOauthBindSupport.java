package cn.org.autumn.modules.client.oauth2;

import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.client.service.WebOauthCombineService;
import cn.org.autumn.modules.client.site.ClientConstants;
import cn.org.autumn.modules.oauth.oauth2.support.OAuth2HttpClient;
import java.net.URI;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** OAuth uuid 绑定辅助：同实例判定等。 */
@Component
public class WebOauthBindSupport {

    @Autowired
    private WebOauthCombineService webOauthCombineService;

    public boolean isSameUser(String upstreamUuid, String sessionUserUuid) {
        return StringUtils.isNotBlank(upstreamUuid) && StringUtils.isNotBlank(sessionUserUuid) && upstreamUuid.equalsIgnoreCase(sessionUserUuid);
    }

    /** 解析 userInfo 传参模式：显式配置优先，否则同实例 LEGACY、跨实例 BEARER。 */
    public OAuth2HttpClient.UserInfoDelivery resolveUserInfoDelivery(WebAuthenticationEntity webAuth, HttpServletRequest request) {
        if (webAuth != null && StringUtils.isNotBlank(webAuth.getUserInfoDelivery())) {
            String mode = webAuth.getUserInfoDelivery().trim();
            if (ClientConstants.USERINFO_DELIVERY_BEARER.equalsIgnoreCase(mode)) {
                return OAuth2HttpClient.UserInfoDelivery.BEARER;
            }
            if (ClientConstants.USERINFO_DELIVERY_LEGACY.equalsIgnoreCase(mode)) {
                return OAuth2HttpClient.UserInfoDelivery.LEGACY;
            }
        }
        if (isSameInstance(webAuth, request)) {
            return OAuth2HttpClient.UserInfoDelivery.LEGACY;
        }
        return OAuth2HttpClient.UserInfoDelivery.BEARER;
    }

    public boolean isSameInstance(WebAuthenticationEntity webAuth, HttpServletRequest request) {
        if (webAuth == null || request == null) {
            return false;
        }
        if (webOauthCombineService.getByClientId(webAuth.getClientId()) != null) {
            return true;
        }
        String requestHost = normalizeHost(request.getHeader("host"));
        if (StringUtils.isBlank(requestHost)) {
            requestHost = normalizeHost(request.getServerName());
            int port = request.getServerPort();
            if (port > 0 && port != 80 && port != 443) {
                requestHost = requestHost + ":" + port;
            }
        }
        return hostMatches(webAuth.getAccessTokenUri(), requestHost) || hostMatches(webAuth.getUserInfoUri(), requestHost);
    }

    private boolean hostMatches(String uriText, String requestHost) {
        if (StringUtils.isBlank(uriText) || StringUtils.isBlank(requestHost)) {
            return false;
        }
        try {
            URI uri = URI.create(uriText.trim());
            String target = normalizeHost(uri.getHost());
            if (uri.getPort() > 0) {
                target = target + ":" + uri.getPort();
            }
            return StringUtils.equalsIgnoreCase(target, requestHost) || StringUtils.equalsIgnoreCase(uri.getHost(), stripPort(requestHost));
        } catch (Exception e) {
            return false;
        }
    }

    private static String normalizeHost(String host) {
        if (StringUtils.isBlank(host)) {
            return "";
        }
        return host.trim().toLowerCase();
    }

    private static String stripPort(String host) {
        if (StringUtils.isBlank(host)) {
            return "";
        }
        int idx = host.indexOf(':');
        return idx > 0 ? host.substring(0, idx) : host;
    }
}
