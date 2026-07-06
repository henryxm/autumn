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
import cn.org.autumn.modules.opl.entity.OpenCodeEntity;
import cn.org.autumn.modules.opl.service.OpenCodeService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;

public class OpenCodeControllerGen {

    @Autowired
    protected OpenCodeService openCodeService;

    @RequestMapping("/list")
    @RequiresPermissions("opl:opencode:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = openCodeService.queryPage(params);
        return R.ok().put("page" , page);
    }

    @RequestMapping("/info/{id}")
    @RequiresPermissions("opl:opencode:info")
    public R info(@PathVariable("id") Long id) {
        OpenCodeEntity openCode = openCodeService.selectById(id);
        return R.ok().put("openCode" , openCode);
    }

    @RequestMapping("/save")
    @RequiresPermissions("opl:opencode:save")
    public R save(@RequestBody OpenCodeEntity openCode) {
        openCodeService.insert(openCode);
        return R.ok();
    }

    @RequestMapping("/update")
    @RequiresPermissions("opl:opencode:update")
    public R update(@RequestBody OpenCodeEntity openCode) {
        ValidatorUtils.validateEntity(openCode);
        openCodeService.updateAllColumnById(openCode);
        return R.ok();
    }

    @RequestMapping("/delete")
    @RequiresPermissions("opl:opencode:delete")
    public R delete(@RequestBody Long[] ids) {
        openCodeService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }
}
