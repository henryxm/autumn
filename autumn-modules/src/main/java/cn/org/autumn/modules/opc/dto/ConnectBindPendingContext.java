package cn.org.autumn.modules.opc.dto;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

/** OPC 绑定选择页 pending token 载荷（Redis 或内存，TTL 10 分钟）。 */
@Getter
@Setter
public class ConnectBindPendingContext implements Serializable {
    private static final long serialVersionUID = 1L;

    private String connectAppUuid;
    private String appId;
    private String userInfoJson;
    private String accessToken;
    private String callback;
}
