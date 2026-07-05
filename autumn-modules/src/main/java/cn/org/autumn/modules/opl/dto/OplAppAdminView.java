package cn.org.autumn.modules.opl.dto;

import cn.org.autumn.opl.model.OpenAppType;
import java.io.Serializable;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OplAppAdminView implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String uuid;
    private String account;
    private String accountName;
    private String appId;
    private String name;
    private OpenAppType appType;
    private String redirectUri;
    private String scope;
    private int status;
    private long userCount;
    private long codeCount;
    private long tokenCount;
    private Date create;
    private Date update;
}
