package cn.org.autumn.modules.client.dto;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

/** OAuth RP state 载荷：回调地址 + 发起授权的 clientId。 */
@Getter
@Setter
public class OauthRpStatePayload implements Serializable {
    private static final long serialVersionUID = 1L;

    private String callback;
    private String clientId;
}
