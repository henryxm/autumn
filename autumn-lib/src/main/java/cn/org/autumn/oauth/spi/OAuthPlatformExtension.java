package cn.org.autumn.oauth.spi;

import cn.org.autumn.auth.model.AuthUserInfo;
import cn.org.autumn.oauth.model.OAuthClientSnapshot;

/**
 * OAuth 经典轨扩展点：scope 校验与 userInfo enrichment。
 */
public interface OAuthPlatformExtension {

    default boolean supports(OAuthClientSnapshot client) {
        return true;
    }

    default void validateScope(OAuthClientSnapshot client, String scope) throws Exception {
    }

    default void enrichUserInfo(OAuthClientSnapshot client, AuthUserInfo userInfo) {
    }
}
