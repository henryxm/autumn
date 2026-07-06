package cn.org.autumn.opc.model;

import cn.org.autumn.opc.OpcConstants;
import cn.org.autumn.opl.OplConstants;
import java.io.Serializable;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

/** 接入应用对外视图（不含 appSecret）。 */
@Getter
@Setter
public class ConnectAppSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    private String uuid;
    private String user;
    private String appId;
    private String name;
    private String platformBaseUrl;
    private String redirectUri;
    private String scope;
    private String authorizeUri;
    private String tokenUri;
    private String userInfoUri;
    private int status = OplConstants.STATUS_ACTIVE;
    private Date create;
    private Date update;

    /** 本系统 OPC 授权入口（相对路径）。 */
    public String authorizeEntryUrl() {
        return OpcConstants.OAUTH2_AUTHORIZE + "?appId=" + appId;
    }
}
