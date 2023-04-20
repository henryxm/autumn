package cn.org.autumn.modules.oauth.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.modules.oauth.dao.TokenStoreDao;
import cn.org.autumn.modules.oauth.entity.TokenStoreEntity;
import cn.org.autumn.modules.oauth.store.ValueType;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

import static cn.org.autumn.modules.oauth.service.ClientDetailsService.ACCESS_TOKEN_DEFAULT_EXPIRED_IN;
import static cn.org.autumn.modules.oauth.service.ClientDetailsService.REFRESH_TOKEN_DEFAULT_EXPIRED_IN;

@Service
public class TokenStoreService extends ModuleService<TokenStoreDao, TokenStoreEntity> {

    @Autowired
    @Lazy
    ClientDetailsService clientDetailsService;

    @Autowired
    @Lazy
    SysUserService sysUserService;

    @Override
    public String ico() {
        return "fa-tumblr-square";
    }

    public void init() {
        load();
        super.init();
    }

    public TokenStoreEntity findByUserUuid(String userUuid) {
        return baseMapper.findByUserUuid(userUuid);
    }

    public TokenStoreEntity findByAuthCode(String authCode) {
        return baseMapper.findByAuthCode(authCode);
    }

    public TokenStoreEntity findByAccessToken(String accessToken) {
        return baseMapper.findByAccessToken(accessToken);
    }

    public TokenStoreEntity findByRefreshToken(String refreshToken) {
        return baseMapper.findByRefreshToken(refreshToken);
    }

    public TokenStoreEntity findByUser(SysUserEntity sysUserEntity) {
        return findByUserUuid(sysUserEntity.getUuid());
    }

    public TokenStoreEntity saveOrUpdate(SysUserEntity sysUserEntity, String accessToken, String refreshToken, String authCode, Long accessTokenExpiredIn, Long refreshTokenExpiredIn) {
        TokenStoreEntity tokenStoreEntity = findByUser(sysUserEntity);
        if (null == tokenStoreEntity) {
            tokenStoreEntity = new TokenStoreEntity();
            tokenStoreEntity.setUserUuid(sysUserEntity.getUuid());
        }
        if (null == tokenStoreEntity.getCreateTime())
            tokenStoreEntity.setCreateTime(new Date());
        tokenStoreEntity.setAuthCode(authCode);
        tokenStoreEntity.setAccessToken(accessToken);
        tokenStoreEntity.setRefreshToken(refreshToken);
        tokenStoreEntity.setAccessTokenExpiredIn(accessTokenExpiredIn);
        tokenStoreEntity.setRefreshTokenExpiredIn(refreshTokenExpiredIn);
        tokenStoreEntity.setUpdateTime(new Date());
        insertOrUpdate(tokenStoreEntity);
        return tokenStoreEntity;
    }

    public TokenStoreEntity saveOrUpdate(TokenStoreEntity tokenStoreEntity, String accessToken, String refreshToken, Long accessTokenExpiredIn, Long refreshTokenExpiredIn) {
        if (null == tokenStoreEntity)
            return null;
        tokenStoreEntity.setAccessToken(accessToken);
        tokenStoreEntity.setRefreshToken(refreshToken);
        if (null != accessTokenExpiredIn)
            tokenStoreEntity.setAccessTokenExpiredIn(accessTokenExpiredIn);
        if (null != refreshTokenExpiredIn)
            tokenStoreEntity.setRefreshTokenExpiredIn(refreshTokenExpiredIn);
        if (null == tokenStoreEntity.getCreateTime())
            tokenStoreEntity.setCreateTime(new Date());
        tokenStoreEntity.setUpdateTime(new Date());
        insertOrUpdate(tokenStoreEntity);
        return tokenStoreEntity;
    }

    public TokenStoreEntity saveOrUpdate(TokenStoreEntity tokenStoreEntity, String accessToken, String refreshToken) {
        return saveOrUpdate(tokenStoreEntity, accessToken, refreshToken, ACCESS_TOKEN_DEFAULT_EXPIRED_IN, REFRESH_TOKEN_DEFAULT_EXPIRED_IN);
    }

    public TokenStoreEntity saveOrUpdate(SysUserEntity sysUserEntity, String accessToken, String refreshToken, String authCode) {
        return saveOrUpdate(sysUserEntity, accessToken, refreshToken, authCode, ACCESS_TOKEN_DEFAULT_EXPIRED_IN, REFRESH_TOKEN_DEFAULT_EXPIRED_IN);
    }

    public boolean isAccessTokenExpired(TokenStoreEntity tokenStoreEntity) {
        Date date = tokenStoreEntity.getCreateTime();
        date.setTime(date.getTime() + tokenStoreEntity.getAccessTokenExpiredIn() * 1000);
        return date.before(new Date());
    }

    public boolean isRefreshTokenExpired(TokenStoreEntity tokenStoreEntity) {
        Date date = tokenStoreEntity.getCreateTime();
        date.setTime(date.getTime() + tokenStoreEntity.getRefreshTokenExpiredIn() * 1000);
        return date.before(new Date());
    }

    /**
     * 根据创建时间计算 Access token 剩余的有效时间
     *
     * @param tokenStoreEntity
     * @return
     */
    public Long getAccessTokenExpiredIn(TokenStoreEntity tokenStoreEntity) {
        Date date = tokenStoreEntity.getCreateTime();
        Date now = new Date();
        Long lest = date.getTime() + tokenStoreEntity.getAccessTokenExpiredIn() * 1000 - now.getTime();
        if (lest > 0) {
            return lest / 1000;
        }
        return 0L;
    }

    /**
     * 根据创建时间计算 Refresh token 剩余的有效时间
     *
     * @param tokenStoreEntity
     * @return
     */
    public Long getRefreshTokenExpiredIn(TokenStoreEntity tokenStoreEntity) {
        Date date = tokenStoreEntity.getCreateTime();
        Date now = new Date();
        Long lest = date.getTime() + tokenStoreEntity.getRefreshTokenExpiredIn() * 1000 - now.getTime();
        if (lest > 0) {
            return lest / 1000;
        }
        return 0L;
    }

    public void load() {
        List<TokenStoreEntity> list = selectByMap(null);
        for (TokenStoreEntity tokenStoreEntity : list) {
            SysUserEntity sysUserEntity = sysUserService.selectById(tokenStoreEntity.getUserUuid());
            if (null != sysUserEntity) {
                Long at = getAccessTokenExpiredIn(tokenStoreEntity);
                if (at > 0)
                    clientDetailsService.put(ValueType.accessToken, null, tokenStoreEntity.getAccessToken(), tokenStoreEntity.getRefreshToken(), sysUserEntity, at);
                Long rt = getRefreshTokenExpiredIn(tokenStoreEntity);
                if (rt > 0) {
                    clientDetailsService.put(ValueType.refreshToken, null, tokenStoreEntity.getAccessToken(), tokenStoreEntity.getRefreshToken(), sysUserEntity, rt);
                }
            }
        }
    }
}
