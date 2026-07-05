package cn.org.autumn.opl.model;

import cn.org.autumn.opl.OplConstants;
import java.io.Serializable;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OpenAppSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    private String uuid;
    private String account;
    private String appId;
    private String name;
    private OpenAppType appType;
    private String redirectUri;
    private String scope;
    private int status = OplConstants.STATUS_ACTIVE;
    private Date create;
    private Date update;
}
