package cn.org.autumn.modules.opl.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.opl.OplConstants;
import cn.org.autumn.modules.opl.dao.OpenTokenDao;
import cn.org.autumn.opl.model.OpenIdentitySnapshot;
import cn.org.autumn.modules.opl.entity.OpenCodeEntity;
import cn.org.autumn.modules.opl.entity.OpenTokenEntity;
import cn.org.autumn.modules.opl.store.OplTokenContext;
import cn.org.autumn.utils.RedisUtils;
import cn.org.autumn.utils.Uuid;
import com.qiniu.util.Md5;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpenTokenService extends ModuleService<OpenTokenDao, OpenTokenEntity> {

    private static final String REDIS_PREFIX = "opl:token:";

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private OpenIdentityService openIdentityService;

    private final Map<String, OplTokenContext> localCache = new HashMap<>();

    @Transactional(rollbackFor = Exception.class)
    public OplTokenContext issueFromCode(OpenCodeEntity codeEntity) {
        if (codeEntity == null) {
            throw new IllegalArgumentException("授权码无效");
        }
        OpenIdentitySnapshot identity = openIdentityService.resolveOrCreate(codeEntity.getAppId(), codeEntity.getUser());
        String accessToken = generateToken("at");
        String refreshToken = generateToken("rt");
        OplTokenContext context = new OplTokenContext(codeEntity.getAppId(), codeEntity.getUser(), identity.getOpenId(), identity.getUnionId());
        context.setAuthCode(codeEntity.getCode());
        context.setAccessToken(accessToken);
        context.setRefreshToken(refreshToken);
        context.setExpireAt(new Date(System.currentTimeMillis() + OplConstants.ACCESS_TOKEN_TTL_SECONDS * 1000L));
        putContext(ValueType.authCode, codeEntity.getCode(), context, OplConstants.AUTH_CODE_TTL_SECONDS);
        putContext(ValueType.accessToken, accessToken, context, OplConstants.ACCESS_TOKEN_TTL_SECONDS);
        putContext(ValueType.refreshToken, refreshToken, context, OplConstants.REFRESH_TOKEN_TTL_SECONDS);
        persistToken(context);
        removeContext(ValueType.authCode, codeEntity.getCode());
        return context;
    }

    @Transactional(rollbackFor = Exception.class)
    public OplTokenContext refresh(String refreshToken) {
        OplTokenContext context = getContext(ValueType.refreshToken, refreshToken);
        if (context == null) {
            return null;
        }
        removeContext(ValueType.accessToken, context.getAccessToken());
        removeContext(ValueType.refreshToken, refreshToken);
        OpenTokenEntity stored = baseMapper.getByRefreshToken(refreshToken);
        if (stored != null) {
            deleteById(stored.getId());
        }
        String newAccess = generateToken("at");
        String newRefresh = generateToken("rt");
        context.setAccessToken(newAccess);
        context.setRefreshToken(newRefresh);
        context.setExpireAt(new Date(System.currentTimeMillis() + OplConstants.ACCESS_TOKEN_TTL_SECONDS * 1000L));
        putContext(ValueType.accessToken, newAccess, context, OplConstants.ACCESS_TOKEN_TTL_SECONDS);
        putContext(ValueType.refreshToken, newRefresh, context, OplConstants.REFRESH_TOKEN_TTL_SECONDS);
        persistToken(context);
        return context;
    }

    public OplTokenContext getByAccessToken(String accessToken) {
        return getContext(ValueType.accessToken, accessToken);
    }

    public boolean isValidAccessToken(String accessToken) {
        return getByAccessToken(accessToken) != null;
    }

    public boolean isValidRefreshToken(String refreshToken) {
        return getContext(ValueType.refreshToken, refreshToken) != null;
    }

    private void persistToken(OplTokenContext context) {
        OpenTokenEntity entity = new OpenTokenEntity();
        entity.setAccessToken(context.getAccessToken());
        entity.setRefreshToken(context.getRefreshToken());
        entity.setAuthCode(context.getAuthCode());
        entity.setAppId(context.getAppId());
        entity.setUser(context.getUser());
        entity.setOpenId(context.getOpenId());
        entity.setUnionId(context.getUnionId());
        entity.setAccessExpireIn(OplConstants.ACCESS_TOKEN_TTL_SECONDS);
        entity.setRefreshExpireIn(OplConstants.REFRESH_TOKEN_TTL_SECONDS);
        entity.setUpdateTime(new Date());
        insert(entity);
    }

    private void putContext(ValueType type, String key, OplTokenContext context, long expireSeconds) {
        if (StringUtils.isBlank(key) || context == null) {
            return;
        }
        String redisKey = redisKey(type, key);
        if (redisUtils.isOpen()) {
            redisUtils.set(redisKey, context, expireSeconds);
        } else {
            localCache.put(redisKey, context);
        }
    }

    private OplTokenContext getContext(ValueType type, String key) {
        if (StringUtils.isBlank(key)) {
            return null;
        }
        String redisKey = redisKey(type, key);
        if (redisUtils.isOpen()) {
            Object value = redisUtils.get(redisKey);
            if (value instanceof OplTokenContext) {
                OplTokenContext context = (OplTokenContext) value;
                if (!context.isExpired()) {
                    return context;
                }
            }
        } else {
            OplTokenContext context = localCache.get(redisKey);
            if (context != null) {
                if (!context.isExpired()) {
                    return context;
                }
                localCache.remove(redisKey);
            }
        }
        return loadFromDb(type, key);
    }

    private OplTokenContext loadFromDb(ValueType type, String key) {
        OpenTokenEntity entity = null;
        if (type == ValueType.accessToken) {
            entity = baseMapper.getByAccessToken(key);
        } else if (type == ValueType.refreshToken) {
            entity = baseMapper.getByRefreshToken(key);
        } else if (type == ValueType.authCode) {
            entity = baseMapper.getByAuthCode(key);
        }
        if (entity == null || entity.getUpdateTime() == null) {
            return null;
        }
        long expireMs = type == ValueType.refreshToken ? entity.getRefreshExpireIn() * 1000L : entity.getAccessExpireIn() * 1000L;
        Date expireAt = new Date(entity.getUpdateTime().getTime() + expireMs);
        if (!expireAt.after(new Date())) {
            return null;
        }
        OplTokenContext context = new OplTokenContext(entity.getAppId(), entity.getUser(), entity.getOpenId(), entity.getUnionId());
        context.setAuthCode(entity.getAuthCode());
        context.setAccessToken(entity.getAccessToken());
        context.setRefreshToken(entity.getRefreshToken());
        context.setExpireAt(expireAt);
        return context;
    }

    private void removeContext(ValueType type, String key) {
        if (StringUtils.isBlank(key)) {
            return;
        }
        String redisKey = redisKey(type, key);
        if (redisUtils.isOpen()) {
            redisUtils.delete(redisKey);
        } else {
            localCache.remove(redisKey);
        }
    }

    private String redisKey(ValueType type, String key) {
        return REDIS_PREFIX + type.name() + ":" + key;
    }

    private String generateToken(String prefix) {
        return prefix + "_" + Md5.md5((Uuid.uuid() + System.nanoTime()).getBytes());
    }

    public enum ValueType {
        authCode, accessToken, refreshToken
    }
}
