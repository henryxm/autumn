package cn.org.autumn.modules.client.dto;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

/** 绑定选择页 pending token 载荷（Redis 或内存，TTL 10 分钟）。 */
@Getter
@Setter
public class WebOauthBindPendingContext implements Serializable {
    private static final long serialVersionUID = 1L;

    private String webAuthUuid;
    private String upstreamJson;
    private String tokenBody;
    private String callback;
}
