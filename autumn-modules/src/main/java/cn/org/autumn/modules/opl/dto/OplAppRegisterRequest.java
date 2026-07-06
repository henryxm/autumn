package cn.org.autumn.modules.opl.dto;

import cn.org.autumn.opl.model.OpenAppType;
import java.io.Serializable;
import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OplAppRegisterRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "应用名称不能为空")
    private String name;
    @NotBlank(message = "redirectUri不能为空")
    private String redirectUri;
    private String scope;
    private OpenAppType appType;
}
