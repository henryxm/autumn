package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.annotation.SkipInterceptor;
import cn.org.autumn.modules.sys.service.AuthLoginProviderService;
import cn.org.autumn.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 登录页授权 Provider 公开 API。 */
@RestController
@RequestMapping("auth/login")
@SkipInterceptor
public class AuthLoginProviderController {

    @Autowired
    private AuthLoginProviderService authLoginProviderService;

    @GetMapping("/providers")
    public R providers() {
        return R.ok().put("data", authLoginProviderService.listPageProviders());
    }
}
