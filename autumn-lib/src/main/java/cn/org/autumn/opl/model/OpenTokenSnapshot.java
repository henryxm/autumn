package cn.org.autumn.opl.model;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OpenTokenSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    private String appId;
    private String user;
    private String openId;
    private String unionId;
    private String accessToken;
    private String refreshToken;
    private long accessExpireIn;
    private long refreshExpireIn;
}
