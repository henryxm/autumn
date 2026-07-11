package cn.org.autumn.modules.sys.support;

import cn.org.autumn.config.Config;
import cn.org.autumn.modules.oauth.service.ClientDetailsService;
import cn.org.autumn.modules.oauth.store.TokenStore;
import cn.org.autumn.modules.oauth.store.ValueType;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.usr.entity.UserTokenEntity;
import cn.org.autumn.modules.usr.service.UserTokenService;
import cn.org.autumn.site.UserTokenFactory;
import java.util.Date;
import org.apache.commons.lang.StringUtils;

/**
 * 服务间 / Open API 令牌登录兼容：OAuth access_token、JSON 包裹 token、usr_user_token / 业务工厂令牌。
 */
public final class ApiTokenLoginSupport {

    private ApiTokenLoginSupport() {
    }

    /**
     * 从 Authorization / Token 等原始值解析可用于登录校验的令牌（兼容整段 /oauth2/token JSON 响应作 Bearer）。
     */
    public static String normalizeLoginToken(String raw) {
        if (StringUtils.isBlank(raw))
            return "";
        return ApiAuthSupport.normalizeBearer(raw);
    }

    public static boolean isLoginableApiToken(String rawToken) {
        return StringUtils.isNotBlank(resolveUserUuid(rawToken));
    }

    /**
     * 解析 API 令牌对应用户 uuid：OAuth access_token 优先，其次 usr_user_token / UserTokenFactory。
     */
    public static String resolveUserUuid(String rawOrToken) {
        String token = normalizeLoginToken(rawOrToken);
        if (StringUtils.isBlank(token) && StringUtils.isNotBlank(rawOrToken))
            token = rawOrToken.trim();
        if (StringUtils.isBlank(token))
            return "";
        String oauthUuid = resolveOAuthUserUuid(token);
        if (StringUtils.isNotBlank(oauthUuid))
            return oauthUuid;
        return resolveLegacyUserUuid(token);
    }

    /**
     * 按 OAuth2 client_credentials 等签发的 access_token 解析用户 uuid。
     */
    public static String resolveOAuthUserUuid(String token) {
        if (StringUtils.isBlank(token))
            return "";
        ClientDetailsService clientDetailsService = clientDetailsService();
        if (clientDetailsService == null || !clientDetailsService.isValidAccessToken(token))
            return "";
        TokenStore store = clientDetailsService.get(ValueType.accessToken, token);
        if (store == null || store.getValue() == null)
            return "";
        Object value = store.getValue();
        if (value instanceof SysUserEntity)
            return StringUtils.defaultString(((SysUserEntity) value).getUuid());
        return "";
    }

    /**
     * 按遗留 usr_user_token / UserTokenFactory 解析用户 uuid。
     */
    public static String resolveLegacyUserUuid(String token) {
        if (StringUtils.isBlank(token))
            return "";
        UserTokenFactory factory = userTokenFactory();
        if (factory != null) {
            String userUuid = factory.getUser(token);
            if (StringUtils.isNotBlank(userUuid))
                return userUuid;
        }
        UserTokenService userTokenService = userTokenService();
        if (userTokenService == null)
            return "";
        UserTokenEntity entity = userTokenService.getToken(token);
        if (entity == null)
            return "";
        if (entity.getExpireTime() != null && entity.getExpireTime().before(new Date()))
            return "";
        return StringUtils.defaultString(entity.getUserUuid());
    }

    private static ClientDetailsService clientDetailsService() {
        try {
            return (ClientDetailsService) Config.getBean("clientDetailsService");
        } catch (Exception ignored) {
            return null;
        }
    }

    private static UserTokenFactory userTokenFactory() {
        try {
            return (UserTokenFactory) Config.getBean("userTokenFactory");
        } catch (Exception ignored) {
            return null;
        }
    }

    private static UserTokenService userTokenService() {
        try {
            return (UserTokenService) Config.getBean("userTokenService");
        } catch (Exception ignored) {
            return null;
        }
    }
}
