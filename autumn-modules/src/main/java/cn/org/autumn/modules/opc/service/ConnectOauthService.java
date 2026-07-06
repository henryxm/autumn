package cn.org.autumn.modules.opc.service;

import cn.org.autumn.modules.oauth.oauth2.support.OAuth2HttpClient;
import cn.org.autumn.modules.opc.dto.OpcTokenResult;
import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import cn.org.autumn.opc.OpcConstants;
import cn.org.autumn.opl.OplConstants;
import cn.org.autumn.opl.model.OpenUserInfoSnapshot;
import com.alibaba.fastjson.JSON;
import java.util.UUID;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** 远程 OPL OAuth HTTP 客户端。 */
@Service
public class ConnectOauthService {

    @Autowired
    private ConnectAppService connectAppService;

    @Autowired
    private OAuth2HttpClient oauth2HttpClient;

    public String buildAuthorizeUrl(ConnectAppEntity app, String state) {
        connectAppService.fillDefaultUris(app);
        if (StringUtils.isBlank(state)) {
            state = UUID.randomUUID().toString().replace("-", "");
        }
        return oauth2HttpClient.buildAuthorizeUrl(app.getAuthorizeUri(), OAuth2HttpClient.CredentialParam.OPL, app.getAppId(), app.getRedirectUri(), StringUtils.defaultIfBlank(app.getScope(), OplConstants.DEFAULT_SCOPE), state);
    }

    public OpcTokenResult exchangeCode(ConnectAppEntity app, String code) {
        connectAppService.fillDefaultUris(app);
        return OpcTokenResult.from(oauth2HttpClient.exchangeAuthorizationCode(OAuth2HttpClient.CredentialParam.OPL, app.getTokenUri(), app.getAppId(), connectAppService.requirePlainSecret(app), code, app.getRedirectUri()));
    }

    public OpenUserInfoSnapshot fetchUserInfo(ConnectAppEntity app, String accessToken) {
        connectAppService.fillDefaultUris(app);
        String body = oauth2HttpClient.fetchUserInfoBody(app.getUserInfoUri(), accessToken, OAuth2HttpClient.UserInfoDelivery.BEARER);
        return JSON.parseObject(body, OpenUserInfoSnapshot.class);
    }
}
