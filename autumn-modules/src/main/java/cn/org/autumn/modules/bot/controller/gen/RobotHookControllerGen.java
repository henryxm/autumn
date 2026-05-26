package cn.org.autumn.modules.bot.controller.gen;

import java.util.Arrays;
import java.util.Map;
import cn.org.autumn.validator.ValidatorUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import cn.org.autumn.modules.bot.entity.RobotHookEntity;
import cn.org.autumn.modules.bot.service.RobotHookService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;

public class RobotHookControllerGen {

    @Autowired
    protected RobotHookService robotHookService;

    @RequestMapping("/list")
    @RequiresPermissions("bot:robothook:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = robotHookService.queryPage(params);
        return R.ok().put("page" , page);
    }

    @RequestMapping("/info/{id}")
    @RequiresPermissions("bot:robothook:info")
    public R info(@PathVariable("id") Long id) {
        RobotHookEntity robotHook = robotHookService.selectById(id);
        return R.ok().put("robotHook" , robotHook);
    }

    @RequestMapping("/save")
    @RequiresPermissions("bot:robothook:save")
    public R save(@RequestBody RobotHookEntity robotHook) {
        robotHookService.insert(robotHook);
        return R.ok();
    }

    @RequestMapping("/update")
    @RequiresPermissions("bot:robothook:update")
    public R update(@RequestBody RobotHookEntity robotHook) {
        ValidatorUtils.validateEntity(robotHook);
        robotHookService.updateAllColumnById(robotHook);
        return R.ok();
    }

    @RequestMapping("/delete")
    @RequiresPermissions("bot:robothook:delete")
    public R delete(@RequestBody Long[] ids) {
        robotHookService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }
}
