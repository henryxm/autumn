package cn.org.autumn.modules.oauth.service;

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
import com.qiniu.util.Md5;
import org.springframework.beans.factory.annotation.Autowired;
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

    public void init() {
        super.init();
        LoopJob.onOneHour(this);
        ClientDetailsEntity clientDetailsEntity = findByClientId(sysConfigService.getClientId());
        if (null == clientDetailsEntity) {
            clientDetailsEntity = new ClientDetailsEntity();
            clientDetailsEntity.setClientId(sysConfigService.getClientId());
            clientDetailsEntity.setArchived(0);
            clientDetailsEntity.setClientIconUri("");
            clientDetailsEntity.setClientName("默认的客户端");
            clientDetailsEntity.setClientSecret(sysConfigService.getClientSecret());
            clientDetailsEntity.setClientUri(sysConfigService.getBaseUrl());
            clientDetailsEntity.setGrantTypes("all");
            clientDetailsEntity.setResourceIds("all");
            clientDetailsEntity.setRedirectUri(sysConfigService.getBaseUrl() + "/client/oauth2/callback");
            clientDetailsEntity.setDescription("默认的客户端");
            clientDetailsEntity.setScope("basic");
            clientDetailsEntity.setTrusted(1);
            clientDetailsEntity.setRoles("user");
            clientDetailsEntity.setCreateTime(new Date());
            insert(clientDetailsEntity);
        }
        clientToUser(clientDetailsEntity);
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
            ClientDetailsEntity finalClientDetailsEntity = clientDetailsEntity;
            asyncTaskExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    clientToUser(finalClientDetailsEntity);
                }
            });
            return clientDetailsEntity;
        } else
            return null;
    }

    public void clientToUser(ClientDetailsEntity detailsEntity) {
        List<ClientDetailsEntity> list = null;
        if (null != detailsEntity) {
            list = new ArrayList<>();
            list.add(detailsEntity);
        } else
            list = baseMapper.selectByMap(null);
        for (ClientDetailsEntity clientDetailsEntity : list) {
            SysUserEntity sysUserEntity = sysUserService.getByUsername(clientDetailsEntity.getClientId());
            if (null == sysUserEntity)
                sysUserService.newUser(clientDetailsEntity.getClientId(), Uuid.uuid(), Uuid.uuid(), sysConfigService.getDefaultRoleKeys());
        }
    }
}
