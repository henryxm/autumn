package cn.org.autumn.modules.client.dto;

import java.io.Serializable;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OauthRpBindAdminView implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String authentication;
    private String clientId;
    private String clientName;
    private String user;
    private String username;
    private String upper;
    private Date create;
    private Date update;
}
