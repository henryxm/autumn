package cn.org.autumn.model;

import cn.org.autumn.annotation.ConfigField;
import cn.org.autumn.annotation.ConfigParam;
import cn.org.autumn.config.InputType;
import cn.org.autumn.utils.WebPathUtils;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;

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

    @ConfigField(category = InputType.StringType, name = "登录成功后跳转地址", description = "用户直接打开登录页且无 SavedRequest 时的默认跳转；留空则按 SPM/index.html 原逻辑。须为 /、*.html 或带 spm= 的有效页面地址，不可为 API/REST")
    private String postLoginRedirect;

    @ConfigField(category = InputType.BooleanType, name = "开发环境登录页静默探测", description = "默认关闭：登录页不自动跳转、不静默登录。仅 dev 环境且开启后，checkenv 才可静默使用默认 admin 账号")
    private boolean devAutologinEnabled = false;

    public List<String> validateAndFix() {
        List<String> fixes = new ArrayList<>();
        if (StringUtils.isBlank(postLoginRedirect)) {
            postLoginRedirect = null;
            return fixes;
        }
        String trimmed = postLoginRedirect.trim();
        if (!WebPathUtils.isValidPostLoginRedirectConfig(trimmed)) {
            fixes.add("登录成功后跳转地址无效，已清空: " + trimmed);
            postLoginRedirect = null;
            return fixes;
        }
        postLoginRedirect = trimmed;
        return fixes;
    }
}
