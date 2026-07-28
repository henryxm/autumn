package cn.org.autumn.modules.opc.service;

import cn.org.autumn.auth.model.AuthRealNameInfo;
import cn.org.autumn.auth.scope.AuthScopeCatalog;
import cn.org.autumn.auth.scope.AuthScopeSet;
import cn.org.autumn.auth.scope.AuthTrack;
import cn.org.autumn.modules.auth.service.AuthRealNameService;
import cn.org.autumn.modules.auth.support.AuthRealNameHttpSupport;
import cn.org.autumn.modules.auth.support.AuthScopeSupport;
import cn.org.autumn.modules.oauth.oauth2.support.OAuth2HttpClient;
import cn.org.autumn.modules.opc.dto.OpcTokenResult;
import cn.org.autumn.modules.opc.dto.OpcUserInfoResult;
import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import cn.org.autumn.modules.opc.support.ConnectBindSupport;
import cn.org.autumn.modules.opl.service.OpenTokenService;
import cn.org.autumn.modules.opl.store.OplTokenContext;
import cn.org.autumn.opl.OplConstants;
import cn.org.autumn.opl.model.OpenAppSnapshot;
import cn.org.autumn.opl.model.OpenUserInfoSnapshot;
import cn.org.autumn.opl.spi.OpenPlatformService;
import com.alibaba.fastjson2.JSON;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** 远程 OPL OAuth HTTP 客户端；同平台时直调 {@link OpenPlatformService} 解析 platformUser。 */
@Slf4j
@Service
public class ConnectOauthService {

    @Autowired
    private ConnectAppService connectAppService;

    @Autowired
    private OAuth2HttpClient oauth2HttpClient;

    @Autowired
    private ConnectBindSupport connectBindSupport;

    @Autowired(required = false)
    private OpenPlatformService openPlatformService;

    @Autowired(required = false)
    private OpenTokenService openTokenService;

    @Autowired
    private AuthScopeSupport authScopeSupport;

    @Autowired
    private AuthScopeCatalog authScopeCatalog;

    @Autowired
    private AuthRealNameService authRealNameService;

    public String buildAuthorizeUrl(ConnectAppEntity app, String state) {
        connectAppService.fillDefaultUris(app);
        if (StringUtils.isBlank(state)) {
            state = UUID.randomUUID().toString().replace("-", "");
        }
        String scope = resolveEffectiveOplScope(app);
        return oauth2HttpClient.buildAuthorizeUrl(app.getAuthorizeUri(), OAuth2HttpClient.CredentialParam.OPL, app.getAppId(), app.getRedirectUri(), scope, state);
    }

    private String resolveEffectiveOplScope(ConnectAppEntity app) {
        String downstream = StringUtils.defaultIfBlank(app.getScope(), OplConstants.DEFAULT_SCOPE);
        if (!connectBindSupport.isSamePlatform(app) || openPlatformService == null) {
            return downstream;
        }
        OpenAppSnapshot upstream = openPlatformService.getApp(app.getAppId());
        if (upstream == null) {
            return downstream;
        }
        return authScopeSupport.grantOplScope(upstream, downstream);
    }

    public OpcTokenResult exchangeCode(ConnectAppEntity app, String code) {
        connectAppService.fillDefaultUris(app);
        return OpcTokenResult.from(oauth2HttpClient.exchangeAuthorizationCode(OAuth2HttpClient.CredentialParam.OPL, app.getTokenUri(), app.getAppId(), connectAppService.requirePlainSecret(app), code, app.getRedirectUri()));
    }

    public OpenUserInfoSnapshot fetchUserInfo(ConnectAppEntity app, String accessToken) {
        return fetchUserInfoForBind(app, accessToken, null).getSnapshot();
    }

