package cn.org.autumn.modules.opc.dto;

import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OpcBindAdminView {
    private Long id;
    private String uuid;
    private String connectApp;
    private String appId;
    private String appName;
    private String user;
    private String username;
    private String openId;
    private String unionId;
    private Date create;
    private Date update;
}
