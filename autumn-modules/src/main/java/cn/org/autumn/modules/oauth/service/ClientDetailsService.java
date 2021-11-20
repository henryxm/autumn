package cn.org.autumn.modules.oauth.service;

import cn.org.autumn.cluster.UserHandler;
import cn.org.autumn.cluster.UserMapping;
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
import cn.org.autumn.site.InitFactory;
import cn.org.autumn.utils.RedisUtils;
import cn.org.autumn.utils.Uuid;
import com.qiniu.util.Md5;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.AsyncTaskExecutor;
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

    @Autowired
    AsyncTaskExecutor asyncTaskExecutor;

    @Autowired(required = false)
    List<UserHandler> userHandlers;

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
        if (null != tokenStore)
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

    @Order(2000)
    public void init() {
        super.init();
        LoopJob.onOneHour(this);
        create(sysConfigService.getClientId(), sysConfigService.getClientSecret(), "默认的客户端", "默认的客户端");
    }

    public static final String allChar = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static String newKey(String prefix) {
        int length = 10;
        StringBuffer sb = new StringBuffer();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(allChar.charAt(random.nextInt(allChar.length())));
        }
        return prefix + sb;
    }

    public ClientDetailsEntity create(String clientId, String secret, String name, String description) {
        if (StringUtils.isBlank(clientId))
            return null;
        if (StringUtils.isBlank(secret))
            secret = Uuid.uuid();
        ClientDetailsEntity clientDetailsEntity = findByClientId(clientId);
        if (null == clientDetailsEntity) {
            clientDetailsEntity = new ClientDetailsEntity();
            clientDetailsEntity.setClientId(clientId);
            clientDetailsEntity.setArchived(0);
            clientDetailsEntity.setClientIconUri("");
            clientDetailsEntity.setClientName(name);
            clientDetailsEntity.setClientSecret(secret);
            clientDetailsEntity.setClientUri(sysConfigService.getBaseUrl());
            clientDetailsEntity.setGrantTypes("all");
            clientDetailsEntity.setResourceIds("all");
            clientDetailsEntity.setRedirectUri(sysConfigService.getBaseUrl() + "/client/oauth2/callback");
            clientDetailsEntity.setDescription(description);
            clientDetailsEntity.setScope("basic");
            clientDetailsEntity.setTrusted(1);
            clientDetailsEntity.setRoles("user");
            clientDetailsEntity.setCreateTime(new Date());
            insert(clientDetailsEntity);
            clientToUser(clientDetailsEntity);
        }
        return clientDetailsEntity;
    }

    public ClientDetailsEntity create(String prefix) {
        String accessKey = newKey(prefix);
        String secret = Md5.md5((accessKey + Uuid.uuid()).getBytes());
        ClientDetailsEntity clientDetailsEntity = findByClientId(accessKey);
        if (null == clientDetailsEntity) {
            clientDetailsEntity = new ClientDetailsEntity();
            clientDetailsEntity.setClientId(accessKey);
            clientDetailsEntity.setArchived(0);
            clientDetailsEntity.setClientIconUri("");
            clientDetailsEntity.setClientName(accessKey);
            clientDetailsEntity.setClientSecret(secret);
            clientDetailsEntity.setClientUri("");
            clientDetailsEntity.setGrantTypes("client_credentials");
            clientDetailsEntity.setRedirectUri("");
            clientDetailsEntity.setDescription("AccessKey");
            clientDetailsEntity.setScope("basic");
            clientDetailsEntity.setTrusted(1);
            clientDetailsEntity.setRoles("user");
            clientDetailsEntity.setCreateTime(new Date());
            insert(clientDetailsEntity);
            clientToUser(clientDetailsEntity);
            return clientDetailsEntity;
        } else
            return null;
    }

    public void clientToUser() {
        clientToUser(null);
    }

    public synchronized void clientToUser(ClientDetailsEntity detailsEntity) {
        List<ClientDetailsEntity> list = null;
        if (null != detailsEntity) {
            list = new ArrayList<>();
            list.add(detailsEntity);
        } else
            list = baseMapper.selectByMap(null);
        for (ClientDetailsEntity clientDetailsEntity : list) {
            SysUserEntity sysUserEntity = sysUserService.getUsername(clientDetailsEntity.getClientId());
            if (null == sysUserEntity) {
                sysUserService.newUser(clientDetailsEntity.getClientId(), Uuid.uuid(), Uuid.uuid(), sysConfigService.getDefaultRoleKeys());
            }
        }
    }
}