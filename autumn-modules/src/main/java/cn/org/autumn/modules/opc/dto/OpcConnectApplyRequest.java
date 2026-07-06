package cn.org.autumn.modules.opc.dto;

import java.io.Serializable;
import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OpcConnectApplyRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "platformBaseUrl不能为空")
    private String platformBaseUrl;
    private String name;
    @NotBlank(message = "redirectUri不能为空")
    private String redirectUri;
    private String scope;
    @NotBlank(message = "accessToken不能为空")
    private String accessToken;
}
