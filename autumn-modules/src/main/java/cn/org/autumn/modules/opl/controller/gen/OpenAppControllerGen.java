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
import cn.org.autumn.modules.opl.entity.OpenAppEntity;
import cn.org.autumn.modules.opl.service.OpenAppService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;

public class OpenAppControllerGen {

    @Autowired
    protected OpenAppService openAppService;

    @RequestMapping("/list")
    @RequiresPermissions("opl:openapp:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = openAppService.queryPage(params);
        return R.ok().put("page" , page);
    }

    @RequestMapping("/info/{id}")
    @RequiresPermissions("opl:openapp:info")
    public R info(@PathVariable("id") Long id) {
        OpenAppEntity openApp = openAppService.selectById(id);
        return R.ok().put("openApp" , openApp);
    }

    @RequestMapping("/save")
    @RequiresPermissions("opl:openapp:save")
    public R save(@RequestBody OpenAppEntity openApp) {
        openAppService.insert(openApp);
        return R.ok();
    }

    @RequestMapping("/update")
    @RequiresPermissions("opl:openapp:update")
    public R update(@RequestBody OpenAppEntity openApp) {
        ValidatorUtils.validateEntity(openApp);
        openAppService.updateAllColumnById(openApp);
        return R.ok();
    }

    @RequestMapping("/delete")
    @RequiresPermissions("opl:openapp:delete")
    public R delete(@RequestBody Long[] ids) {
        openAppService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }
}
