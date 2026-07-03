package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.config.Config;
import cn.org.autumn.modules.spm.service.SuperPositionModelService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.usr.service.UserProfileService;
import cn.org.autumn.utils.IPUtils;
import cn.org.autumn.utils.R;
import cn.org.autumn.utils.WebPathUtils;
import com.google.code.kaptcha.Constants;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;

/**
 * 登录、注册、忘记密码等前台认证接口的公共逻辑。
 */
final class SysAuthSupport {

    private SysAuthSupport() {
    }

    static String resolvePostLoginRedirect(HttpServletRequest request, SuperPositionModelService superPositionModelService) {
        if (superPositionModelService != null && superPositionModelService.menuWithSpm()) {
            return WebPathUtils.forBrowser(request, "/");
        }
        return "index.html";
    }

    static String resolveLoginPagePath(HttpServletRequest request) {
        return WebPathUtils.forBrowser(request, "/login.html");
    }

    static R validateCaptcha(String captcha) {
        if (Config.isDev()) {
            return null;
        }
        String kaptcha = ShiroUtils.getKaptcha(Constants.KAPTCHA_SESSION_KEY);
        if (StringUtils.isBlank(captcha) || !captcha.equalsIgnoreCase(kaptcha)) {
            return R.error("验证码不正确");
        }
        return null;
    }

    static R validatePasswordPair(String password, String confirmPassword) {
        if (!StringUtils.equals(password, confirmPassword)) {
            return R.error("两次输入的密码不一致");
        }
        return null;
    }

    static void recordLoginProfile(UserProfileService userProfileService, String userUuid, HttpServletRequest request) {
        if (userProfileService == null || StringUtils.isBlank(userUuid) || request == null) {
            return;
        }
        try {
            userProfileService.updateLoginIp(userUuid, IPUtils.getIp(request), request.getHeader("user-agent"));
        } catch (Exception ignored) {
        }
    }
}
