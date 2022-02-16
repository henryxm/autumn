package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.config.Config;
import cn.org.autumn.modules.spm.service.SuperPositionModelService;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.modules.usr.service.UserProfileService;
import cn.org.autumn.site.PageFactory;
import cn.org.autumn.utils.IPUtils;
import com.google.code.kaptcha.Constants;
import com.google.code.kaptcha.Producer;
import cn.org.autumn.utils.R;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authc.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;

@Controller
public class SysLoginController {
    @Autowired
    private Producer producer;

    @Autowired
    SuperPositionModelService superPositionModelService;

    @Autowired
    SysUserService sysUserService;

    @Autowired
    PageFactory pageFactory;

    @Autowired
    UserProfileService userProfileService;

    @RequestMapping("captcha.jpg")
    public void captcha(HttpServletResponse response) throws IOException {
        response.setHeader("Cache-Control", "no-store, no-cache");
        response.setContentType("image/jpeg");

        //生成文字验证码
        String text = producer.createText();
        //生成图片验证码
        BufferedImage image = producer.createImage(text);
        //保存到shiro session
        ShiroUtils.setSessionAttribute(Constants.KAPTCHA_SESSION_KEY, text);

        ServletOutputStream out = response.getOutputStream();
        ImageIO.write(image, "jpg", out);
    }

    /**
     * 登录
     */
    @ResponseBody
    @RequestMapping(value = "/sys/login", method = RequestMethod.POST)
    public R login(String username, String password, boolean rememberMe, String captcha, HttpServletRequest request) {
        if (!Config.isDev()) {
            String kaptcha = ShiroUtils.getKaptcha(Constants.KAPTCHA_SESSION_KEY);
            if (!captcha.equalsIgnoreCase(kaptcha)) {
                return R.error("验证码不正确");
            }
        } else {
            if (StringUtils.isEmpty(username))
                username = "admin";
            if (StringUtils.isEmpty(password))
                password = "admin";
        }

        try {
            sysUserService.login(username, password, rememberMe);
            String ip = IPUtils.getIp(request);
            try {
                SysUserEntity userEntity = ShiroUtils.getUserEntity();
                if (null != userEntity) {
                    userProfileService.updateLoginIp(userEntity.getUuid(), ip, request.getHeader("user-agent"));
                }
            } catch (Exception ignored) {
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

        String j = "index.html";
        if (superPositionModelService.menuWithSpm())
            j = "/";
        return R.ok().put("data", j);
    }

    /**
     * 退出
     */
    @ResponseBody
    @RequestMapping(value = "sys/autologin", method = RequestMethod.POST)
    public R autoLogin() {
        if (Config.isDev())
            return R.ok();
        else
            return R.error();
    }

    /**
     * 退出
     */
    @RequestMapping(value = "logout", method = RequestMethod.GET)
    public String logout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        ShiroUtils.logout();
        return "redirect:" + pageFactory.logout(httpServletRequest, httpServletResponse, model);
    }
}
