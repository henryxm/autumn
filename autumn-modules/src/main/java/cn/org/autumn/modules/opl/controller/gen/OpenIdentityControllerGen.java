package cn.org.autumn.modules.opl.controller.gen;

import java.util.Arrays;
import java.util.Map;
import cn.org.autumn.validator.ValidatorUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import cn.org.autumn.modules.opl.entity.OpenIdentityEntity;
import cn.org.autumn.modules.opl.service.OpenIdentityService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;

public class OpenIdentityControllerGen {

    @Autowired
    protected OpenIdentityService openIdentityService;

    @RequestMapping("/list")
    @RequiresPermissions("opl:openidentity:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = openIdentityService.queryPage(params);
        return R.ok().put("page" , page);
    }

    @RequestMapping("/info/{id}")
    @RequiresPermissions("opl:openidentity:info")
    public R info(@PathVariable("id") Long id) {
        OpenIdentityEntity openIdentity = openIdentityService.selectById(id);
        return R.ok().put("openIdentity" , openIdentity);
    }

    @RequestMapping("/save")
    @RequiresPermissions("opl:openidentity:save")
    public R save(@RequestBody OpenIdentityEntity openIdentity) {
        openIdentityService.insert(openIdentity);
        return R.ok();
    }

    @RequestMapping("/update")
    @RequiresPermissions("opl:openidentity:update")
    public R update(@RequestBody OpenIdentityEntity openIdentity) {
        ValidatorUtils.validateEntity(openIdentity);
        openIdentityService.updateAllColumnById(openIdentity);
        return R.ok();
    }

    @RequestMapping("/delete")
    @RequiresPermissions("opl:openidentity:delete")
    public R delete(@RequestBody Long[] ids) {
        openIdentityService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }
}
