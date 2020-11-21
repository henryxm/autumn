package cn.org.autumn.modules.oauth.service;

import cn.org.autumn.aspect.RedisAspect;
import cn.org.autumn.modules.oauth.entity.ClientDetailsEntity;
import cn.org.autumn.modules.oauth.service.gen.ClientDetailsServiceGen;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class ClientDetailsService extends ClientDetailsServiceGen {

    @Autowired
    RedisUtils redisUtils;

    Map<String, String> cache = new HashMap<>();

    @Autowired
    RedisAspect redisAspect;

    private void put(String code, String username) {
        if (redisAspect.isOpen()) {
            redisUtils.set(code, username);
        } else {
            cache.put(code, username);
        }
    }

    private String get(String code) {
        if (redisAspect.isOpen()) {
            return redisUtils.get(code);
        } else {
            return cache.get(code);
        }
    }

    @Override
    public int menuOrder() {
        return super.menuOrder();
    }

    @Override
    public String ico() {
        return "fa-reddit-alien";
    }

    public ClientDetailsEntity findByClientId(String clientId) {
        return baseMapper.findByClientId(clientId);
    }

    public void addAuthCode(String authCode, String username) {
        put(authCode, username);
    }

    public boolean checkClientSecret(String clientSecret) {
        return baseMapper.findByClientSecret(clientSecret) != null;
    }

    public boolean checkAuthCode(String authCode) {
        return get(authCode) != null;
    }

    public long getExpireIn() {
        return 3600L;
    }

    public String getUsernameByAuthCode(String authCode) {
        return get(authCode);
    }

    public void addAccessToken(String accessToken, String username) {
        put(accessToken, username);
    }

    public boolean checkAccessToken(String accessToken) {
        return get(accessToken) != null;
    }

    public String getUsernameByAccessToken(String accessToken) {
        return get(accessToken);
    }

    public void addLanguageColumnItem() {
        languageService.addLanguageColumnItem("oauth_clientdetails_table_comment", "客户端详情", "Client details");
        languageService.addLanguageColumnItem("oauth_clientdetails_column_id", "id");
        languageService.addLanguageColumnItem("oauth_clientdetails_column_resource_ids", "资源ID", "Resource ID");
        languageService.addLanguageColumnItem("oauth_clientdetails_column_scope", "范围", "Scope");
        languageService.addLanguageColumnItem("oauth_clientdetails_column_grant_types", "授权类型", "Grant type");
        languageService.addLanguageColumnItem("oauth_clientdetails_column_roles", "角色", "Roles");
        languageService.addLanguageColumnItem("oauth_clientdetails_column_trusted", "是否可信", "Trusted");
        languageService.addLanguageColumnItem("oauth_clientdetails_column_archived", "是否归档", "Archived");
        languageService.addLanguageColumnItem("oauth_clientdetails_column_create_time", "创建时间", "Create time");
        languageService.addLanguageColumnItem("oauth_clientdetails_column_client_id", "客户端ID", "Client ID");
        languageService.addLanguageColumnItem("oauth_clientdetails_column_client_secret", "客户端密匙", "Client secret");
        languageService.addLanguageColumnItem("oauth_clientdetails_column_client_name", "客户端名字", "Client name");
        languageService.addLanguageColumnItem("oauth_clientdetails_column_client_uri", "客户端URI", "Client uri");
        languageService.addLanguageColumnItem("oauth_clientdetails_column_client_icon_uri", "客户端图标URI", "Client icon uri");
        languageService.addLanguageColumnItem("oauth_clientdetails_column_redirect_uri", "重定向地址", "Redirect uri");
        languageService.addLanguageColumnItem("oauth_clientdetails_column_description", "描述信息", "Description");
    }

    public void init() {
        super.init();
        String clientId = "default_client_id";
        ClientDetailsEntity clientDetailsEntity = findByClientId(clientId);
        if (null != clientDetailsEntity)
            return;
        clientDetailsEntity = new ClientDetailsEntity();
        clientDetailsEntity.setClientId(clientId);
        clientDetailsEntity.setArchived(0);
        clientDetailsEntity.setClientIconUri("");
        clientDetailsEntity.setClientName("默认的客户端");
        clientDetailsEntity.setClientSecret("default_client_secret");
        clientDetailsEntity.setClientUri("http://localhost");
        clientDetailsEntity.setGrantTypes("all");
        clientDetailsEntity.setRedirectUri("http://localhost/client/oauth2/callback");
        clientDetailsEntity.setDescription("系统缺省的客户端");
        clientDetailsEntity.setScope("all");
        clientDetailsEntity.setTrusted(1);
        clientDetailsEntity.setRoles("admin");
        clientDetailsEntity.setCreateTime(new Date());
        insert(clientDetailsEntity);
    }
}
