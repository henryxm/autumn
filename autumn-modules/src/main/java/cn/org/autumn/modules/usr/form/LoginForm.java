package cn.org.autumn.modules.usr.form;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;

@Schema(description = "登录表单")
public class LoginForm {
    @Schema(description = "手机号")
    @NotBlank(message="手机号不能为空")
    private String mobile;

    @Schema(description = "密码")
    @NotBlank(message="密码不能为空")
    private String password;

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
