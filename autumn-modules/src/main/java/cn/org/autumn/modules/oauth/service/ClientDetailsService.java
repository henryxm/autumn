package cn.org.autumn.modules.oauth.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.cluster.UserHandler;
import cn.org.autumn.config.ClientType;
import cn.org.autumn.config.DomainHandler;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.oauth.dao.ClientDetailsDao;
import cn.org.autumn.modules.oauth.entity.ClientDetailsEntity;
import cn.org.autumn.modules.client.support.OAuthClientDomainSupport;
import cn.org.autumn.modules.oauth.entity.TokenStoreEntity;
import cn.org.autumn.modules.oauth.store.TokenStore;
import cn.org.autumn.modules.oauth.store.ValueType;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.site.UpgradeFactory;
import cn.org.autumn.utils.RedisUtils;
import cn.org.autumn.utils.Uuid;
import com.qiniu.util.Md5;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class ClientDetailsService extends ModuleService<ClientDetailsDao, ClientDetailsEntity> implements LoopJob.OneHour, UpgradeFactory.Domain, DomainHandler {

    @Autowired
    RedisUtils redisUtils;

    @Autowired
    @Lazy
    TokenStoreService tokenStoreService;

    @Autowired
    @Lazy
    SysUserService sysUserService;

    @Autowired
    @Lazy
    SysConfigService sysConfigService;

    @Autowired
    AsyncTaskExecutor asyncTaskExecutor;

    @Autowired(required = false)
    List<UserHandler> userHandlers;

    public static final Long AUTH_CODE_DEFAULT_EXPIRED_IN = 5 * 60L;
    public static final Long ACCESS_TOKEN_DEFAULT_EXPIRED_IN = 24 * 60 * 60L;
    public static final Long REFRESH_TOKEN_DEFAULT_EXPIRED_IN = 7 * 24 * 60 * 60L;

    public static final String allChar = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    Map<String, TokenStore> cache = new HashMap<>();

    public String getKey(ValueType type, String code) {
        return type.getValue() + code;
    }

    public void put(ValueType type, String code, String accessToken, String refreshToken, Object user, Long expire) {
        put(type, code, accessToken, refreshToken, user, expire, null);
    }

    public void put(ValueType type, String code, String accessToken, String refreshToken, Object user, Long expire, String grantedScope) {
        put(type, code, accessToken, refreshToken, user, expire, grantedScope, null);
    }

    public void put(ValueType type, String code, String accessToken, String refreshToken, Object user, Long expire, String grantedScope, String clientId) {
        String v = code;
        if (type == ValueType.accessToken)
            v = accessToken;
        if (type == ValueType.refreshToken)
            v = refreshToken;

        String key = getKey(type, v);
        TokenStore tokenStore = new TokenStore(user, code, accessToken, refreshToken, grantedScope, expire);
        tokenStore.setClientId(clientId);
        if (redisUtils.isOpen()) {
            redisUtils.set(key, tokenStore, expire);
        } else {
            if (cache.containsKey(key))
                cache.replace(key, tokenStore);
            else
                cache.put(key, tokenStore);
        }
    }

    public void putAuthCode(String code, Object user) {
        putAuthCode(code, user, null);
    }

    public void putAuthCode(String code, Object user, String grantedScope) {
        putAuthCode(code, user, grantedScope, null);
    }

    public void putAuthCode(String code, Object user, String grantedScope, String clientId) {
        put(ValueType.authCode, code, null, null, user, AUTH_CODE_DEFAULT_EXPIRED_IN, grantedScope, clientId);
    }

    /**
     * 保存Token 并删除 authCode
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
                if (!tokenStoreEntity.getAccessToken().equals(accessToken)) {
                    remove(ValueType.accessToken, tokenStoreEntity.getAccessToken());
                }
                if (!tokenStoreEntity.getRefreshToken().equals(refreshToken)) {
                    remove(ValueType.refreshToken, tokenStoreEntity.getRefreshToken());
                }
            }
        }
        String grantedScope = tokenStore == null ? null : tokenStore.getGrantedScope();
        String clientId = tokenStore == null ? null : tokenStore.getClientId();
        put(ValueType.accessToken, null, accessToken, refreshToken, v, ACCESS_TOKEN_DEFAULT_EXPIRED_IN, grantedScope, clientId);
        put(ValueType.refreshToken, null, accessToken, refreshToken, v, REFRESH_TOKEN_DEFAULT_EXPIRED_IN, grantedScope, clientId);
        if (null != tokenStore)
            remove(ValueType.authCode, tokenStore.getAuthCode());
        if (null != sysUserEntity) {
            tokenStoreService.saveOrUpdate(sysUserEntity, accessToken, refreshToken, tokenStore.getAuthCode(), ACCESS_TOKEN_DEFAULT_EXPIRED_IN, REFRESH_TOKEN_DEFAULT_EXPIRED_IN, tokenStore.getGrantedScope());
        }
    }

    public TokenStore get(ValueType type, String code) {
        String key = getKey(type, code);
        TokenStore tokenStore = null;
        if (redisUtils.isOpen()) {
            Object object = redisUtils.get(key);
            if (object instanceof TokenStore)
                tokenStore = (TokenStore) object;
        } else {
            TokenStore od = cache.get(key);
            if (null != od && !od.isExpired())
                tokenStore = od;
        }
        if (null == tokenStore) {
            TokenStoreEntity storeEntity = null;
            Long expire = null;
            SysUserEntity sysUserEntity = null;
            if (type.equals(ValueType.accessToken)) {
                storeEntity = tokenStoreService.findByAccessToken(code);
            } else if (type.equals(ValueType.refreshToken)) {
                storeEntity = tokenStoreService.findByRefreshToken(code);
            } else if (type.equals(ValueType.authCode)) {
                storeEntity = tokenStoreService.findByAuthCode(code);
            }
            if (null != storeEntity) {
                sysUserEntity = sysUserService.getByUuid(storeEntity.getUserUuid());
                if (null != sysUserEntity) {
                    Date date = storeEntity.getUpdateTime();
                    if (null == date) {
                        date = new Date();
                        storeEntity.setUpdateTime(date);
                        tokenStoreService.updateById(storeEntity);
                    }
                    expire = (date.getTime() + storeEntity.getAccessTokenExpiredIn() * 1000 - (new Date().getTime())) / 1000;
                    if (expire > 0)
                        tokenStore = new TokenStore(sysUserEntity, storeEntity.getAuthCode(), storeEntity.getAccessToken(), storeEntity.getRefreshToken(), storeEntity.getScope(), expire);
                }
            }
            if (null != tokenStore) {
                put(type, storeEntity.getAuthCode(), storeEntity.getAccessToken(), storeEntity.getRefreshToken(), sysUserEntity, expire, storeEntity.getScope());
            }
        }
        return tokenStore;
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
    public String ico() {
        return "fa-reddit-alien";
    }

    public ClientDetailsEntity findByClientId(String clientId) {
        return baseMapper.findByClientId(clientId);
    }

    public boolean isValidClientSecret(String clientSecret) {
        return baseMapper.findByClientSecret(clientSecret) != null;
    }

    /**
     * 遗留 client_credentials：优先校验当前 clientId 的 secret，其次全局 secret 表（历史行为）。
     */
    public boolean acceptsClientSecret(String clientId, String clientSecret) {
        if (StringUtils.isBlank(clientSecret))
            return false;
        ClientDetailsEntity client = findByClientId(clientId);
        if (client != null && StringUtils.isNotBlank(client.getClientSecret()) && client.getClientSecret().equals(clientSecret))
            return true;
        return isValidClientSecret(clientSecret);
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
        ensureSiteDefaultClient();
    }

    private void ensureSiteDefaultClient() {
        create(sysConfigService.getClientId(), sysConfigService.getClientSecret(), "默认的客户端", "默认的客户端");
        updateClientType(sysConfigService.getClientId(), ClientType.SiteDefault);
    }

    @Override
    public void onOneHour() {
        Iterator<String> iterator = cache.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            TokenStore od = cache.get(key);
            if (od.isExpired()) {
                iterator.remove();
            }
        }
    }

    public static String newKey(String prefix) {
        int length = 10;
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(allChar.charAt(random.nextInt(allChar.length())));
        }
        return prefix + sb;
    }

    public ClientDetailsEntity create(String clientId, String secret, String name, String description) {
        ClientType type = baseMapper.countClientType(ClientType.SiteDefault) > 0 ? null : ClientType.SiteDefault;
        return create(sysConfigService.getBaseUrl(), clientId, secret, type, name, description);
    }

    public ClientDetailsEntity create(String baseUrl, String clientId, String secret, ClientType clientType, String name, String description) {
        if (StringUtils.isBlank(clientId))
            return null;
        if (StringUtils.isBlank(secret))
            secret = Uuid.uuid();
        ClientDetailsEntity clientDetailsEntity = findByClientId(clientId);
        if (null == clientDetailsEntity) {
            clientDetailsEntity = new ClientDetailsEntity();
            clientDetailsEntity.setUuid(Uuid.uuid());
            clientDetailsEntity.setClientId(clientId);
            clientDetailsEntity.setArchived(0);
            clientDetailsEntity.setClientIconUri("");
            clientDetailsEntity.setClientName(name);
            clientDetailsEntity.setClientSecret(secret);
            clientDetailsEntity.setClientUri(baseUrl);
            clientDetailsEntity.setGrantTypes("all");
            clientDetailsEntity.setResourceIds("all");
            clientDetailsEntity.setRedirectUri(baseUrl + "/client/oauth2/callback");
            clientDetailsEntity.setDescription(description);
            clientDetailsEntity.setScope("basic");
            clientDetailsEntity.setTrusted(1);
            clientDetailsEntity.setRoles("user");
            clientDetailsEntity.setClientType(clientType);
            clientDetailsEntity.setCreateTime(new Date());
            save(clientDetailsEntity);
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
            clientDetailsEntity.setUuid(Uuid.uuid());
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
            save(clientDetailsEntity);
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
            list = list();
        for (ClientDetailsEntity clientDetailsEntity : list) {
            SysUserEntity sysUserEntity = sysUserService.getUsername(clientDetailsEntity.getClientId());
            if (null == sysUserEntity) {
                sysUserService.newUser(clientDetailsEntity.getClientId(), Uuid.uuid(), Uuid.uuid(), sysConfigService.getDefaultRoleKeys());
            }
        }
    }

    public void updateClientType(String clientId, ClientType clientType) {
        baseMapper.updateClientType(clientId, clientType);
    }

    /**
     * 域名变更升级钩子：仅补全缺失 uuid，不修改 redirectUri、clientUri、clientId 等 OAuth 配置。
     */
    @Override
    public void onDomainChanged() {
        backfillMissingUuids();
    }

    private void backfillMissingUuids() {
        List<ClientDetailsEntity> entities = list();
        for (ClientDetailsEntity entity : entities) {
            if (StringUtils.isBlank(entity.getUuid())) {
                entity.setUuid(OAuthClientDomainSupport.ensureUuid(null));
                updateById(entity);
            }
        }
    }

    /**
     * WebOauthCombine 管理端显式换绑域名时调用；按标准路径重建 OAuth 配置。
     */
    public void rebindToDomain(String uuid, String domain) {
        ClientDetailsEntity entity = baseMapper.getByUuid(uuid);
        if (entity == null || StringUtils.isBlank(domain)) {
            return;
        }
        String host = domain.trim();
        if (StringUtils.isBlank(entity.getUuid())) {
            entity.setUuid(OAuthClientDomainSupport.ensureUuid(null));
        }
        OAuthClientDomainSupport.applyAsDomainRebind(entity, sysConfigService.getScheme(), host);
        if (!Objects.equals(entity.getClientId(), host)) {
            ClientDetailsEntity occupied = findByClientId(host);
            if (occupied != null && !Objects.equals(occupied.getId(), entity.getId())) {
                entity.setClientType(null);
                updateById(entity);
                return;
            }
            entity.setClientId(host);
        }
        updateById(entity);
    }

    /** 开放扫码（pageLogin 含 QR）时同步 AS 侧 oauth_client_details，使 appId 可用于 QRC open/create。 */
    public ClientDetailsEntity ensureQrcOpenClient(String baseUrl, String clientId, String secret, String redirectUri, String name) {
        if (StringUtils.isBlank(clientId) || StringUtils.isBlank(secret)) {
            return null;
        }
        String normalizedRedirect = StringUtils.trimToEmpty(redirectUri);
        String normalizedBase = StringUtils.defaultIfBlank(baseUrl, "");
        String displayName = StringUtils.defaultIfBlank(name, clientId);
        ClientDetailsEntity entity = findByClientId(clientId);
        if (entity == null) {
            entity = new ClientDetailsEntity();
            entity.setUuid(Uuid.uuid());
            entity.setClientId(clientId);
            entity.setArchived(0);
            entity.setClientIconUri("");
            entity.setClientName(displayName);
            entity.setClientSecret(secret);
            entity.setClientUri(normalizedBase);
            entity.setGrantTypes("all");
            entity.setResourceIds("all");
            entity.setRedirectUri(normalizedRedirect);
            entity.setDescription(displayName);
            entity.setScope("basic");
            entity.setTrusted(1);
            entity.setRoles("user");
            entity.setCreateTime(new Date());
            insert(entity);
            clientToUser(entity);
            return entity;
        }
        boolean changed = false;
        if (!secret.equals(entity.getClientSecret())) {
            entity.setClientSecret(secret);
            changed = true;
        }
        if (StringUtils.isNotBlank(normalizedRedirect) && !normalizedRedirect.equalsIgnoreCase(StringUtils.defaultString(entity.getRedirectUri()))) {
            entity.setRedirectUri(normalizedRedirect);
            changed = true;
        }
        if (StringUtils.isNotBlank(normalizedBase) && !normalizedBase.equalsIgnoreCase(StringUtils.defaultString(entity.getClientUri()))) {
            entity.setClientUri(normalizedBase);
            changed = true;
        }
        if (entity.getTrusted() == null || entity.getTrusted() == 0) {
            entity.setTrusted(1);
            changed = true;
        }
        if (changed) {
            updateById(entity);
        }
        return entity;
    }

    @Override
    public boolean isSiteDomain(String domain) {
        return baseMapper.count(domain) > 0;
    }
}