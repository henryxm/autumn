package cn.org.autumn.auth.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

/**
 * 统一授权用户信息（按 granted scope 裁剪后输出）。
 */
@Getter
@Setter
public class AuthUserInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private String uuid;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private String openId;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private String unionId;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private String nickname;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private String icon;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private String username;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private String mobile;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private String email;

    private Integer verified;

    private Integer status;
}
