package cn.org.autumn.modules.client.oauth2;

import cn.org.autumn.modules.client.entity.WebOauthBindEntity;
import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import lombok.Getter;

/** OAuth uuid 绑定冲突或校验失败。 */
@Getter
public class WebOauthBindException extends RuntimeException {

    public enum ConflictType {
        UPSTREAM_UUID_INVALID,
        UPSTREAM_BOUND_TO_OTHER,
        LOCAL_ALREADY_BOUND
    }

    private final ConflictType conflictType;
    private final String clientId;
    private final String upstreamUuid;
    private final String sessionUserUuid;
    private final String boundUserUuid;
    private final WebAuthenticationEntity webAuth;
    private final WebOauthBindEntity existingBind;

    public WebOauthBindException(ConflictType conflictType, String message, WebAuthenticationEntity webAuth, String upstreamUuid, String sessionUserUuid, String boundUserUuid, WebOauthBindEntity existingBind) {
        super(message);
        this.conflictType = conflictType;
        this.webAuth = webAuth;
        this.clientId = webAuth == null ? null : webAuth.getClientId();
        this.upstreamUuid = upstreamUuid;
        this.sessionUserUuid = sessionUserUuid;
        this.boundUserUuid = boundUserUuid;
        this.existingBind = existingBind;
    }

    public static WebOauthBindException invalidUpstream(WebAuthenticationEntity webAuth) {
        return new WebOauthBindException(ConflictType.UPSTREAM_UUID_INVALID, "授权用户信息无效", webAuth, null, null, null, null);
    }

    public static WebOauthBindException upstreamBoundToOther(WebAuthenticationEntity webAuth, String upstreamUuid, String sessionUserUuid, String boundUserUuid, WebOauthBindEntity bind) {
        return new WebOauthBindException(ConflictType.UPSTREAM_BOUND_TO_OTHER, "该上游账号已绑定其他本地用户", webAuth, upstreamUuid, sessionUserUuid, boundUserUuid, bind);
    }

    public static WebOauthBindException localAlreadyBound(WebAuthenticationEntity webAuth, String upstreamUuid, String sessionUserUuid, WebOauthBindEntity bind) {
        return new WebOauthBindException(ConflictType.LOCAL_ALREADY_BOUND, "当前本地账号已绑定其他上游账号", webAuth, upstreamUuid, sessionUserUuid, bind == null ? null : bind.getUser(), bind);
    }
}
