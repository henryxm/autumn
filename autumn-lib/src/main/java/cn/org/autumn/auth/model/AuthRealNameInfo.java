package cn.org.autumn.auth.model;

import com.fasterxml.jackson.annotation.JsonInclude;
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

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String name;

    private Integer age;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String gender;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String birthday;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String address;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String idNumber;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String ethnicity;
}
