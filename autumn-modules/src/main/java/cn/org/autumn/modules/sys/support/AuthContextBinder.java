package cn.org.autumn.modules.sys.support;

import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.web.AuthContext;
import cn.org.autumn.web.AuthContexts;
import cn.org.autumn.web.AuthSource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;

/**
 * 统一组装并绑定 {@link AuthContext}。
 * <p>
 * 负责：Session 与 API 凭证并存时补齐 accessKey；调用方勿重复查 TokenStore。
 */
public final class AuthContextBinder {

    private AuthContextBinder() {
    }

    /**
     * Session 富化结果：可能提升的 {@link AuthSource} + 补齐后的 accessKey。
     */
    public static final class EnrichResult {
        private final AuthSource source;
        private final String accessKey;

        public EnrichResult(AuthSource source, String accessKey) {
            this.source = source != null ? source : AuthSource.NONE;
            this.accessKey = accessKey == null ? "" : accessKey;
        }

        public AuthSource getSource() {
            return source;
        }

        public String getAccessKey() {
            return accessKey;
        }
    }

    /**
     * 已有 Session，但请求仍带 API 令牌时：若 OAuth 归属与会话一致（uuid 相同，或影子
     * {@code username == clientId}），则补齐 accessKey，并将 {@link AuthSource#SESSION} /
     * {@link AuthSource#NONE} 提升为 {@link AuthSource#OAUTH_TOKEN}。
     *
     * @param existingAccessKey 已有 accessKey（可空），非空时不覆盖
     */
    public static EnrichResult enrichSessionWithOAuth(AuthSource source, String sessionUserUuid, String sessionUsername, OAuthTokenInfo oauth, String existingAccessKey) {
        AuthSource next = source != null ? source : AuthSource.NONE;
        String accessKey = StringUtils.defaultString(existingAccessKey);
        if (oauth == null || !oauth.hasClientId())
            return new EnrichResult(next, accessKey);
        boolean sameUser = StringUtils.isNotBlank(sessionUserUuid) && sessionUserUuid.equals(oauth.getUserUuid());
        boolean shadowMatch = StringUtils.isNotBlank(sessionUsername) && sessionUsername.equals(oauth.getClientId());
        if (!sameUser && !shadowMatch)
            return new EnrichResult(next, accessKey);
        if (StringUtils.isBlank(accessKey))
            accessKey = oauth.getClientId();
        if (next == AuthSource.SESSION || next == AuthSource.NONE)
            next = AuthSource.OAUTH_TOKEN;
        return new EnrichResult(next, accessKey);
    }

    public static void bind(HttpServletRequest request, ApiCredential credential, String hintUuid, AuthSource source, String userUuid, String actorUuid, String accessKey) {
        if (request == null || StringUtils.isBlank(userUuid))
            return;
        String header = credential != null ? credential.getHeader() : "";
        String token = credential != null ? credential.getToken() : "";
        AuthContexts.bind(request, source, userUuid, actorUuid, accessKey, header, token, hintUuid);
    }

    /** client_credentials 影子账号：username 非空且不等于 uuid */
    public static boolean looksLikeAccessKeyShadow(SysUserEntity entity) {
        if (entity == null)
            return false;
        return looksLikeAccessKeyShadow(entity.getUsername(), entity.getUuid());
    }

    public static boolean looksLikeAccessKeyShadow(String username, String uuid) {
        return StringUtils.isNotBlank(username) && StringUtils.isNotBlank(uuid) && !username.equals(uuid);
    }
}
