package cn.org.autumn.modules.client.service;

import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.client.service.gen.WebAuthenticationServiceGen;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class WebAuthenticationService extends WebAuthenticationServiceGen {

    @Override
    public int menuOrder(){
        return super.menuOrder();
    }

    @Override
    public String ico() {
        return "fa-mobile";
    }

    public WebAuthenticationEntity findByClientId(String clientId) {
        return baseMapper.findByClientId(clientId);
    }

    public void init() {
        super.init();
        String clientId = "default_client_id";
        WebAuthenticationEntity webAuthClientEntity = findByClientId(clientId);
        if (null != webAuthClientEntity)
            return;
        webAuthClientEntity = new WebAuthenticationEntity();
        webAuthClientEntity.setClientId(clientId);
        webAuthClientEntity.setName("默认的客户端");
        webAuthClientEntity.setClientSecret("default_client_secret");
        webAuthClientEntity.setRedirectUri("http://localhost/client/oauth2/callback");
        webAuthClientEntity.setDescription("系统缺省的客户端");
        webAuthClientEntity.setAuthorizeUri("http://localhost/oauth2/authorize");
        webAuthClientEntity.setAccessTokenUri("http://localhost/oauth2/token");
        webAuthClientEntity.setUserInfoUri("http://localhost/oauth2/userInfo");
        webAuthClientEntity.setScope("all");
        webAuthClientEntity.setCreateTime(new Date());
        insert(webAuthClientEntity);
    }
}
