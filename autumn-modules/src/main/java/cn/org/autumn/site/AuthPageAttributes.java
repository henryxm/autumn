package cn.org.autumn.site;

import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.utils.Utils;
import cn.org.autumn.utils.WebPathUtils;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
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

    /**
     * 登录页 OAuth callback 服务端净化，写入 {@code safeOauthCallback} 供 {@code __LOGIN_CONFIG__} 使用。
     */
    public static void applySafeOauthCallback(HttpServletRequest request, Model model) {
        if (request == null || model == null) {
            return;
        }
        String raw = request.getParameter("callback");
        if (StringUtils.isBlank(raw)) {
            raw = Utils.getCallback(request);
        }
        if (StringUtils.isBlank(raw) && Boolean.TRUE.equals(model.getAttribute("oauthAuthorize"))) {
            StringBuffer url = request.getRequestURL();
            String query = request.getQueryString();
            raw = StringUtils.isNotBlank(query) ? url + "?" + query : url.toString();
        }
        model.addAttribute("safeOauthCallback", WebPathUtils.safeOauthCallbackForClient(request, raw));
    }
}
