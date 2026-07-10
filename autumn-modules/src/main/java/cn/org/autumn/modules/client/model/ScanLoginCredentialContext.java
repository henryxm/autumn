package cn.org.autumn.modules.client.model;

import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import lombok.Getter;
import lombok.Setter;

/** 进程内扫码凭证（含 secret，不对外暴露）。 */
@Getter
@Setter
public class ScanLoginCredentialContext {
    private String type;
    private String id;
    private String name;
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String scope;
    private String originUri;
    private String platformBaseUrl;
    private String qrcMode;
    private WebAuthenticationEntity webAuth;
    private ConnectAppEntity connectApp;
}
