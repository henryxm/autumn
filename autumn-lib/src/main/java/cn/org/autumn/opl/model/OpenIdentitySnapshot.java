package cn.org.autumn.opl.model;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OpenIdentitySnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    private String openId;
    private String unionId;
    private String appId;
    private String account;
    private String user;
}
