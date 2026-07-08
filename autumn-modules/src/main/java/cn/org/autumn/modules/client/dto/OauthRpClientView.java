package cn.org.autumn.modules.client.dto;

import java.io.Serializable;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OauthRpClientView implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String uuid;
    private String clientId;
    private String name;
    private String originUri;
    private String redirectUri;
    private String authorizeUri;
    private String accessTokenUri;
    private String userInfoUri;
    private String scope;
    private String userInfoDelivery;
    private Date createTime;
    private long bindCount;
    private String loginUrl;
    private String loginAuthentication;
    private boolean secretConfigured;
    private boolean sameInstance;
    private String icon;
    private String hash;
    private int pageLogin;
}
