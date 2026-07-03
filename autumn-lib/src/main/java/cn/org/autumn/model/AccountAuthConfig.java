package cn.org.autumn.model;

import cn.org.autumn.annotation.ConfigField;
import cn.org.autumn.annotation.ConfigParam;
import cn.org.autumn.config.InputType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigParam(paramKey = AccountAuthConfig.CONFIG_KEY, category = AccountAuthConfig.config, name = "账号认证配置", description = "自助注册、忘记密码等前台账号能力开关")
public class AccountAuthConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String CONFIG_KEY = "ACCOUNT_AUTH_CONFIG";
    public static final String config = "account_auth_config";

    @ConfigField(category = InputType.BooleanType, name = "开放自助注册", description = "关闭后注册页与注册接口不可用，登录页隐藏注册入口")
    private boolean registerEnabled = false;

    @ConfigField(category = InputType.BooleanType, name = "开放忘记密码", description = "关闭后忘记密码页与重置接口不可用，登录页隐藏忘记密码入口")
    private boolean forgotPasswordEnabled = true;

    public List<String> validateAndFix() {
        return new ArrayList<>();
    }
}
