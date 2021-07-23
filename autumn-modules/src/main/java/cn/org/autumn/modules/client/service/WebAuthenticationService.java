package cn.org.autumn.modules.client.service;

import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.client.service.gen.WebAuthenticationServiceGen;
import cn.org.autumn.modules.sys.service.SysConfigService;
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

    public WebAuthenticationEntity findByClientId(String clientId) {
        return baseMapper.findByClientId(clientId);
    }

    @Order(2000)
    public void init() {
        super.init();
        WebAuthenticationEntity webAuthClientEntity = findByClientId(sysConfigService.getClientId());
        if (null != webAuthClientEntity)
            return;
        webAuthClientEntity = new WebAuthenticationEntity();
        webAuthClientEntity.setClientId(sysConfigService.getClientId());
        webAuthClientEntity.setName("默认的客户端");
        webAuthClientEntity.setClientSecret(sysConfigService.getClientSecret());
        webAuthClientEntity.setRedirectUri(sysConfigService.getBaseUrl() + "/client/oauth2/callback");
        webAuthClientEntity.setDescription("默认的客户端");
        webAuthClientEntity.setAuthorizeUri(sysConfigService.getBaseUrl() + "/oauth2/authorize");
        webAuthClientEntity.setAccessTokenUri(sysConfigService.getBaseUrl() + "/oauth2/token");
        webAuthClientEntity.setUserInfoUri(sysConfigService.getBaseUrl() + "/oauth2/userInfo");
        webAuthClientEntity.setScope("basic");
        webAuthClientEntity.setState("normal");
        webAuthClientEntity.setCreateTime(new Date());
        insert(webAuthClientEntity);
    }
}
