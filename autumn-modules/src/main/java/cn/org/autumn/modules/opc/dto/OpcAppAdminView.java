package cn.org.autumn.modules.opc.dto;

import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OpcAppAdminView {
    private Long id;
    private String uuid;
    private String user;
    private String username;
    private String appId;
    private String name;
    private String platformBaseUrl;
    private String redirectUri;
    private String authorizeUri;
    private String tokenUri;
    private String userInfoUri;
    private String scope;
    private int status;
    private Date create;
    private Date update;
    private int bindCount;
    private String authorizeUrl;
    private boolean secretConfigured;
    private String icon;
    private String hash;
    private int pageLogin;
    /**
     * 桌面快捷登录：是否允许该 OPC 应用扫码登录页经 AS Bridge 探测本机超然信。
     * 来自 {@code qrc_client_grant.quick}（clientId = appId），与 {@code pageLogin} 扫码档配合。
     */
    private boolean quick;
}
