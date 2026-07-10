package cn.org.autumn.modules.auth.support;

import cn.org.autumn.modules.oauth.entity.ClientDetailsEntity;
import cn.org.autumn.oauth.model.OAuthClientSnapshot;
import cn.org.autumn.opl.model.OpenAppSnapshot;
import cn.org.autumn.auth.scope.AuthScopeCatalog;
import cn.org.autumn.auth.scope.AuthScopeResolution;
import cn.org.autumn.auth.scope.AuthScopeSet;
import cn.org.autumn.auth.scope.AuthTrack;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AuthScopeSupport {

    @Autowired
    private AuthScopeCatalog authScopeCatalog;

    public AuthScopeResolution resolveOAuth(ClientDetailsEntity client, String requestedScope) {
        List<String> clientScopes = client == null ? null : client.scopes();
        return authScopeCatalog.resolve(AuthTrack.OAUTH, clientScopes, requestedScope);
    }

    public AuthScopeResolution resolveOpl(OpenAppSnapshot app, String requestedScope) {
        List<String> clientScopes = parseScopes(app == null ? null : app.getScope());
        return authScopeCatalog.resolve(AuthTrack.OPL, clientScopes, requestedScope);
    }

    public AuthScopeResolution resolveBoundedOAuth(ClientDetailsEntity client, String downstreamScope) {
        List<String> upstreamScopes = client == null ? null : client.scopes();
        return authScopeCatalog.resolveBounded(AuthTrack.OAUTH, upstreamScopes, downstreamScope);
    }

    public AuthScopeResolution resolveBoundedOpl(OpenAppSnapshot app, String downstreamScope) {
        return authScopeCatalog.resolveBounded(AuthTrack.OPL, parseScopes(app == null ? null : app.getScope()), downstreamScope);
    }

    public AuthScopeResolution resolveBoundedQrc(List<String> upstreamScopes, String downstreamScope, AuthTrack track) {
        return authScopeCatalog.resolveBounded(track == null ? AuthTrack.OAUTH : track, upstreamScopes, downstreamScope);
    }

    public String grantOAuthScope(ClientDetailsEntity client, String downstreamScope) {
        AuthScopeResolution resolution = resolveBoundedOAuth(client, downstreamScope);
        return resolution.getGranted().toScopeString();
    }

    public String grantOplScope(OpenAppSnapshot app, String downstreamScope) {
        AuthScopeResolution resolution = resolveBoundedOpl(app, downstreamScope);
        return resolution.getGranted().toScopeString();
    }

    public List<String> labels(AuthTrack track, String scope) {
        return authScopeCatalog.labels(track, AuthScopeSet.withDefault(scope));
    }

    public OAuthClientSnapshot toSnapshot(ClientDetailsEntity client) {
        OAuthClientSnapshot snapshot = new OAuthClientSnapshot();
        if (client == null) {
            return snapshot;
        }
        snapshot.setClientId(client.getClientId());
        snapshot.setClientName(client.getClientName());
        snapshot.setScope(client.getScope());
        return snapshot;
    }

    public void validateOAuthScope(OAuthClientSnapshot client, String scope) throws Exception {
        ClientDetailsEntity entity = new ClientDetailsEntity();
        entity.setScope(client == null ? null : client.getScope());
        AuthScopeResolution resolution = resolveOAuth(entity, scope);
        if (resolution.hasInvalid()) {
            throw new IllegalArgumentException("invalid_scope");
        }
        if (resolution.getGranted().isEmpty()) {
            throw new IllegalArgumentException("invalid_scope");
        }
    }

    public void validateOplScope(OpenAppSnapshot app, String scope) throws Exception {
        AuthScopeResolution resolution = resolveOpl(app, scope);
        if (resolution.hasInvalid()) {
            throw new IllegalArgumentException("invalid_scope");
        }
        if (resolution.getGranted().isEmpty()) {
            throw new IllegalArgumentException("invalid_scope");
        }
    }

    private List<String> parseScopes(String scopeCsv) {
        ClientDetailsEntity helper = new ClientDetailsEntity();
        helper.setScope(scopeCsv);
        return helper.scopes();
    }
}
