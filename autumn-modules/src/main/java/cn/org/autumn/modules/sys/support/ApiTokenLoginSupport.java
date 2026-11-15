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
import org.apache.commons.lang3.StringUtils;

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
        OAuthTokenInfo info = resolveOAuth(token);
        return info == null ? "" : info.getUserUuid();
    }

    /**
     * 从 OAuth access_token 解析 client_id（即 accessKeyId）；无效则空串。
     */
    public static String resolveOAuthClientId(String token) {
        OAuthTokenInfo info = resolveOAuth(token);
        return info == null ? "" : info.getClientId();
    }

    /**
     * 一次查找 TokenStore，同时得到 userUuid 与 clientId（避免重复 get/isValid）。
     */
    public static OAuthTokenInfo resolveOAuth(String token) {
        TokenStore store = resolveOAuthTokenStore(token);
        if (store == null)
            return null;
        String uuid = "";
        Object value = store.getValue();
        if (value instanceof SysUserEntity)
            uuid = StringUtils.defaultString(((SysUserEntity) value).getUuid());
        return new OAuthTokenInfo(uuid, StringUtils.defaultString(store.getClientId()), store);
    }

    /**
     * 有效 OAuth access_token 对应的 {@link TokenStore}；无效返回 null。
     * 仅一次 {@code get}（与历史 {@code isValidAccessToken} 语义一致：存在即可）。
     */
    public static TokenStore resolveOAuthTokenStore(String token) {
        if (StringUtils.isBlank(token))
            return null;
        ClientDetailsService clientDetailsService = clientDetailsService();
        if (clientDetailsService == null)
            return null;
        return clientDetailsService.get(ValueType.accessToken, token);
    }

    /**
     * 令牌是否为有效 OAuth access_token（相对遗留 usr_user_token）。
     */
    public static boolean isOAuthAccessToken(String token) {
        return resolveOAuthTokenStore(token) != null;
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
