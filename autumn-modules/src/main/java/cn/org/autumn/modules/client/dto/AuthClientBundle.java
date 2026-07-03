package cn.org.autumn.modules.client.dto;

import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.client.entity.WebOauthCombineEntity;
import cn.org.autumn.modules.oauth.entity.ClientDetailsEntity;
import cn.org.autumn.modules.qrc.entity.ClientGrantEntity;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthClientBundle implements Serializable {
    private static final long serialVersionUID = 1L;

    private String clientId;
    private ClientDetailsEntity oauth;
    private WebAuthenticationEntity web;
    private ClientGrantEntity qrc;
    private WebOauthCombineEntity combine;
}
