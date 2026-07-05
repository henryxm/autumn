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
import cn.org.autumn.modules.opl.entity.OpenUnionEntity;
import cn.org.autumn.modules.opl.service.OpenUnionService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;

public class OpenUnionControllerGen {

    @Autowired
    protected OpenUnionService openUnionService;

    @RequestMapping("/list")
    @RequiresPermissions("opl:openunion:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = openUnionService.queryPage(params);
        return R.ok().put("page" , page);
    }

    @RequestMapping("/info/{id}")
    @RequiresPermissions("opl:openunion:info")
    public R info(@PathVariable("id") Long id) {
        OpenUnionEntity openUnion = openUnionService.selectById(id);
        return R.ok().put("openUnion" , openUnion);
    }

    @RequestMapping("/save")
    @RequiresPermissions("opl:openunion:save")
    public R save(@RequestBody OpenUnionEntity openUnion) {
        openUnionService.insert(openUnion);
        return R.ok();
    }

    @RequestMapping("/update")
    @RequiresPermissions("opl:openunion:update")
    public R update(@RequestBody OpenUnionEntity openUnion) {
        ValidatorUtils.validateEntity(openUnion);
        openUnionService.updateAllColumnById(openUnion);
        return R.ok();
    }

    @RequestMapping("/delete")
    @RequiresPermissions("opl:openunion:delete")
    public R delete(@RequestBody Long[] ids) {
        openUnionService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }
}
