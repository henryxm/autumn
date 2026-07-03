package cn.org.autumn.site;

import cn.org.autumn.modules.sys.service.SysConfigService;
import org.springframework.ui.Model;

/**
 * 登录、注册、忘记密码等前台页面的公共 Model 属性。
 */
public final class AuthPageAttributes {

    private AuthPageAttributes() {
    }

    public static void apply(Model model, SysConfigService sysConfigService) {
        if (model == null || sysConfigService == null) {
            return;
        }
        if (!model.containsAttribute("bodyClass")) {
            model.addAttribute("bodyClass", "login-page-v2");
        }
        model.addAttribute("siteName", sysConfigService.getLoadingBrand());
        model.addAttribute("registerEnabled", sysConfigService.isRegisterEnabled());
        model.addAttribute("forgotPasswordEnabled", sysConfigService.isForgotPasswordEnabled());
    }
}
