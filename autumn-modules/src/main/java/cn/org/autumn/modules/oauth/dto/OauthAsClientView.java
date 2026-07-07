package cn.org.autumn.modules.oauth.dto;

import java.io.Serializable;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OauthAsClientView implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String uuid;
    private String clientId;
    private String clientName;
    private String redirectUri;
    private String scope;
    private String grantTypes;
    private Integer trusted;
    private Integer archived;
    private Date createTime;
    private String authorizeUrl;
    private String loginUrl;
}
