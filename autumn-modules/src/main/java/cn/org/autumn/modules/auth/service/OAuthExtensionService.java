package cn.org.autumn.modules.auth.service;

import cn.org.autumn.auth.model.AuthUserInfo;
import cn.org.autumn.oauth.model.OAuthClientSnapshot;
import cn.org.autumn.oauth.spi.OAuthPlatformExtension;
import cn.org.autumn.site.Factory;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OAuthExtensionService extends Factory {

    public void validateScope(OAuthClientSnapshot client, String scope) throws Exception {
        for (OAuthPlatformExtension extension : extensions(client)) {
            extension.validateScope(client, scope);
        }
    }

    public void enrichUserInfo(OAuthClientSnapshot client, AuthUserInfo userInfo) {
        for (OAuthPlatformExtension extension : extensions(client)) {
            extension.enrichUserInfo(client, userInfo);
        }
    }

    private List<OAuthPlatformExtension> extensions(OAuthClientSnapshot client) {
        List<OAuthPlatformExtension> matched = new ArrayList<>();
        for (OAuthPlatformExtension extension : getOrderList(OAuthPlatformExtension.class)) {
            if (extension.supports(client)) {
                matched.add(extension);
            }
        }
        return matched;
    }
}
