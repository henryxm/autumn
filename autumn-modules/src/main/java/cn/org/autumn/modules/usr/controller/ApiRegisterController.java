package cn.org.autumn.modules.usr.controller;

import cn.org.autumn.modules.usr.service.UserProfileService;
import cn.org.autumn.utils.R;
import cn.org.autumn.validator.ValidatorUtils;
import cn.org.autumn.modules.usr.entity.UserProfileEntity;
import cn.org.autumn.modules.usr.form.RegisterForm;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

/**
 * 注册接口
 */
@RestController
@RequestMapping("/api")
@Api(tags="注册接口")
public class ApiRegisterController {
    @Autowired
    private UserProfileService userProfileService;

    @PostMapping("register")
    @ApiOperation("注册")
    public R register(@RequestBody RegisterForm form){
        //表单校验
        ValidatorUtils.validateEntity(form);

        UserProfileEntity user = new UserProfileEntity();
        user.setMobile(form.getMobile());
        user.setUsername(form.getMobile());
        user.setPassword(DigestUtils.sha256Hex(form.getPassword()));
        user.setCreateTime(new Date());
        userProfileService.insert(user);

        return R.ok();
    }
}
