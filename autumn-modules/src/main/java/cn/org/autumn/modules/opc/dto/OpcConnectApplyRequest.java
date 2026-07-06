package cn.org.autumn.modules.opc.dto;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OpcConnectApplyRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String platformBaseUrl;
    private String name;
    private String redirectUri;
    private String scope;
    private String accessToken;
}
