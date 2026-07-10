package cn.org.autumn.modules.opc.service;

import cn.org.autumn.modules.auth.support.AuthScopeSupport;
import cn.org.autumn.modules.oauth.oauth2.support.OAuth2HttpClient;
import cn.org.autumn.modules.opc.dto.OpcTokenResult;
import cn.org.autumn.modules.opc.dto.OpcUserInfoResult;
import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import cn.org.autumn.modules.opc.support.ConnectBindSupport;
import cn.org.autumn.opc.OpcConstants;
import cn.org.autumn.opl.OplConstants;
import cn.org.autumn.opl.model.OpenAppSnapshot;
import cn.org.autumn.opl.model.OpenUserInfoSnapshot;
import cn.org.autumn.opl.spi.OpenPlatformService;
import com.alibaba.fastjson.JSON;
import java.util.UUID;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** 远程 OPL OAuth HTTP 客户端；同平台时直调 {@link OpenPlatformService} 解析 platformUser。 */
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

    @Autowired
    private AuthScopeSupport authScopeSupport;

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
        return fetchUserInfoForBind(app, accessToken).getSnapshot();
    }

    /** 同平台直调 OPL 时可附带 platformUser（不经过 HTTP JSON）。 */
    public OpcUserInfoResult fetchUserInfoForBind(ConnectAppEntity app, String accessToken) {
        connectAppService.fillDefaultUris(app);
        if (connectBindSupport.isSamePlatform(app) && openPlatformService != null) {
            OpenUserInfoSnapshot snapshot = openPlatformService.buildUserInfo(accessToken);
            String platformUser = openPlatformService.resolvePlatformUserUuid(accessToken);
            return OpcUserInfoResult.of(snapshot, platformUser);
        }
        String body = oauth2HttpClient.fetchUserInfoBody(app.getUserInfoUri(), accessToken, OAuth2HttpClient.UserInfoDelivery.BEARER);
        OpenUserInfoSnapshot snapshot = JSON.parseObject(body, OpenUserInfoSnapshot.class);
        return OpcUserInfoResult.of(snapshot, null);
    }
}
