package cn.org.autumn.modules.opl.controller;

import cn.org.autumn.modules.opl.controller.gen.OpenAppControllerGen;
import cn.org.autumn.modules.opl.entity.OpenAppEntity;
import cn.org.autumn.utils.R;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("opl/openapp")
public class OpenAppController extends OpenAppControllerGen {

    @Override
    @RequestMapping("/info/{id}")
    @RequiresPermissions("opl:openapp:info")
    public R info(@PathVariable("id") Long id) {
        OpenAppEntity openApp = openAppService.selectById(id);
        if (openApp != null) {
            openApp.setAppSecretHash(null);
            openApp.setAppSecretSalt(null);
        }
        return R.ok().put("openApp", openApp);
    }
}
