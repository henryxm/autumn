package cn.org.autumn.modules.oauth.oauth2.support;

import cn.org.autumn.modules.oauth.entity.ClientDetailsEntity;
import cn.org.autumn.modules.oauth.service.ClientDetailsService;
import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import cn.org.autumn.modules.opc.service.ConnectAppService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * OAuth / OPL 授权确认页应用图标解析（ConnectApp → OAuth ClientDetails）。
 */
@Component
public class OAuthAuthorizeAppIconSupport {

    @Autowired(required = false)
    private ConnectAppService connectAppService;

    @Autowired(required = false)
    private ClientDetailsService clientDetailsService;

    public String resolveByAppId(String appId) {
        if (StringUtils.isBlank(appId)) {
            return null;
        }
        String trimmed = appId.trim();
        String icon = resolveFromConnectApp(trimmed);
        if (icon != null) {
            return icon;
        }
        return resolveFromClientDetails(trimmed);
    }

    public String resolveByClient(ClientDetailsEntity client) {
        if (client == null) {
            return null;
        }
        String icon = normalize(client.getClientIconUri());
        if (icon != null) {
            return icon;
        }
        return resolveByAppId(client.getClientId());
    }

    private String resolveFromConnectApp(String appId) {
        if (connectAppService == null) {
            return null;
        }
        ConnectAppEntity connectApp = connectAppService.getByAppId(appId);
        if (connectApp == null) {
            return null;
        }
        return normalize(connectApp.getIcon());
    }

    private String resolveFromClientDetails(String clientId) {
        if (clientDetailsService == null) {
            return null;
        }
        ClientDetailsEntity client = clientDetailsService.findByClientId(clientId);
        if (client == null) {
            return null;
        }
        return normalize(client.getClientIconUri());
    }

    private static String normalize(String iconUri) {
        if (StringUtils.isBlank(iconUri)) {
            return null;
        }
        return iconUri.trim();
    }
}
