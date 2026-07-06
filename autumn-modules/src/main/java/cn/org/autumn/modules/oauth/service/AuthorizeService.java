package cn.org.autumn.modules.oauth.service;

import cn.org.autumn.modules.oauth.oauth2.support.OAuthAccessTokenResolver;
import cn.org.autumn.modules.oauth.store.TokenStore;
import cn.org.autumn.modules.oauth.store.ValueType;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthorizeService {

    @Autowired
    ClientDetailsService clientDetailsService;

    public SysUserEntity getAuthorized(HttpServletRequest request) {
        try {
            String accessTokenKey = OAuthAccessTokenResolver.resolve(request, OAuthAccessTokenResolver.Policy.LEGACY_JSON_WRAP);
            if (StringUtils.isBlank(accessTokenKey)) {
                return null;
            }
            if (clientDetailsService.isValidAccessToken(accessTokenKey)) {
                TokenStore tokenStore = clientDetailsService.get(ValueType.accessToken, accessTokenKey);
                Object user = tokenStore.getValue();
                if (user instanceof SysUserEntity) {
                    return (SysUserEntity) user;
                }
            }
        } catch (Exception e) {
            log.error("Get Authorized", e);
        }
        return null;
    }
}

