package cn.org.autumn.modules.auth.support;

import cn.org.autumn.modules.auth.support.AuthScopeSupport;
import cn.org.autumn.oauth.spi.OAuthPlatformExtension;
import cn.org.autumn.opl.spi.OpenPlatformExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import cn.org.autumn.oauth.model.OAuthClientSnapshot;
import cn.org.autumn.opl.model.OpenAppSnapshot;

/**
 * 默认 scope 校验：委托 {@link AuthScopeSupport}。
 */
@Component
@Order(0)
public class DefaultAuthScopeExtension implements OAuthPlatformExtension, OpenPlatformExtension {

    @Autowired
    private AuthScopeSupport authScopeSupport;

    @Override
    public void validateScope(OAuthClientSnapshot client, String scope) throws Exception {
        authScopeSupport.validateOAuthScope(client, scope);
    }

    @Override
    public void validateScope(OpenAppSnapshot app, String scope) throws Exception {
        authScopeSupport.validateOplScope(app, scope);
    }
}
