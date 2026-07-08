package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.annotation.SkipInterceptor;
import cn.org.autumn.config.Config;
import cn.org.autumn.database.CrudGuard;
import cn.org.autumn.model.AccountAuthConfig;
import cn.org.autumn.modules.spm.service.SuperPositionModelService;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.modules.sys.shiro.LogoutSkipSupport;
import cn.org.autumn.modules.sys.shiro.ShiroSessionService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.sys.support.SysAutologinSupport;
import cn.org.autumn.modules.sys.support.SysLogoutSupport;
import cn.org.autumn.modules.usr.service.UserProfileService;
import cn.org.autumn.site.PageFactory;
import cn.org.autumn.utils.R;
import com.google.code.kaptcha.Constants;
import com.google.code.kaptcha.Producer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.imageio.ImageIO;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;

@Controller
@Slf4j
public class SysLoginController {

    @Autowired(required = false)
    private Producer producer;

    @Autowired
    @Lazy
    SuperPositionModelService superPositionModelService;

    @Autowired
    @Lazy
    SysUserService sysUserService;

    @Autowired
    @Lazy
    PageFactory pageFactory;

    @Autowired
    @Lazy
    UserProfileService userProfileService;

    @Autowired
    @Lazy
    SysConfigService sysConfigService;

    @Autowired
    @Lazy
    ShiroSessionService shiroSessionService;

    @RequestMapping("captcha.jpg")
    @SkipInterceptor
    public void captcha(HttpServletResponse response) throws IOException {
        response.setHeader("Cache-Control", "no-store, no-cache");
        response.setContentType("image/jpeg");
        if (null != producer) {
            //生成文字验证码
            String text = producer.createText();
            //生成图片验证码
            BufferedImage image = producer.createImage(text);
            //保存到shiro session
            ShiroUtils.setSessionAttribute(Constants.KAPTCHA_SESSION_KEY, text);
            ServletOutputStream out = response.getOutputStream();
            ImageIO.write(image, "jpg", out);
        }
    }

    /**
     * 登录
     */
    @ResponseBody
    @RequestMapping(value = "/sys/login", method = RequestMethod.POST)
    public R login(String username, String password, boolean rememberMe, String captcha, HttpServletRequest request, HttpServletResponse response) {
        return CrudGuard.force(() -> {
            R result = doLogin(username, password, rememberMe, captcha, request);
            if (result != null && Integer.valueOf(0).equals(result.get("code"))) {
                LogoutSkipSupport.clear(response, request);
            }
            return result;
        });
    }

    private R doLogin(String username, String password, boolean rememberMe, String captcha, HttpServletRequest request) {
        if (Config.isDev()) {
            if (StringUtils.isEmpty(username)) {
                username = "admin";
            }
            if (StringUtils.isEmpty(password)) {
                password = "admin";
            }
        } else {
            R captchaError = SysAuthSupport.validateCaptcha(captcha);
            if (captchaError != null) {
                return captchaError;
            }
        }

        try {
            sysUserService.login(username, password, rememberMe);
            SysUserEntity userEntity = ShiroUtils.getUserEntity();
            if (userEntity != null) {
                SysAuthSupport.recordLoginProfile(userProfileService, userEntity.getUuid(), request);
            }
        } catch (UnknownAccountException e) {
            return R.error(e.getMessage());
        } catch (IncorrectCredentialsException e) {
            return R.error("账号或密码不正确");
        } catch (LockedAccountException e) {
            return R.error("账号已被锁定,请联系管理员");
        } catch (AuthenticationException e) {
            return R.error("账户验证失败");
        }

        return R.ok().put("data", SysAuthSupport.resolvePostLoginRedirect(request, superPositionModelService, sysConfigService.getAccountAuthConfig()));
    }

    /**
     * 登录页环境探测：默认关闭（{@code devAutologinEnabled=false}）时不自动跳转、不静默登录；
     * 开启后仅 dev 环境返回 {@code devProbe} 供静默 admin 探测。
     */
    @ResponseBody
    @RequestMapping(value = "sys/autologin", method = RequestMethod.POST)
    public R autoLogin(HttpServletRequest request) {
        AccountAuthConfig authConfig = sysConfigService.getAccountAuthConfig();
        boolean devAutologinEnabled = authConfig != null && authConfig.isDevAutologinEnabled();
        String redirect = SysAuthSupport.resolvePostLoginRedirect(request, superPositionModelService, authConfig);
        R response = SysAutologinSupport.buildAutologinResponse(LogoutSkipSupport.marked(request), ShiroUtils.isAuthenticated(), ShiroUtils.isLogin(), Config.isDev(), devAutologinEnabled, redirect);
        if (log.isDebugEnabled()) {
            log.debug("autologin result: reason={}", response.get("reason"));
        }
        return response;
    }

    /**
     * 退出
     */
    @RequestMapping(value = "logout", method = RequestMethod.GET)
    @SkipInterceptor
    public String logout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        SysLogoutSupport.logoutAndForceReauth(shiroSessionService);
        LogoutSkipSupport.mark(httpServletRequest, httpServletResponse);
        return "redirect:" + pageFactory.logout(httpServletRequest, httpServletResponse, model);
    }
}
