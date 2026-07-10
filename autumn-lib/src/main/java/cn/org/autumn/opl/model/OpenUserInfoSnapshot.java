package cn.org.autumn.opl.model;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OpenUserInfoSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    private String openId;
    private String unionId;
    private String nickname;
    private String icon;
    private String mobile;
    private String email;
    private Integer verified;
    private Integer status;
}
