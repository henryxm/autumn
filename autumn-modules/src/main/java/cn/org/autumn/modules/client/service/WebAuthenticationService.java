package cn.org.autumn.modules.client.service;

import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.client.service.gen.WebAuthenticationServiceGen;
import cn.org.autumn.utils.Uuid;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class WebAuthenticationService extends WebAuthenticationServiceGen {

    public static final String clientId = "default_client_id";
    public static final String clientSecret = Uuid.uuid();

    @Override
    public int menuOrder() {
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
        WebAuthenticationEntity webAuthClientEntity = findByClientId(clientId);
        if (null != webAuthClientEntity)
            return;
        webAuthClientEntity = new WebAuthenticationEntity();
        webAuthClientEntity.setClientId(clientId);
        webAuthClientEntity.setName("默认的客户端");
        webAuthClientEntity.setClientSecret(clientSecret);
        webAuthClientEntity.setRedirectUri("http://localhost/client/oauth2/callback");
        webAuthClientEntity.setDescription("系统缺省的客户端");
        webAuthClientEntity.setAuthorizeUri("http://localhost/oauth2/authorize");
        webAuthClientEntity.setAccessTokenUri("http://localhost/oauth2/token");
        webAuthClientEntity.setUserInfoUri("http://localhost/oauth2/userInfo");
        webAuthClientEntity.setScope("all");
        webAuthClientEntity.setCreateTime(new Date());
        insert(webAuthClientEntity);
    }

    public void addLanguageColumnItem() {
        languageService.addLanguageColumnItem("client_webauthentication_table_comment", "网站客户端", "Web client");
        languageService.addLanguageColumnItem("client_webauthentication_column_id", "id");
        languageService.addLanguageColumnItem("client_webauthentication_column_name", "客户端名字", "Client name");
        languageService.addLanguageColumnItem("client_webauthentication_column_client_id", "客户端ID", "Client id");
        languageService.addLanguageColumnItem("client_webauthentication_column_client_secret", "客户端密匙", "Client secret");
        languageService.addLanguageColumnItem("client_webauthentication_column_redirect_uri", "重定向地址", "Redirect uri");
        languageService.addLanguageColumnItem("client_webauthentication_column_authorize_uri", "授权码地址", "Authorize uri");
        languageService.addLanguageColumnItem("client_webauthentication_column_access_token_uri", "Token地址", "Token uri");
        languageService.addLanguageColumnItem("client_webauthentication_column_user_info_uri", "用户信息地址", "User info uri");
        languageService.addLanguageColumnItem("client_webauthentication_column_scope", "范围", "Scope");
        languageService.addLanguageColumnItem("client_webauthentication_column_state", "状态", "State");
        languageService.addLanguageColumnItem("client_webauthentication_column_description", "描述信息", "Description");
        languageService.addLanguageColumnItem("client_webauthentication_column_create_time", "创建时间", "Create time");
    }
}
