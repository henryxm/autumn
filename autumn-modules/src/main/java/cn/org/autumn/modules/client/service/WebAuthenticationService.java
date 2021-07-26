package cn.org.autumn.modules.client.service;

import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.client.service.gen.WebAuthenticationServiceGen;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.utils.Uuid;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class WebAuthenticationService extends WebAuthenticationServiceGen {

    @Autowired
    SysConfigService sysConfigService;

    @Override
    public String ico() {
        return "fa-mobile";
    }

    public WebAuthenticationEntity getByClientId(String clientId) {
        if (StringUtils.isBlank(clientId))
            return null;
        return baseMapper.getByClientId(clientId);
    }

    public boolean hasClientId(String clientId) {
        if (StringUtils.isBlank(clientId))
            return false;
        Integer has = baseMapper.hasClientId(clientId);
        return null != has && has > 0;
    }

    public WebAuthenticationEntity create(String clientId, String clientSecret, String baseUrl, String name, String scope, String state) {
        try {
            if (StringUtils.isBlank(clientId))
                return null;
            WebAuthenticationEntity webAuthClientEntity = new WebAuthenticationEntity();
            webAuthClientEntity.setClientId(clientId);
            if (StringUtils.isBlank(name))
                name = clientId;
            webAuthClientEntity.setName(name);
            if (StringUtils.isBlank(clientSecret))
                clientSecret = Uuid.uuid();
            webAuthClientEntity.setClientSecret(clientSecret);
            if (StringUtils.isBlank(baseUrl))
                baseUrl = sysConfigService.getBaseUrl();
            webAuthClientEntity.setRedirectUri(baseUrl + "/client/oauth2/callback");
            webAuthClientEntity.setDescription(name);
            webAuthClientEntity.setAuthorizeUri(baseUrl + "/oauth2/authorize");
            webAuthClientEntity.setAccessTokenUri(baseUrl + "/oauth2/token");
            webAuthClientEntity.setUserInfoUri(baseUrl + "/oauth2/userInfo");
            if (StringUtils.isBlank(scope))
                scope = "basic";
            webAuthClientEntity.setScope(scope);
            if (StringUtils.isBlank(state))
                state = "normal";
            webAuthClientEntity.setState(state);
            webAuthClientEntity.setCreateTime(new Date());
            insert(webAuthClientEntity);
            return webAuthClientEntity;
        } catch (Exception ignored) {
        }
        return null;
    }

    @Order(2000)
    public void init() {
        super.init();
        if (!hasClientId(sysConfigService.getClientId())) {
            create(sysConfigService.getClientId(), sysConfigService.getClientSecret(), sysConfigService.getBaseUrl(), "默认的客户端", "basic", "normal");
        }
    }
}