    /** 同平台直调 OPL 时可附带 platformUser（不经过 HTTP JSON）。 */
    /**
     * @deprecated 远端预取需 token grantedScope，请用 {@link #fetchUserInfoForBind(ConnectAppEntity, String, String)}
     */
    public OpcUserInfoResult fetchUserInfoForBind(ConnectAppEntity app, String accessToken) {
        return fetchUserInfoForBind(app, accessToken, null);
    }

    /**
     * @param grantedScope token 响应中的 scope（远端实名预取用）；本地捷径仍读 OpenToken 上的 grantedScope
     */
    public OpcUserInfoResult fetchUserInfoForBind(ConnectAppEntity app, String accessToken, String grantedScope) {
        connectAppService.fillDefaultUris(app);
        if (connectBindSupport.isSamePlatform(app) && openPlatformService != null) {
            OpenUserInfoSnapshot snapshot = openPlatformService.buildUserInfo(accessToken);
            String platformUser = openPlatformService.resolvePlatformUserUuid(accessToken);
            attachLocalRealName(snapshot, platformUser, accessToken);
            return OpcUserInfoResult.of(snapshot, platformUser);
        }
        String body = oauth2HttpClient.fetchUserInfoBody(app.getUserInfoUri(), accessToken, OAuth2HttpClient.UserInfoDelivery.BEARER);
        OpenUserInfoSnapshot snapshot = JSON.parseObject(body, OpenUserInfoSnapshot.class);
        attachRemoteRealName(snapshot, app, accessToken, grantedScope);
        return OpcUserInfoResult.of(snapshot, null);
    }

    private void attachLocalRealName(OpenUserInfoSnapshot snapshot, String platformUser, String accessToken) {
        if (snapshot == null || StringUtils.isBlank(platformUser) || !authRealNameService.hasProvider()) {
            return;
        }
        if (!localTokenGrantsRealName(accessToken)) {
            return;
        }
        OplTokenContext context = openTokenService == null ? null : openTokenService.getByAccessToken(accessToken);
        AuthScopeSet granted = AuthScopeSet.withDefault(context == null ? null : context.getGrantedScope()).expand(authScopeCatalog, AuthTrack.OPL);
        AuthRealNameInfo info = authRealNameService.project(authRealNameService.resolve(platformUser), granted);
        if (info != null) {
            snapshot.setRealName(info);
        }
    }

    private boolean localTokenGrantsRealName(String accessToken) {
        if (openTokenService == null || StringUtils.isBlank(accessToken)) {
            return false;
        }
        OplTokenContext context = openTokenService.getByAccessToken(accessToken);
        if (context == null) {
            return false;
        }
        AuthScopeSet granted = AuthScopeSet.withDefault(context.getGrantedScope()).expand(authScopeCatalog, AuthTrack.OPL);
        return authRealNameService.hasAnyRealNameScope(granted);
    }

    private void attachRemoteRealName(OpenUserInfoSnapshot snapshot, ConnectAppEntity app, String accessToken, String grantedScope) {
        if (snapshot == null || app == null || StringUtils.isBlank(accessToken) || StringUtils.isBlank(grantedScope)) {
            return;
        }
        AuthScopeSet granted = AuthScopeSet.withDefault(grantedScope).expand(authScopeCatalog, AuthTrack.OPL);
        if (!authRealNameService.hasAnyRealNameScope(granted)) {
            return;
        }
        try {
            String realNameUri = AuthRealNameHttpSupport.deriveRealNameUri(app.getUserInfoUri());
            if (StringUtils.isBlank(realNameUri)) {
                return;
            }
            String body = oauth2HttpClient.fetchUserInfoBody(realNameUri, accessToken, OAuth2HttpClient.UserInfoDelivery.BEARER);
            AuthRealNameInfo info = AuthRealNameHttpSupport.parseBody(body);
            if (info != null) {
                snapshot.setRealName(info);
            }
        } catch (Exception e) {
            log.debug("realName fetch failed: {}", e.getMessage());
        }
    }
}
