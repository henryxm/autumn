package cn.org.autumn.modules.opc.support;

import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import cn.org.autumn.modules.opc.entity.ConnectBindEntity;
import lombok.Getter;

/** OPC openId 绑定冲突或校验失败。 */
@Getter
public class ConnectBindException extends RuntimeException {

    public enum ConflictType {
        USERINFO_INVALID,
        UPSTREAM_BOUND_TO_OTHER,
        LOCAL_ALREADY_BOUND,
        BIND_CHOICE_REQUIRED
    }

    private final ConflictType conflictType;
    private final String appId;
    private final String openId;
    private final String platformUser;
    private final String boundUserUuid;
    private final String pendingToken;
    private final ConnectAppEntity app;
    private final ConnectBindEntity existingBind;

    public ConnectBindException(ConflictType conflictType, String message, ConnectAppEntity app, String openId, String platformUser, String boundUserUuid, ConnectBindEntity existingBind, String pendingToken) {
        super(message);
        this.conflictType = conflictType;
        this.app = app;
        this.appId = app == null ? null : app.getAppId();
        this.openId = openId;
        this.platformUser = platformUser;
        this.boundUserUuid = boundUserUuid;
        this.existingBind = existingBind;
        this.pendingToken = pendingToken;
    }

    public ConnectBindException(ConflictType conflictType, String message, ConnectAppEntity app, String openId, String platformUser, String boundUserUuid, ConnectBindEntity existingBind) {
        this(conflictType, message, app, openId, platformUser, boundUserUuid, existingBind, null);
    }

    public static ConnectBindException invalidUserInfo(ConnectAppEntity app) {
        return new ConnectBindException(ConflictType.USERINFO_INVALID, "授权用户信息无效", app, null, null, null, null);
    }

    public static ConnectBindException upstreamBoundToOther(ConnectAppEntity app, String openId, String platformUser, String boundUserUuid, ConnectBindEntity bind) {
        return new ConnectBindException(ConflictType.UPSTREAM_BOUND_TO_OTHER, "该开放平台账号已绑定其他本地用户", app, openId, platformUser, boundUserUuid, bind);
    }

    public static ConnectBindException localAlreadyBound(ConnectAppEntity app, String openId, String platformUser, ConnectBindEntity bind) {
        return new ConnectBindException(ConflictType.LOCAL_ALREADY_BOUND, "该本地用户已绑定其他开放平台账号", app, openId, platformUser, bind == null ? null : bind.getUser(), bind);
    }

    public static ConnectBindException bindChoiceRequired(ConnectAppEntity app, String openId) {
        return new ConnectBindException(ConflictType.BIND_CHOICE_REQUIRED, "请选择绑定方式", app, openId, null, null, null);
    }

    public static ConnectBindException bindChoiceRequired(ConnectAppEntity app, String openId, String pendingToken) {
        return new ConnectBindException(ConflictType.BIND_CHOICE_REQUIRED, "请选择绑定方式", app, openId, null, null, null, pendingToken);
    }
}
