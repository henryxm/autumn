package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.config.Config;
import cn.org.autumn.modules.spm.service.SuperPositionModelService;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.site.PageFactory;
import com.google.code.kaptcha.Constants;
import com.google.code.kaptcha.Producer;
import cn.org.autumn.utils.R;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authc.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
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
    public R login(String username, String password, String captcha) {
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
            sysUserService.login(username, password);
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
    public String logout() {
        ShiroUtils.logout();
        return "redirect:" + pageFactory.getLogin();
    }
}
