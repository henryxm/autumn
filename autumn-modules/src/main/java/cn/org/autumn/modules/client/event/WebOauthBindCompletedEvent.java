package cn.org.autumn.modules.client.event;

import cn.org.autumn.modules.usr.dto.UserProfile;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 经典 OAuth2 RP 绑定（含登录建绑）成功后发布：本地用户 uuid + 上游 userInfo（可含 verified）。
 */
@Getter
public class WebOauthBindCompletedEvent extends ApplicationEvent {

    private final String localUserUuid;
    private final String clientId;
    private final String originUri;
    private final UserProfile upstream;
    private final boolean idempotent;

    public WebOauthBindCompletedEvent(Object source, String localUserUuid, String clientId, String originUri, UserProfile upstream, boolean idempotent) {
        super(source);
        this.localUserUuid = localUserUuid;
        this.clientId = clientId;
        this.originUri = originUri;
        this.upstream = upstream;
        this.idempotent = idempotent;
    }
}
