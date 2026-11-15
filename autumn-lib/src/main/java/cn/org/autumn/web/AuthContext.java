package cn.org.autumn.web;

import java.io.Serializable;

/**
 * 请求级鉴权上下文：本次请求<strong>如何</strong>合法解析身份、用的<strong>哪把</strong> accessKey。
 * <p>
 * 由 {@code UserContextService} / minclouds {@code UserInfoResolver} 等在解析成功后绑定到
 * {@link jakarta.servlet.http.HttpServletRequest}；Controller 可通过方法参数注入或
 * {@link AuthContexts#current()} 读取。
 * <p>
 * <strong>禁止</strong>写入 {@code client_secret} 或完整 {@code access_token}（仅允许 {@link #tokenPrefix}）。
 *
 * @see AuthSource
 * @see AuthContexts
 */
public class AuthContext implements Serializable {
    private static final long serialVersionUID = 1L;

    private AuthSource source = AuthSource.NONE;
    /** 最终业务用户 uuid（影子换真人后为真人） */
    private String userUuid;
    /** 行动者：机器人 uuid / 换真人之前的影子账号 uuid */
    private String actorUuid;
    /** OAuth {@code client_id} / accessKeyId */
    private String accessKey;
    /** 命中的凭证头名，如 Authorization、Token */
    private String credentialHeader;
    /** 令牌安全前缀（见 {@link AuthContexts#tokenPrefix}），禁止存全文 */
    private String tokenPrefix;
    private boolean hintPresent;
    private String hintUuid;
    private long resolvedAt;

    public AuthContext() {
        this.resolvedAt = System.currentTimeMillis();
    }

    public static AuthContext of(AuthSource source) {
        AuthContext ctx = new AuthContext();
        ctx.setSource(source != null ? source : AuthSource.NONE);
        return ctx;
    }

    /** 空安全：未绑定或无 accessKey 时返回 "" */
    public static String accessKeyOf(AuthContext auth) {
        return auth == null ? "" : auth.accessKeyOrEmpty();
    }

    /** 空安全：未绑定返回 "" */
    public static String sourceOf(AuthContext auth) {
        return auth == null ? "" : auth.sourceName();
    }

    public String accessKeyOrEmpty() {
        return accessKey == null ? "" : accessKey;
    }

    public String sourceName() {
        return source == null ? AuthSource.NONE.name() : source.name();
    }

    public boolean hasAccessKey() {
        return accessKey != null && !accessKey.isBlank();
    }

    /** 单行审计片段，便于拼进业务日志 */
    public String auditLabel() {
        return "Source=" + sourceName() + ", AccessKey=" + accessKeyOrEmpty();
    }

    public AuthSource getSource() {
        return source;
    }

    public void setSource(AuthSource source) {
        this.source = source != null ? source : AuthSource.NONE;
    }

    public String getUserUuid() {
        return userUuid;
    }

    public void setUserUuid(String userUuid) {
        this.userUuid = userUuid;
    }

    public String getActorUuid() {
        return actorUuid;
    }

    public void setActorUuid(String actorUuid) {
        this.actorUuid = actorUuid;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getCredentialHeader() {
        return credentialHeader;
    }

    public void setCredentialHeader(String credentialHeader) {
        this.credentialHeader = credentialHeader;
    }

    public String getTokenPrefix() {
        return tokenPrefix;
    }

    public void setTokenPrefix(String tokenPrefix) {
        this.tokenPrefix = tokenPrefix;
    }

    public boolean isHintPresent() {
        return hintPresent;
    }

    public void setHintPresent(boolean hintPresent) {
        this.hintPresent = hintPresent;
    }

    public String getHintUuid() {
        return hintUuid;
    }

    public void setHintUuid(String hintUuid) {
        this.hintUuid = hintUuid;
    }

    public long getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(long resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public AuthContext source(AuthSource source) {
        setSource(source);
        return this;
    }

    public AuthContext userUuid(String userUuid) {
        setUserUuid(userUuid);
        return this;
    }

    public AuthContext actorUuid(String actorUuid) {
        setActorUuid(actorUuid);
        return this;
    }

    public AuthContext accessKey(String accessKey) {
        setAccessKey(accessKey);
        return this;
    }

    public AuthContext credentialHeader(String credentialHeader) {
        setCredentialHeader(credentialHeader);
        return this;
    }

    public AuthContext tokenPrefix(String tokenPrefix) {
        setTokenPrefix(tokenPrefix);
        return this;
    }

    public AuthContext hint(String hintUuid) {
        this.hintPresent = hintUuid != null && !hintUuid.isBlank();
        this.hintUuid = hintUuid;
        return this;
    }
}
