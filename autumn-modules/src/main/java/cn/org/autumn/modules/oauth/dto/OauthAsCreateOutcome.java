package cn.org.autumn.modules.oauth.dto;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OauthAsCreateOutcome implements Serializable {
    private static final long serialVersionUID = 1L;

    private String clientId;
    private String clientSecret;
    private String clientName;
    private String redirectUri;
    private String scope;
    private String authorizeUrl;
    private String tokenUrl;
    private String userInfoUrl;
}
