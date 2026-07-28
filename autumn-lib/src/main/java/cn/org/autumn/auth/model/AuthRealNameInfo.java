package cn.org.autumn.auth.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

/**
 * 授权实名详情（按 granted 的 realname_* 档位裁剪后经独立资源接口下发）。
 */
@Getter
@Setter
public class AuthRealNameInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private String name;

    private Integer age;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private String gender;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private String birthday;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private String address;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private String idNumber;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private String ethnicity;
}
