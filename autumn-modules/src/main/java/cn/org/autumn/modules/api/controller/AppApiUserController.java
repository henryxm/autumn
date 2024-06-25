package cn.org.autumn.modules.api.controller;

import cn.org.autumn.modules.api.service.AppApiUserService;
import cn.org.autumn.modules.sys.dto.SysUser;
import cn.org.autumn.utils.R;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/app/api/user")
@Tags({@Tag(name = "app_api_user", description = "移动端登录接口")})
public class AppApiUserController {

    @Autowired
    AppApiUserService appApiUserService;

    /**
     * 用户登录接口
     * */
    @RequestMapping(value = "/login", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public R login(@RequestBody Map<String, Object> params){

        String username = Objects.toString(params.get("username"), "");
        String password = Objects.toString(params.get("password"), "");

        //用户密码校验
        SysUser sysUser = appApiUserService.checkUserPassword(username, password);

        return R.ok().put("userinfo" , sysUser);
    }

    /**
     * 注册页面获取验证码
     * */
    public R getcode(@RequestBody Map<String, Object> params){

        //区分是手机还是邮箱

        //验证是否已经注册

        //生成验证码

        //发送验证码

        return R.ok();
    }

}
