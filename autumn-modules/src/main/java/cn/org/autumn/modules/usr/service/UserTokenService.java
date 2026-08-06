package cn.org.autumn.modules.usr.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.config.AccountHandler;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.oauth.oauth2.support.OAuthTokenResponse;
import cn.org.autumn.modules.oauth.oauth2.support.OAuthTokenResponseParser;
import cn.org.autumn.modules.usr.dao.UserTokenDao;
import cn.org.autumn.modules.usr.entity.UserTokenEntity;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class UserTokenService extends ModuleService<UserTokenDao, UserTokenEntity> implements AccountHandler {

    private final static int EXPIRE = 3600 * 12;

    /** Align AppApiUserLoginService /login/uniform long-lived API token TTL. */
    public static final int API_EXPIRE = 3600 * 24 * 365;

    @Override
    public String ico() {
        return "fa-eye-slash";
    }

    public UserTokenEntity queryByToken(String token) {
        return getToken(token);
    }

    public UserTokenEntity getUuid(String uuid) {
        if (StringUtils.isBlank(uuid))
            return null;
        return baseMapper.getUuid(uuid);
    }

    public UserTokenEntity getToken(String token) {
        if (StringUtils.isBlank(token))
            return null;
        return baseMapper.getToken(token);
    }

    public List<UserTokenEntity> getUser(String user) {
        if (StringUtils.isBlank(user))
            return null;
        return baseMapper.getUser(user);
    }

    public void saveToken(String token) {
        OAuthTokenResponse parsed = OAuthTokenResponseParser.parse(token);
        if (StringUtils.isBlank(parsed.getAccessToken())) {
            return;
        }
        UserTokenEntity userTokenEntity = queryByToken(parsed.getAccessToken());
        if (null == userTokenEntity) {
            userTokenEntity = new UserTokenEntity();
            if (ShiroUtils.isLogin()) {
                userTokenEntity.setUserUuid(ShiroUtils.getUserUuid());
            }
            userTokenEntity.setToken(parsed.getAccessToken());
        }
        if (StringUtils.isNotBlank(parsed.getRefreshToken())) {
            userTokenEntity.setRefreshToken(parsed.getRefreshToken());
        }
        if (parsed.getExpiresIn() > 0) {
            Date date = new Date();
            date.setTime(date.getTime() + parsed.getExpiresIn() * 1000);
            userTokenEntity.setExpireTime(date);
            userTokenEntity.setUpdateTime(date);
        }
        saveOrUpdate(userTokenEntity);
    }

    public UserTokenEntity createToken(String userUuid) {
        //当前时间
        Date now = new Date();
        //过期时间
        Date expireTime = new Date(now.getTime() + EXPIRE * 1000);
        //生成token
        String token = generateToken();
        //保存或更新用户token
        UserTokenEntity tokenEntity = new UserTokenEntity();
        tokenEntity.setUserUuid(userUuid);
        tokenEntity.setToken(token);
        tokenEntity.setUpdateTime(now);
        tokenEntity.setExpireTime(expireTime);
        this.saveOrUpdate(tokenEntity);
        return tokenEntity;
    }

    /**
     * Issue or refresh a long-lived API token (same physical token as account /login/uniform).
     */
    public UserTokenEntity createApiToken(String userUuid) {
        return createApiToken(userUuid, null);
    }

    /**
     * Issue or refresh a long-lived API token; bind {@code deviceUuid} to {@link UserTokenEntity#uuid} when present.
     */
    public UserTokenEntity createApiToken(String userUuid, String deviceUuid) {
        if (StringUtils.isBlank(userUuid)) {
            return null;
        }
        UserTokenEntity entity = null;
        if (StringUtils.isNotBlank(deviceUuid)) {
            entity = getUuid(deviceUuid);
            if (entity == null) {
                List<UserTokenEntity> list = getUser(userUuid);
                if (list != null) {
                    for (UserTokenEntity row : list) {
                        if (StringUtils.isBlank(row.getUuid())) {
                            entity = row;
                            break;
                        }
                    }
                }
            }
        } else {
            List<UserTokenEntity> list = getUser(userUuid);
            if (list != null && !list.isEmpty()) {
                entity = list.get(0);
            }
        }
        if (entity == null) {
            entity = new UserTokenEntity();
            entity.setUserUuid(userUuid);
        }
        if (StringUtils.isNotBlank(deviceUuid)) {
            entity.setUuid(deviceUuid);
        }
        Date now = new Date();
        entity.setToken(generateToken());
        entity.setUpdateTime(now);
        entity.setExpireTime(new Date(now.getTime() + API_EXPIRE * 1000L));
        saveOrUpdate(entity);
        return entity;
    }

    public void expireToken(String userUuid) {
        Date now = new Date();
        UserTokenEntity tokenEntity = new UserTokenEntity();
        tokenEntity.setUserUuid(userUuid);
        tokenEntity.setUpdateTime(now);
        tokenEntity.setExpireTime(now);
        this.saveOrUpdate(tokenEntity);
    }

    public void deleteUser(String user) {
        baseMapper.deleteUser(user);
    }

    public void deleteUuid(String user) {
        baseMapper.deleteUuid(user);
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public void canceled(Account obj) {
        if (null != obj)
            baseMapper.deleteUser(obj.getUuid());
    }
}
