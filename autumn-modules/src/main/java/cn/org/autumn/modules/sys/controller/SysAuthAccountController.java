package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.database.CrudGuard;
import cn.org.autumn.modules.spm.service.SuperPositionModelService;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.modules.usr.service.UserProfileService;
import cn.org.autumn.utils.R;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@Controller
public class SysAuthAccountController {

    @Autowired
    @Lazy
    private SysUserService sysUserService;

    @Autowired
    @Lazy
    private UserProfileService userProfileService;

    @Autowired
    @Lazy
    private SuperPositionModelService superPositionModelService;

    @Autowired
    @Lazy
    private SysConfigService sysConfigService;

    @ResponseBody
    @RequestMapping(value = "/sys/account/available", method = RequestMethod.GET)
    public R accountAvailable(String account) {
        R closed = ensureRegisterEnabled();
        if (closed != null) {
            return closed;
        }
        if (StringUtils.isBlank(account)) {
            return R.error("请输入账号");
        }
        if (sysUserService.getUser(account.trim()) != null) {
            return R.error("该账号已被注册");
        }
        return R.ok();
    }

    @ResponseBody
    @RequestMapping(value = "/sys/register", method = RequestMethod.POST)
    public R register(String account, String password, String confirmPassword, String captcha, HttpServletRequest request) {
        return CrudGuard.force(() -> doRegister(account, password, confirmPassword, captcha, request));
    }

    @ResponseBody
    @RequestMapping(value = "/sys/forgotpassword", method = RequestMethod.POST)
    public R forgotPassword(String account, String password, String confirmPassword, String captcha, HttpServletRequest request) {
        return CrudGuard.force(() -> doForgotPassword(account, password, confirmPassword, captcha, request));
    }

    private R doRegister(String account, String password, String confirmPassword, String captcha, HttpServletRequest request) {
        R precondition = validateAccountForm(ensureRegisterEnabled(), account, captcha, password, confirmPassword);
        if (precondition != null) {
            return precondition;
        }
        String trimmedAccount = account.trim();
        try {
            SysUserEntity user = sysUserService.registerSelfAccount(trimmedAccount, password);
            sysUserService.login(trimmedAccount, password, true);
            SysAuthSupport.recordLoginProfile(userProfileService, user.getUuid(), request);
            return R.ok().put("data", SysAuthSupport.resolvePostLoginRedirect(request, superPositionModelService, sysConfigService.getAccountAuthConfig()));
        } catch (IllegalArgumentException e) {
            return R.error(e.getMessage());
        } catch (Exception e) {
            log.error("Register failed: {}", e.getMessage());
            return R.error("注册失败，请稍后重试");
        }
    }

    private R doForgotPassword(String account, String password, String confirmPassword, String captcha, HttpServletRequest request) {
        R precondition = validateAccountForm(ensureForgotPasswordEnabled(), account, captcha, password, confirmPassword);
        if (precondition != null) {
            return precondition;
        }
        try {
            sysUserService.resetPasswordByAccount(account, password);
            return R.ok().put("data", SysAuthSupport.resolveLoginPagePath(request));
        } catch (IllegalArgumentException e) {
            return R.error(e.getMessage());
        } catch (Exception e) {
            log.error("Reset password failed: {}", e.getMessage());
            return R.error("密码重置失败，请稍后重试");
        }
    }

    private R validateAccountForm(R featureGate, String account, String captcha, String password, String confirmPassword) {
        if (featureGate != null) {
            return featureGate;
        }
        if (StringUtils.isBlank(account)) {
            return R.error("请输入账号");
        }
        R captchaError = SysAuthSupport.validateCaptcha(captcha);
        if (captchaError != null) {
            return captchaError;
        }
        return SysAuthSupport.validatePasswordPair(password, confirmPassword);
    }

    private R ensureRegisterEnabled() {
        if (!sysConfigService.isRegisterEnabled()) {
            return R.error("暂未开放注册，请联系管理员");
        }
        return null;
    }

    private R ensureForgotPasswordEnabled() {
        if (!sysConfigService.isForgotPasswordEnabled()) {
            return R.error("暂未开放密码重置，请联系管理员");
        }
        return null;
    }
}
