package cn.org.autumn.modules.oauth.service;

import cn.org.autumn.aspect.RedisAspect;
import cn.org.autumn.modules.client.service.WebAuthenticationService;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.oauth.entity.ClientDetailsEntity;
import cn.org.autumn.modules.oauth.service.gen.ClientDetailsServiceGen;
import cn.org.autumn.modules.oauth.store.TokenStore;
import cn.org.autumn.modules.oauth.store.ValueType;
import cn.org.autumn.modules.sys.shiro.RedisShiroSessionDAO;
import cn.org.autumn.utils.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ClientDetailsService extends ClientDetailsServiceGen implements LoopJob.Job {

    @Autowired
    RedisUtils redisUtils;

    @Autowired
    RedisAspect redisAspect;

    @Autowired
    RedisShiroSessionDAO redisShiroSessionDAO;

    Map<String, TokenStore> cache = new HashMap<>();

    @Override
    public void runJob() {
        Iterator<String> iterator = cache.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            TokenStore od = cache.get(key);
            if (od.isExpired()) {
                iterator.remove();
            }
        }
    }

    public static void main(String[] args) {
        Map<String, String> map = new HashMap<>();
        map.put("1", "1");
        map.put("2", "1");
        map.put("3", "1");
        Iterator<String> iterator = map.keySet().iterator();
        while (iterator.hasNext()) {
            String k = iterator.next();
            if (k.equals("2")) {
                iterator.remove();
            }
        }
        System.out.println(ValueType.accessToken.getValue() + "ddd");
    }

    public String getKey(ValueType type, String code) {
        return type.getValue() + code;
    }

    private void put(ValueType type, String code, String accessToken, String refreshToken, Object user, Long expire) {
        String v = code;
        if (type == ValueType.accessToken)
            v = accessToken;
        if (type == ValueType.refreshToken)
            v = refreshToken;

        String key = getKey(type, v);
        TokenStore tokenStore = new TokenStore(user, code, accessToken, refreshToken, expire);
        if (redisAspect.isOpen()) {
            redisUtils.set(key, tokenStore, expire);
        } else {
            cache.put(key, tokenStore);
        }
    }

    public void putAuthCode(String code, Object user) {
        put(ValueType.authCode, code, null, null, user, 5 * 60L);
    }

    /**
     * 保存Token 并删除 authCode
     * @param accessToken
     * @param refreshToken
     * @param authCode
     */
    public void putToken(String accessToken, String refreshToken, String authCode) {
        TokenStore tokenStore = get(ValueType.authCode, authCode);
        Object v = null;
        if (null != tokenStore)
            v = tokenStore.getValue();
        put(ValueType.accessToken, null, accessToken, refreshToken, v, 24 * 60 * 60L);
        put(ValueType.refreshToken, null, accessToken, refreshToken, v, 7 * 24 * 60 * 60L);
        remove(ValueType.authCode, authCode);
    }

    public TokenStore get(ValueType type, String code) {
        String key = getKey(type, code);
        if (redisAspect.isOpen()) {
            return (TokenStore) redisUtils.get(key);
        } else {
            TokenStore od = cache.get(key);
            if (!od.isExpired())
                return od;
            return null;
        }
    }

    public void remove(ValueType type, String code) {
        String key = getKey(type, code);
        if (redisAspect.isOpen()) {
            redisUtils.delete(key);
        } else {
            cache.remove(key);
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

    public boolean isValidClientSecret(String clientSecret) {
        return baseMapper.findByClientSecret(clientSecret) != null;
    }

    public boolean isValidCode(String code) {
        return get(ValueType.authCode, code) != null;
    }

    public boolean isValidAccessToken(String accessToken) {
        return get(ValueType.accessToken, accessToken) != null;
    }

    public boolean isValidRefreshToken(String refreshToken) {
        return get(ValueType.refreshToken, refreshToken) != null;
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
        LoopJob.onOneHour(this);
        ClientDetailsEntity clientDetailsEntity = findByClientId(WebAuthenticationService.clientId);
        if (null != clientDetailsEntity)
            return;
        clientDetailsEntity = new ClientDetailsEntity();
        clientDetailsEntity.setClientId(WebAuthenticationService.clientId);
        clientDetailsEntity.setArchived(0);
        clientDetailsEntity.setClientIconUri("");
        clientDetailsEntity.setClientName("默认的客户端");
        clientDetailsEntity.setClientSecret(WebAuthenticationService.clientSecret);
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
