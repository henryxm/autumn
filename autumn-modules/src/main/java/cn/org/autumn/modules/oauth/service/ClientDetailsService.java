package cn.org.autumn.modules.oauth.service;

import cn.org.autumn.modules.client.service.WebAuthenticationService;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.oauth.entity.ClientDetailsEntity;
import cn.org.autumn.modules.oauth.entity.TokenStoreEntity;
import cn.org.autumn.modules.oauth.service.gen.ClientDetailsServiceGen;
import cn.org.autumn.modules.oauth.store.TokenStore;
import cn.org.autumn.modules.oauth.store.ValueType;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.modules.sys.shiro.RedisShiroSessionDAO;
import cn.org.autumn.utils.RedisUtils;
import cn.org.autumn.utils.Uuid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ClientDetailsService extends ClientDetailsServiceGen implements LoopJob.Job {

    @Autowired
    RedisUtils redisUtils;

    @Autowired
    RedisShiroSessionDAO redisShiroSessionDAO;

    @Autowired
    TokenStoreService tokenStoreService;

    @Autowired
    SysUserService sysUserService;

    @Autowired
    SysConfigService sysConfigService;

    public static final Long AUTH_CODE_DEFAULT_EXPIRED_IN = 5 * 60L;
    public static final Long ACCESS_TOKEN_DEFAULT_EXPIRED_IN = 24 * 60 * 60L;
    public static final Long REFRESH_TOKEN_DEFAULT_EXPIRED_IN = 7 * 24 * 60 * 60L;

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

    public String getKey(ValueType type, String code) {
        return type.getValue() + code;
    }

    public void put(ValueType type, String code, String accessToken, String refreshToken, Object user, Long expire) {
        String v = code;
        if (type == ValueType.accessToken)
            v = accessToken;
        if (type == ValueType.refreshToken)
            v = refreshToken;

        String key = getKey(type, v);
        TokenStore tokenStore = new TokenStore(user, code, accessToken, refreshToken, expire);
        if (redisUtils.isOpen()) {
            redisUtils.set(key, tokenStore, expire);
        } else {
            cache.put(key, tokenStore);
        }
    }

    public void putAuthCode(String code, Object user) {
        put(ValueType.authCode, code, null, null, user, AUTH_CODE_DEFAULT_EXPIRED_IN);
    }

    /**
     * 保存Token 并删除 authCode
     *
     * @param accessToken
     * @param refreshToken
     * @param tokenStore
     */
    public void putToken(String accessToken, String refreshToken, TokenStore tokenStore) {
        Object v = null;
        TokenStoreEntity tokenStoreEntity = null;
        SysUserEntity sysUserEntity = null;
        if (null != tokenStore)
            v = tokenStore.getValue();
        if (v instanceof SysUserEntity) {
            sysUserEntity = (SysUserEntity) v;
            tokenStoreEntity = tokenStoreService.findByUser(sysUserEntity);
            if (null != tokenStoreEntity) {
                /**
                 * 删除之前的access token
                 */
                if (!tokenStoreEntity.getAccessToken().equals(accessToken)) {
                    remove(ValueType.accessToken, tokenStoreEntity.getAccessToken());
                }
                /**
                 * 删除之前的refresh token
                 */
                if (!tokenStoreEntity.getRefreshToken().equals(refreshToken)) {
                    remove(ValueType.refreshToken, tokenStoreEntity.getRefreshToken());
                }
            }
        }

        put(ValueType.accessToken, null, accessToken, refreshToken, v, ACCESS_TOKEN_DEFAULT_EXPIRED_IN);
        put(ValueType.refreshToken, null, accessToken, refreshToken, v, REFRESH_TOKEN_DEFAULT_EXPIRED_IN);
        remove(ValueType.authCode, tokenStore.getAuthCode());
        if (null != sysUserEntity) {
            tokenStoreService.saveOrUpdate(sysUserEntity, accessToken, refreshToken, tokenStore.getAuthCode(), ACCESS_TOKEN_DEFAULT_EXPIRED_IN, REFRESH_TOKEN_DEFAULT_EXPIRED_IN);
        }
    }

    public TokenStore get(ValueType type, String code) {
        String key = getKey(type, code);
        if (redisUtils.isOpen()) {
            return (TokenStore) redisUtils.get(key);
        } else {
            TokenStore od = cache.get(key);
            if (null != od && !od.isExpired())
                return od;
            return null;
        }
    }

    public void remove(ValueType type, String code) {
        String key = getKey(type, code);
        if (redisUtils.isOpen()) {
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

    public String[][] getLanguageItems() {
        String[][] items = new String[][]{
                {"oauth_clientdetails_table_comment", "客户端详情", "Client details"},
                {"oauth_clientdetails_column_id", "id"},
                {"oauth_clientdetails_column_resource_ids", "资源ID", "Resource ID"},
                {"oauth_clientdetails_column_scope", "范围", "Scope"},
                {"oauth_clientdetails_column_grant_types", "授权类型", "Grant type"},
                {"oauth_clientdetails_column_roles", "角色", "Roles"},
                {"oauth_clientdetails_column_trusted", "是否可信", "Trusted"},
                {"oauth_clientdetails_column_archived", "是否归档", "Archived"},
                {"oauth_clientdetails_column_create_time", "创建时间", "Create time"},
                {"oauth_clientdetails_column_client_id", "客户端ID", "Client ID"},
                {"oauth_clientdetails_column_client_secret", "客户端密匙", "Client secret"},
                {"oauth_clientdetails_column_client_name", "客户端名字", "Client name"},
                {"oauth_clientdetails_column_client_uri", "客户端URI", "Client uri"},
                {"oauth_clientdetails_column_client_icon_uri", "客户端图标URI", "Client icon uri"},
                {"oauth_clientdetails_column_redirect_uri", "重定向地址", "Redirect uri"},
                {"oauth_clientdetails_column_description", "描述信息", "Description"},
        };
        return items;
    }

    public void init() {
        super.init();
        LoopJob.onOneHour(this);
        ClientDetailsEntity clientDetailsEntity = findByClientId(WebAuthenticationService.clientId);
        if (null == clientDetailsEntity) {
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
        clientToUser();
    }

    public void clientToUser() {
        List<ClientDetailsEntity> list = baseMapper.selectByMap(null);
        for (ClientDetailsEntity clientDetailsEntity : list) {
            SysUserEntity sysUserEntity = sysUserService.getByUsername(clientDetailsEntity.getClientId());
            if (null == sysUserEntity)
                sysUserService.newUser(clientDetailsEntity.getClientId(), Uuid.uuid(), Uuid.uuid(), sysConfigService.getDefaultRoleKeys());
        }
    }
}
