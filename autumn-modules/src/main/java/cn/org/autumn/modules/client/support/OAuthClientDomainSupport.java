package cn.org.autumn.modules.client.support;

import cn.org.autumn.config.ClientType;
import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.client.oauth2.WebOauthEndpointResolver;
import cn.org.autumn.modules.oauth.entity.ClientDetailsEntity;
import cn.org.autumn.utils.Uuid;
import org.apache.commons.lang3.StringUtils;

/**
 * OAuth 客户端域名相关工具。
 * <p>
 * 启动/升级阶段仅做 uuid 等无害迁移；换绑域名须由 WebOauthCombine 管理端显式触发，
 * 按同实例标准路径重建 endpoint 与 redirectUri（与 {@code create()} 一致）。
 */
public final class OAuthClientDomainSupport {

    private static final String PATH_REDIRECT_CALLBACK = "/client/oauth2/callback";

    private OAuthClientDomainSupport() {
    }

    public static boolean isSiteDefault(ClientType clientType) {
        return ClientType.SiteDefault == clientType;
    }

    public static String ensureUuid(String uuid) {
        return StringUtils.isBlank(uuid) ? Uuid.uuid() : uuid.trim();
    }

    /**
     * WebOauthCombine 显式换绑：按新域名重建 RP 侧 OAuth endpoint 与 redirectUri。
     */
    public static void applyRpDomainRebind(WebAuthenticationEntity entity, String scheme, String host) {
        if (entity == null || StringUtils.isBlank(host)) {
            return;
        }
        String baseUrl = buildBaseUrl(scheme, host);
        entity.setRedirectUri(baseUrl + PATH_REDIRECT_CALLBACK);
        entity.setAuthorizeUri(baseUrl + WebOauthEndpointResolver.PATH_AUTHORIZE);
        entity.setAccessTokenUri(baseUrl + WebOauthEndpointResolver.PATH_TOKEN);
        entity.setUserInfoUri(baseUrl + WebOauthEndpointResolver.PATH_USER_INFO);
        entity.setName(host);
        entity.setDescription(host);
        entity.setClientId(host);
    }

    /**
     * WebOauthCombine 显式换绑：按新域名重建 AS 侧 clientUri、redirectUri 与展示名；不修改 clientIconUri。
     * clientId 换绑由调用方在占用校验后执行。
     */
    public static void applyAsDomainRebind(ClientDetailsEntity entity, String scheme, String host) {
        if (entity == null || StringUtils.isBlank(host)) {
            return;
        }
        String baseUrl = buildBaseUrl(scheme, host);
        entity.setClientUri(baseUrl);
        entity.setRedirectUri(baseUrl + PATH_REDIRECT_CALLBACK);
        entity.setClientName(host);
        entity.setDescription(host);
    }

    static String buildBaseUrl(String scheme, String host) {
        String normalizedScheme = StringUtils.defaultIfBlank(scheme, "http").trim().toLowerCase();
        if (normalizedScheme.endsWith("://")) {
            normalizedScheme = normalizedScheme.substring(0, normalizedScheme.length() - 3);
        }
        return normalizedScheme + "://" + host.trim();
    }
}
