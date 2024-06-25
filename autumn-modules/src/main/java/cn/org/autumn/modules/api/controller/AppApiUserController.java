package cn.org.autumn.modules.api.controller;

import cn.org.autumn.config.Config;

import cn.org.autumn.modules.api.service.AppApiUserService;
import cn.org.autumn.modules.sys.dto.SysUser;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.modules.wall.service.IpBlackService;
//import cn.org.autumn.modules.account.site.AccountConfig;
import cn.org.autumn.minclouds.api.email.EmailService;
import cn.org.autumn.minclouds.api.entity.SmsResult;
import cn.org.autumn.minclouds.api.sms.SmsService;
import cn.org.autumn.utils.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping(value = "/app/api/user", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
@Tags({@Tag(name = "app_api_user", description = "移动端登录接口")})
public class AppApiUserController {

    Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    AppApiUserService appApiUserService;

    @Autowired
    SysUserService sysUserService;

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    IpBlackService ipBlackService;

//    @Autowired
//    AccountConfig accountConfig;

    @Autowired
    SmsService smsService;

    @Autowired
    EmailService emailService;

    Map<String, Integer> black = new ConcurrentHashMap<>();

    /**
     * 用户登录接口
     * */
    @RequestMapping(value = "/login")
    public R login(@RequestBody Map<String, Object> params){

        String username = Objects.toString(params.get("username"), "");
        String password = Objects.toString(params.get("password"), "");

        //用户密码校验
        SysUser sysUser = appApiUserService.checkUserPassword(username, password);

        return R.ok().put("userinfo" , sysUser);
    }

    /**
     * 账号有效性检查
     */
    @RequestMapping(value = "/usernamecheck")
    public R usernameCheck(@RequestBody Map<String, Object> params) {
        String username = Objects.toString(params.get("username"), "");
        if (StringUtils.isNotBlank(username)) {
            if ("admin".equalsIgnoreCase(username))
                return R.ok();

            SysUserEntity user = sysUserService.getUser(username);
            if (null != user)
                return R.ok();
        }
        return R.error("账号不存在");
    }

    /**
     * 获取验证码
     */
    @RequestMapping(value = "/getcode")
    public synchronized R getCode(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        String username = Objects.toString(params.get("username"), "");
        if (StringUtils.isNotBlank(username))
            username = username.trim();
        boolean isPhone = false;
        if (Phone.isPhone(username)) {
            isPhone = true;
        } else if (Email.isEmail(username)) {
            isPhone = false;
        } else
            return R.error("输入的账号格式不正确！");

        Object o = redisTemplate.opsForValue().get(username);
        if (null != o) {
            return R.error("验证码发送太频繁，请稍后再试！");
        }
        String ip = IPUtils.getIp(request);

        if (StringUtils.isBlank(ip) || Objects.equals(ip, "0.0.0.0")) {
            log.error("非法请求:{}", ip);
            return R.error("非法网络请求");
        }

        String code = Utils.getRandomCode(6); //生成一个6位的随机数字串
        if (!Config.isDev()) {
            if (isPhone) {
                if (StringUtils.isBlank(ip))
                    return R.error();
                if (!black.containsKey(ip)) {
                    black.put(ip, 1);
                } else {
                    int c = black.get(ip);
                    black.replace(ip, c + 1);
                }
                int t = black.get(ip);
                if (t > 5) {
                    log.error("刷验证码:{}, IP:{}", username, ip);
                    ipBlackService.create(ip, "sms", "刷验证码拉黑策略");
                    return R.ok();
                }
                log.debug("短信发送:{}, 验证码:{}, IP:{}", username, code, ip);
                SmsResult result = smsService.verifyWith(username, code, ip);
                if (!result.success()) {
                    log.error("发送失败:{}, 错误码:{}, 错误信息:{}", username, result.getCode(), result.getMessage());
                }
            } else {
//                try {
//                    if (!accountConfig.isRegisterSupport())
//                        return R.error("暂不支持注册");
//                } catch (Throwable e) {
//                    log.error("Get Code", e);
//                }
                log.debug("邮件发送:{}, 验证码:{}, IP:{}", username, code, ip);
                emailService.sendCode(username, code);
            }
        } else {
            log.info(code);
        }
        redisTemplate.opsForValue().set(username, code, 3, TimeUnit.MINUTES);
        return R.ok();
    }

}
