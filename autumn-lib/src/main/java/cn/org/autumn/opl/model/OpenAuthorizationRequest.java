package cn.org.autumn.opl.model;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OpenAuthorizationRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String appId;
    private String redirectUri;
    private String responseType;
    private String scope;
    private String state;
    private String userUuid;
    private OpenAppType appType;
    private boolean consented;
}
