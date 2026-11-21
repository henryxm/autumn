package cn.org.autumn.auth.model;

import com.fasterxml.jackson.annotation.JsonInclude;
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

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String uuid;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String openId;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String unionId;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String nickname;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String icon;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String username;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String mobile;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String email;

    private Integer verified;

    private Integer status;
}
